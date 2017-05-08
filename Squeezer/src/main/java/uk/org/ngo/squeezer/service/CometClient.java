/*
 * Copyright (c) 2015 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.service;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.collect.ImmutableMap;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.HttpClientTransport;
import org.eclipse.jetty.client.HttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayersChanged;

class CometClient extends BaseClient {
    private static final String TAG = CometClient.class.getSimpleName();

    /** The maximum number of milliseconds to wait before considering a request to the LMS failed */
    private static final int LONG_POLLING_TIMEOUT = 120_000;

    /** {@link java.util.regex.Pattern} that splits strings on colon. */
    private static final Pattern mColonSplitPattern = Pattern.compile(":");

    /** {@link java.util.regex.Pattern} that splits strings on spaces. */
    private static final Pattern mSpaceSplitPattern = Pattern.compile(" ");

    /** {@link java.util.regex.Pattern} that splits strings on forward slash. */
    private static final Pattern mSlashSplitPattern = Pattern.compile("/");

    /** The channel to publish one-shot requests to. */
    private static final String CHANNEL_SLIM_REQUEST = "/slim/request";

    /** The format string for the channel to listen to for responses to one-shot requests. */
    private static final String CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT = "/%s/slim/request/%s";

    /** The channel to publish subscription requests to. */
    private static final String CHANNEL_SLIM_SUBSCRIBE = "/slim/subscribe";

    /** The format string for the channel to listen to for playerstatus evcents. */
    private static final String CHANNEL_PLAYER_STATUS_FORMAT = "/%s/slim/playerstatus/%s";

    /** The format string for the channel to listen to for serverstatus events. */
    private static final String CHANNEL_SERVER_STATUS_FORMAT = "/%s/slim/serverstatus";

    // Maximum time for wait for comet connect
    private static final int CONNECTION_TIMEOUT = 10000;


    // Maximum time for wait replies for server capabilities
    private static final long HANDSHAKE_TIMEOUT = 2000;


    /** Handler for off-main-thread work. */
    @NonNull
    private final Handler mBackgroundHandler;

    /** Map from a command ("players") to the listener class for responses. */
    private  final Map<String, ItemListener> mItemRequestMap;

    /** Client to the comet server. */
    @Nullable
    private BayeuxClient mBayeuxClient;

    private final Map<String, ClientSessionChannel.MessageListener> mPendingRequests
            = new ConcurrentHashMap<>();

    private final Map<String, BrowseRequest<? extends Item>> mPendingBrowseRequests
            = new ConcurrentHashMap<>();

    private final ClientSessionChannel.MessageListener publishListener = new PublishListener();

    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private volatile int mCorrelationId = 0;

    CometClient(@NonNull EventBus eventBus) {
        super(eventBus);

        HandlerThread handlerThread = new HandlerThread(SqueezeService.class.getSimpleName());
        handlerThread.start();
        mBackgroundHandler = new CliHandler(handlerThread.getLooper());

        mItemRequestMap = ImmutableMap.<String, ItemListener>builder()
                .put("artists", new ArtistsListener())
                .put("albums", new AlbumsListener())
                .put("songs", new SongsListener())
                .put("genres", new GenresListener())
                .put("years", new YearsListener())
                .put("playlists", new PlaylistsListener())
                .put("playlists tracks", new SongsListener())
                .put("musicfolder", new MusicFolderListener())
                .put("radios", new PluginListener("radioss_loop"))
                .put("apps", new PluginListener("appss_loop"))
                .put("alarms", new AlarmsListener())
                .put("alarm playlists", new AlarmPlaylistsListener())
                .put("status", new SongsListener("playlist_tracks", "playlist_loop"))
                .put("items", new PluginItemListener())
                .build();
    }

    // Shims around ConnectionState methods.
    public void startConnect(final SqueezeService service,
                             final String host, final int cliPort, final int httpPort,
                             final String userName, final String password) {
        Log.i(TAG, "Connecting to: " + userName + "@" + host + ":" + cliPort + "," + httpPort);
        if (!mEventBus.isRegistered(this)) {
            mEventBus.register(this);
        }
        mConnectionState.setConnectionState(ConnectionState.CONNECTION_STARTED);

        final HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            // XXX: Handle this properly. Maybe startConnect() should throw exceptions if the
            // connection fails?
            Log.e(TAG, "Can't start HttpClient", e);
            mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
            return;
        }

        // XXX: Need to deal with usernames and passwords, and HTTPS
        currentHost.set(host);

        // Start the background connect
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Background connect to: " + userName + "@" + host + ":" + cliPort + "," + httpPort);
                if (httpPort != 0) {
                    CometClient.this.httpPort.set(httpPort);
                } else {
                    try {
                        int port = new HttpPortLearner().learnHttpPort(host, cliPort, userName, password);
                        CometClient.this.httpPort.set(port);
                        new Preferences(service).saveHttpPort(port);
                    } catch (IOException e) {
                        Log.e(TAG, "Can't learn http port", e);
                        mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
                        return;
                    } catch (ServerDisconnectedException e) {
                        Log.i(TAG, "learnHttpPort", e);
                        mConnectionState.setConnectionState(ConnectionState.LOGIN_FAILED);
                        return;
                    }
                }

                mUrlPrefix = "http://" + getCurrentHost() + ":" + getHttpPort();
                String url = mUrlPrefix + "/cometd";

                Map<String, Object> options = new HashMap<>();
                options.put(HttpClientTransport.MAX_NETWORK_DELAY_OPTION, LONG_POLLING_TIMEOUT);
                ClientTransport httpTransport = new HttpStreamingTransport(options, httpClient);
                mBayeuxClient = new SqueezerBayeuxClient(url, httpTransport);
                mBayeuxClient.getChannel(Channel.META_HANDSHAKE).addListener(new ClientSessionChannel.MessageListener() {
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        if (!message.isSuccessful()) {
                            Log.w(TAG, Channel.META_HANDSHAKE + ": " + message.getJSON());
                            Map<String, Object> failure = (Map<String, Object>) message.get("failure");
                            if (failure != null) {
                                Message failMessage = (Message) failure.get("message");
                                Exception exception = (Exception) failure.get("exception");
                                Log.w(TAG, "Failure message: " + failMessage.getJSON(), exception);
                            }
                        }
                    }
                });

                mBayeuxClient.handshake();
                if (!mBayeuxClient.waitFor(CONNECTION_TIMEOUT, BayeuxClient.State.CONNECTED)) {
                    mBayeuxClient.disconnect();
                    mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
                    Log.i(TAG, "comet handshake TIMEOUT");
                    return;
                }

                // Connected from this point on.
                Log.i(TAG, "Connected, start learning server capabilities");
                mConnectionState.setConnectionState(ConnectionState.CONNECTION_COMPLETED);
                mConnectionState.setConnectionState(ConnectionState.LOGIN_STARTED);
                mConnectionState.setConnectionState(ConnectionState.LOGIN_COMPLETED);

                String clientId = mBayeuxClient.getId();

                mBayeuxClient.getChannel(String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "*")).subscribe(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        ClientSessionChannel.MessageListener listener = mPendingRequests.get(message.getChannel());
                        if (listener != null) {
                            listener.onMessage(channel, message);
                            mPendingRequests.remove(message.getChannel());
                        }
                    }
                });

                mBayeuxClient.getChannel(String.format(CHANNEL_PLAYER_STATUS_FORMAT, clientId, "*")).subscribe(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        parseStatus(message);
                    }
                });

                mBayeuxClient.getChannel(String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId)).subscribe(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        parseServerStatus(message);
                    }
                });

                publishMessage(null, CHANNEL_SLIM_SUBSCRIBE, String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId), null, "serverstatus", "0", "255",
                        "subscribe:0",
                        "prefs:ignoredarticles,browseagelimit,noGenreFilter,PLUGIN_TRACKSTAT,audiodir",
                        "playerprefs:playtrackalbum,digitalVolumeControl"
                );

                // Set a timeout for the handshake
                mBackgroundHandler.removeMessages(MSG_HANDSHAKE_TIMEOUT);
                mBackgroundHandler.sendEmptyMessageDelayed(MSG_HANDSHAKE_TIMEOUT, HANDSHAKE_TIMEOUT);

                // Learn server capabilites.
                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanMusicfolder(getInt(message.getDataAsMap().get("_can"))== 1);
                    }
                }, "can", "musicfolder", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanRandomplay(getInt(message.getDataAsMap().get("_can")) == 1);
                    }
                }, "can", "randomplay", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanFavorites(getInt(message.getDataAsMap().get("_can")) == 1);
                    }
                }, "can", "favorites", "items", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanMyApps(getInt(message.getDataAsMap().get("_can")) == 1);
                    }
                }, "can", "myapps", "items", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setMediaDirs((Object[]) message.getDataAsMap().get("_p2"));
                    }
                }, "pref", "mediadirs", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setPreferedAlbumSort((String) message.getDataAsMap().get("_p2"));
                    }
                }, "pref", "jivealbumsort", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setServerVersion((String) message.getDataAsMap().get("_version"));
                    }
                }, "version", "?");
            }
        });
    }


    private void parseServerStatus(Message message) {
        Map<String, Object> data = message.getDataAsMap();
        Object[] item_data = (Object[]) data.get("players_loop");
        final HashMap<String, Player> players = new HashMap<>();
        if (item_data != null) {
            for (Object item_d : item_data) {
                Map<String, Object> record = (Map<String, Object>) item_d;
                Player player = new Player(record);
                players.put(player.getId(), player);
            }
            if (!players.equals(mPlayers)) {
                mPlayers.clear();
                mPlayers.putAll(players);

                // XXX: postSticky?
                mEventBus.postSticky(new PlayersChanged(mPlayers));
            }
        }
    }

    private void parseStatus(Message message) {
        String[] channelParts = mSlashSplitPattern.split(message.getChannel());
        String playerId = channelParts[channelParts.length - 1];
        Player player = mPlayers.get(playerId);

        // XXX: Can we ever see a status for a player we don't know about?
        // XXX: Maybe the better thing to do is to add it.
        if (player == null)
            return;

        Object data = message.getData();
        if (data instanceof Map) {
            Map<String, Object> messageData = message.getDataAsMap();

            Song song = null;
            Object[] item_data = (Object[]) messageData.get("playlist_loop");
            if (item_data != null && item_data.length > 0) {
                Map<String, Object> record = (Map<String, Object>) item_data[0];
                song = new Song(record);
            }
            parseStatus(player, song, messageData);
        } else {
            Map<String, Object> tokenMap = new HashMap<>();
            Object[] tokens = (Object[]) data;
            if (Util.arraysStartsWith(tokens, new String[]{"status", "-", "subscribe:1", "1"})) {
                for (Object token : tokens) {
                    String[] split = mColonSplitPattern.split((String) token, 2);
                    tokenMap.put(split[0], split.length > 1 ? split[1] : null);
                }
                parseStatus(player, null, tokenMap);
            }
        }

    }

    private static class PublishListener implements ClientSessionChannel.MessageListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (!message.isSuccessful()) {
                // TODO crashlytics
                Log.e(TAG, message.getJSON());
            }
        }
    }

    private abstract class ItemListener<T extends Item> extends BaseListHandler<T> implements ClientSessionChannel.MessageListener {
        void parseMessage(String countName, String itemLoopName, Message message) {
            @SuppressWarnings("unchecked")
            BrowseRequest<T> browseRequest = (BrowseRequest<T>) mPendingBrowseRequests.get(message.getChannel());
            if (browseRequest == null) {
                return;
            }

            mPendingBrowseRequests.remove(message.getChannel());
            clear();
            Map<String, Object> data = message.getDataAsMap();
            int count = getInt(data.get(countName));
            Object[] item_data = (Object[]) data.get(itemLoopName);
            if (item_data != null) {
                for (Object item_d : item_data) {
                    Map<String, Object> record = (Map<String, Object>) item_d;
                    add(record);
                }
            }

            // Process the lists for all the registered handlers
            final boolean fullList = browseRequest.isFullList();
            final int start = browseRequest.getStart();
            final int end = start + browseRequest.getItemsPerResponse();
            int max = 0;
            browseRequest.getCallback().onItemsReceived(count, start, data, getItems(), getDataType());
            if (count > max) {
                max = count;
            }

            // Check if we need to order more items
            if ((fullList || end % mPageSize != 0) && end < max) {
                int itemsPerResponse = (end + mPageSize > max ? max - end : fullList ? mPageSize : mPageSize - browseRequest.getItemsPerResponse());
                //XXX support prefix
                internalRequestItems(browseRequest.update(end, itemsPerResponse));
            }
        }

        void parseMessage(String itemLoopName, Message message) {
            parseMessage("count", itemLoopName, message);
        }
    }

    private int getInt(Object value) {
        return (value instanceof Number) ? ((Number)value).intValue() : Util.parseDecimalIntOrZero((String)value);
    }

    private class ArtistsListener extends ItemListener<Artist> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("artists_loop", message);
        }
    }

    private class AlbumsListener extends ItemListener<Album> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("albums_loop", message);
        }

        @Override
        public void add(Map<String, Object> record) {
            addArtworkUrlTag(record);
            super.add(record);
        }
    }

    private class SongsListener extends ItemListener<Song> {
        private String countName = "count";
        private String itemLoopName = "titles_loop";

        SongsListener() {
        }

        SongsListener(String countName, String itemLoopName) {
            this.countName = countName;
            this.itemLoopName = itemLoopName;
        }

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage(countName, itemLoopName, message);
        }

        @Override
        public void add(Map<String, Object> record) {
            addArtworkUrlTag(record);
            addDownloadUrlTag(record);
            super.add(record);
        }
    }

    private class GenresListener extends ItemListener<Genre> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("genres_loop", message);
        }
    }

    private class YearsListener extends ItemListener<Year> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("years_loop", message);
        }
    }

    private class PlaylistsListener extends ItemListener<Playlist> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("playlists_loop", message);
        }
    }

    private class MusicFolderListener extends ItemListener<MusicFolderItem> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("folder_loop", message);
        }

        @Override
        public void add(Map<String, Object> record) {
            addDownloadUrlTag(record);
            super.add(record);
        }
    }

    private class AlarmsListener extends ItemListener<Alarm> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("alarms_loop", message);
        }
    }

    private class AlarmPlaylistsListener extends ItemListener<AlarmPlaylist> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("playlists_loop", message);
        }
    }

    private class PluginListener extends ItemListener<Plugin> {
        private String itemLoopName;

        PluginListener(String itemLoopName) {
            this.itemLoopName = itemLoopName;
        }

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage(itemLoopName, message);
        }
    }

    private class PluginItemListener extends ItemListener<PluginItem> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("loop_loop", message);
        }
    }

    public void onEvent(HandshakeComplete event) {
        mBackgroundHandler.removeMessages(MSG_HANDSHAKE_TIMEOUT);
        request(new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                int maxOrdinal = 0;
                Map<String, Object> tokenMap = message.getDataAsMap();
                for (Map.Entry<String, Object> entry : tokenMap.entrySet()) {
                    if (entry.getValue() != null) {
                        ServerString serverString = ServerString.valueOf(entry.getKey());
                        serverString.setLocalizedString(entry.getValue().toString());
                        if (serverString.ordinal() > maxOrdinal) {
                            maxOrdinal = serverString.ordinal();
                        }
                    }
                }

                // Fetch the next strings until the list is completely translated
                if (maxOrdinal < ServerString.values().length - 1) {
                    request(this,"getstring", ServerString.values()[maxOrdinal + 1].name());
                }
            }
        }, "getstring",  ServerString.values()[0].name());
    }

    @Override
    public void disconnect(boolean loginFailed) {
        mBackgroundHandler.sendEmptyMessage(MSG_DISCONNECT);
        mConnectionState.disconnect(loginFailed);
        mPlayers.clear();
    }

    @Override
    public void command(Player player, String command) {
        request(player, null, mSpaceSplitPattern.split(command));
    }

    @Override
    public void cancelClientRequests(Object object) {

    }

    private String request(ClientSessionChannel.MessageListener callback, String... cmd) {
        return request(null, callback, cmd);
    }

    private String request(final Player player, ClientSessionChannel.MessageListener callback, String... cmd) {
        String responseChannel = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, mBayeuxClient.getId(), mCorrelationId++);
        if (callback != null) mPendingRequests.put(responseChannel, callback);
        publishMessage(player, CHANNEL_SLIM_REQUEST, responseChannel, null, cmd);
        return responseChannel;
    }

    private void publishMessage(final Player player, final String channel, final String responseChannel, final ClientSessionChannel.MessageListener publishListener, final String... cmd) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            _publishMessage(player, channel, responseChannel, publishListener, cmd);
        } else {
            PublishMessage publishMessage = new PublishMessage(player, channel, responseChannel, publishListener, cmd);
            android.os.Message message = mBackgroundHandler.obtainMessage(MSG_PUBLISH, publishMessage);
            mBackgroundHandler.sendMessage(message);
        }

    }

    private void _publishMessage(Player player, String channel, String responseChannel, ClientSessionChannel.MessageListener publishListener, String... cmd) {
        List<Object> request = new ArrayList<>();
        request.add(player == null ? "" : player.getId());
        request.add(cmd);

        Map<String, Object> data = new HashMap<>();
        data.put("request", request);
        data.put("response", responseChannel);

        mBayeuxClient.getChannel(channel).publish(data, publishListener != null ? publishListener : this.publishListener);
    }

    @Override
    protected  <T extends Item> void internalRequestItems(final BrowseRequest<T> browseRequest) {
        final ItemListener listener = mItemRequestMap.get(browseRequest.getRequest());

        final List<String> request = new ArrayList<>(browseRequest.getParameters().size() + 3);
        if (browseRequest.getPlugin() != null) request.add(browseRequest.getPlugin().getId());
        request.addAll(Arrays.asList(mSpaceSplitPattern.split(browseRequest.getRequest())));
        request.add(String.valueOf(browseRequest.getStart()));
        request.add(String.valueOf(browseRequest.getItemsPerResponse()));
        for (String parameter : browseRequest.getParameters()) {
            request.add(parameter);
        }

        final String[] cmd = request.toArray(new String[request.size()]);
        mPendingBrowseRequests.put(request(browseRequest.getPlayer(), listener, cmd), browseRequest);
    }

    @Override
    public void requestPlayerStatus(Player player) {
        publishMessage(player, CHANNEL_SLIM_REQUEST, playerStatusResponseChannel(player), null, "status", "-", "1", "tags:" + SONGTAGS);
    }

    @Override
    public void subscribePlayerStatus(final Player player, final String subscriptionType) {
        publishMessage(player, CHANNEL_SLIM_SUBSCRIBE, playerStatusResponseChannel(player), new PublishListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                super.onMessage(channel, message);
                if (message.isSuccessful()) {
                    player.getPlayerState().setSubscriptionType(subscriptionType);
                }
            }
        }, "status", "-", "1", "subscribe:" + subscriptionType, "tags:" + SONGTAGS);
    }

    private String playerStatusResponseChannel(Player player) {
        return String.format(CHANNEL_PLAYER_STATUS_FORMAT, mBayeuxClient.getId(), player.getId());
    }

    @Override
    public String encode(String s) {
        return s;
    }

    private static final int MSG_PUBLISH = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_HANDSHAKE_TIMEOUT = 3;
    private class CliHandler extends Handler {
        CliHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_PUBLISH:
                    PublishMessage message = (PublishMessage) msg.obj;
                    _publishMessage(message.player, message.channel, message.responseChannel, message.publishListener, message.cmd);
                    break;
                case MSG_DISCONNECT:
                    mBayeuxClient.disconnect();
                    break;
                case MSG_HANDSHAKE_TIMEOUT:
                    Log.w(TAG, "LMS handshake timeout");
                    disconnect(false);
                    break;
            }
        }
    }

    private static class PublishMessage {
        final Player player;
        final String channel;
        final String responseChannel;
        final ClientSessionChannel.MessageListener publishListener;
        final String[] cmd;

        private PublishMessage(Player player, String channel, String responseChannel, ClientSessionChannel.MessageListener publishListener, String... cmd) {
            this.player = player;
            this.channel = channel;
            this.responseChannel = responseChannel;
            this.publishListener = publishListener;
            this.cmd = cmd;
        }
    }
}
