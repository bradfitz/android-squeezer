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
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.B64Code;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
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
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayerPrefReceived;
import uk.org.ngo.squeezer.service.event.PlayerVolume;
import uk.org.ngo.squeezer.service.event.PlayersChanged;
import uk.org.ngo.squeezer.service.event.PlaylistRenameFailed;

class CometClient extends BaseClient {
    private static final String TAG = CometClient.class.getSimpleName();

    /** The maximum number of milliseconds to wait before considering a request to the LMS failed */
    private static final int LONG_POLLING_TIMEOUT = 120_000;

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

    // Maximum time for wait replies for server capabilities
    private static final long HANDSHAKE_TIMEOUT = 2000;


    /** Handler for off-main-thread work. */
    @NonNull
    private final Handler mBackgroundHandler;

    /** Map from an item request command ("players") to the listener class for responses. */
    private  final Map<String, ItemListener> mItemRequestMap;

    /** Map from a request to the listener class for responses. */
    private  final Map<String, ResponseHandler> mRequestMap;

    /** Client to the comet server. */
    @Nullable
    private BayeuxClient mBayeuxClient;

    private final Map<String, Request> mPendingRequests
            = new ConcurrentHashMap<>();

    private final Map<String, BrowseRequest<? extends Item>> mPendingBrowseRequests
            = new ConcurrentHashMap<>();

    private final Queue<PublishMessage> mCommandQueue = new LinkedList<>();
    private boolean mCurrentCommand = false;

    private final PublishListener publishListener = new PublishListener();

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
                .put("playlists tracks", new SongsListener("count", "playlisttracks_loop"))
                .put("musicfolder", new MusicFolderListener())
                .put("alarms", new AlarmsListener())
                .put("alarm playlists", new AlarmPlaylistsListener())
                .put("status", new SongsListener("playlist_tracks", "playlist_loop"))
                .build();

        mRequestMap = ImmutableMap.<String, ResponseHandler>builder()
                .put("playerpref", new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        //noinspection WrongConstant
                        String p2 = (String) message.getDataAsMap().get("_p2");
                        mEventBus.post(new PlayerPrefReceived(player, request.cmd[1], p2 != null ? p2 : request.cmd[2]));
                    }
                })
                .put("mixer", new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        if (request.cmd[1].equals("volume")) {
                            String volume = (String) message.getDataAsMap().get("_volume");
                            if (volume != null) {
                                int newVolume = Integer.valueOf(volume);
                                player.getPlayerState().setCurrentVolume(newVolume);
                                mEventBus.post(new PlayerVolume(newVolume, player));
                            } else {
                                command(player, new String[]{"mixer", "volume", "?"});
                            }
                        }
                    }
                })
                .put("playlists", new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        if (request.cmd.length >= 2 && "rename".equals(request.cmd[1])) {
                            if (request.params.containsKey("dry_run")) {
                                if (message.getDataAsMap().get("overwritten_playlist_id") != null) {
                                    mEventBus.post(new PlaylistRenameFailed(Squeezer.getContext().getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                              request.params.get("newname"))));
                                } else {
                                    Map<String, Object> params = new HashMap<>(request.params);
                                    params.remove("dry_run");
                                    command(request.cmd, params);
                                }
                            }
                        }
                    }
                })
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

        // XXX: Need to deal with HTTPS
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
                        Log.i(TAG, "learnHttpPort: " + e.getMessage());
                        mConnectionState.setConnectionState(ConnectionState.LOGIN_FAILED);
                        return;
                    }
                }

                mUrlPrefix = "http://" + getCurrentHost() + ":" + getHttpPort();
                String url = mUrlPrefix + "/cometd";

                // Set the VM-wide authentication handler (needed by image fetcher and other using
                // the standard java http API)
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userName, password.toCharArray());
                    }
                });

                Map<String, Object> options = new HashMap<>();
                options.put(HttpClientTransport.MAX_NETWORK_DELAY_OPTION, LONG_POLLING_TIMEOUT);
                ClientTransport httpTransport = new HttpStreamingTransport(options, httpClient) {
                    @Override
                    protected List<HttpField> customHeaders() {
                        String authorization = B64Code.encode(userName + ":" + password);
                        return Collections.singletonList(new HttpField(HttpHeader.AUTHORIZATION, "Basic " + authorization));
                    }
                };
                mBayeuxClient = new SqueezerBayeuxClient(url, httpTransport);
                mBayeuxClient.getChannel(Channel.META_HANDSHAKE).addListener(new ClientSessionChannel.MessageListener() {
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        if (message.isSuccessful()) {
                            onConnected();
                        } else {
                            Log.w(TAG, channel + ": " + message.getJSON());

                            Map<String, Object> failure = (Map<String, Object>) message.get("failure");
                            Object httpCodeValue = failure.get("httpCode");
                            int httpCode = (httpCodeValue instanceof Integer) ? (int) httpCodeValue : -1;

                            mBayeuxClient.disconnect();
                            mConnectionState.setConnectionState((httpCode == 401) ? ConnectionState.LOGIN_FAILED : ConnectionState.CONNECTION_FAILED);
                        }
                    }
                });

                mBayeuxClient.handshake();
            }

            private void onConnected() {
                Log.i(TAG, "Connected, start learning server capabilities");
                mConnectionState.setConnectionState(ConnectionState.CONNECTION_COMPLETED);
                mConnectionState.setConnectionState(ConnectionState.LOGIN_STARTED);
                mConnectionState.setConnectionState(ConnectionState.LOGIN_COMPLETED);

                String clientId = mBayeuxClient.getId();

                mBayeuxClient.getChannel(String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "*")).subscribe(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        Request request = mPendingRequests.get(message.getChannel());
                        if (request != null) {
                            request.callback.onResponse(request.player, request, message);
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

                // Request server status
                publishMessage(new Request("serverstatus", "0", "255"), CHANNEL_SLIM_REQUEST, String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId), null);

                // Subscribe to server changes
                {
                    Request request = new Request(null, new String[]{"serverstatus", "0", "255"}, Collections.singletonMap("subscribe", (Object) "0"));
                    publishMessage(request, CHANNEL_SLIM_SUBSCRIBE, String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId), null);
                }

                // Set a timeout for the handshake
                mBackgroundHandler.removeMessages(MSG_HANDSHAKE_TIMEOUT);
                mBackgroundHandler.sendEmptyMessageDelayed(MSG_HANDSHAKE_TIMEOUT, HANDSHAKE_TIMEOUT);

                // Learn server capabilites.
                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        mConnectionState.setCanMusicfolder(getInt(message.getDataAsMap().get("_can"))== 1);
                    }
                }, "can", "musicfolder", "?");

                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        mConnectionState.setCanRandomplay(getInt(message.getDataAsMap().get("_can")) == 1);
                    }
                }, "can", "randomplay", "?");

                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        mConnectionState.setCanFavorites(getInt(message.getDataAsMap().get("_can")) == 1);
                    }
                }, "can", "favorites", "items", "?");

                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        mConnectionState.setCanMyApps(getInt(message.getDataAsMap().get("_can")) == 1);
                    }
                }, "can", "myapps", "items", "?");

                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        mConnectionState.setMediaDirs((Object[]) message.getDataAsMap().get("_p2"));
                    }
                }, "pref", "mediadirs", "?");

                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
                        mConnectionState.setPreferedAlbumSort((String) message.getDataAsMap().get("_p2"));
                    }
                }, "pref", "jivealbumsort", "?");

                request(new ResponseHandler() {
                    @Override
                    public void onResponse(Player player, Request request, Message message) {
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
            String[] tokens = Util.getStringArray((Object[]) data);
            if (Util.arraysStartsWith(tokens, new String[]{"status", "-", "subscribe:1", "1"})) {
                parseStatus(player, null, Util.mapify(tokens));
            }
        }

    }

    private interface ResponseHandler {
        void onResponse(Player player, Request request, Message message);
    }

    private class PublishListener implements ClientSessionChannel.MessageListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (!message.isSuccessful()) {
                // TODO crashlytics and possible other handling
                Log.e(TAG, channel + ": " + message.getJSON());
            }
            mBackgroundHandler.sendEmptyMessage(MSG_PUBLISH_RESPONSE_RECIEVED);
        }
    }

    private abstract class ItemListener<T extends Item> extends BaseListHandler<T> implements ResponseHandler {
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
            Object baseRecord = data.get("base");
            Object[] item_data = (Object[]) data.get(itemLoopName);
            if (item_data != null) {
                for (Object item_d : item_data) {
                    Map<String, Object> record = (Map<String, Object>) item_d;
                    record.put("base", baseRecord);
                    fixImageTag(record);
                    add(record);
                    record.remove("base");
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
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("artists_loop", message);
        }
    }

    private class AlbumsListener extends ItemListener<Album> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
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
        public void onResponse(Player player, Request request, Message message) {
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
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("genres_loop", message);
        }
    }

    private class YearsListener extends ItemListener<Year> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("years_loop", message);
        }
    }

    private class PlaylistsListener extends ItemListener<Playlist> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("playlists_loop", message);
        }
    }

    private class MusicFolderListener extends ItemListener<MusicFolderItem> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
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
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("alarms_loop", message);
        }
    }

    private class AlarmPlaylistsListener extends ItemListener<AlarmPlaylist> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("item_loop", message);
        }
    }

    private class PluginListener extends ItemListener<Plugin> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("item_loop", message);
        }
    }

    public void onEvent(HandshakeComplete event) {
        mBackgroundHandler.removeMessages(MSG_HANDSHAKE_TIMEOUT);
        request(new ResponseHandler() {
            @Override
            public void onResponse(Player player, Request request, Message message) {
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
        if (mBayeuxClient != null) mBackgroundHandler.sendEmptyMessage(MSG_DISCONNECT);
        mConnectionState.disconnect(loginFailed);
        mPlayers.clear();
    }

    @Override
    public void cancelClientRequests(Object object) {
        //TODO see CliClient
    }

    private String request(ResponseHandler callback, String... cmd) {
        return request(new Request(null, cmd, callback));
    }

    private String request(Request request) {
        String responseChannel = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, mBayeuxClient.getId(), mCorrelationId++);
        if (request.callback != null) mPendingRequests.put(responseChannel, request);
        publishMessage(request, CHANNEL_SLIM_REQUEST, responseChannel, null);
        return responseChannel;
    }

    private void publishMessage(final Request request, final String channel, final String responseChannel, final PublishListener publishListener) {
        // Make sure all requests are done in the handler thread
        if (mBackgroundHandler.getLooper() == Looper.myLooper()) {
            _publishMessage(request, channel, responseChannel, publishListener);
        } else {
            PublishMessage publishMessage = new PublishMessage(request, channel, responseChannel, publishListener);
            android.os.Message message = mBackgroundHandler.obtainMessage(MSG_PUBLISH, publishMessage);
            mBackgroundHandler.sendMessage(message);
        }

    }

    /** This may only be called from the handler thread */
    private void _publishMessage(Request request, String channel, String responseChannel, PublishListener publishListener) {
        if (!mCurrentCommand) {
            mCurrentCommand = true;
            List<Object> ttt = new ArrayList<>();
            ttt.add(request.player == null ? "" : request.player.getId());
            List<String> inner = new ArrayList<>();
            ttt.add(inner);
            inner.addAll(Arrays.asList(request.cmd));
            for (Map.Entry<String, Object> parameter : request.params.entrySet()) {
                inner.add(parameter.getKey() + ":" + parameter.getValue());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("request", ttt);
            data.put("response", responseChannel);
            mBayeuxClient.getChannel(channel).publish(data, publishListener != null ? publishListener : this.publishListener);
        } else
            mCommandQueue.add(new PublishMessage(request, channel, responseChannel, publishListener));
    }

    @Override
    protected  <T extends Item> void internalRequestItems(final BrowseRequest<T> browseRequest) {
        ItemListener listener = mItemRequestMap.get(browseRequest.getRequest());
        if (listener == null) listener = new PluginListener();

        final List<String> request = new ArrayList<>();
        request.addAll(Arrays.asList(browseRequest.getCmd()));
        request.add(String.valueOf(browseRequest.getStart()));
        request.add(String.valueOf(browseRequest.getItemsPerResponse()));

        String responseChannel = request(new Request(browseRequest.getPlayer(), request, browseRequest.getParams(), listener));
        mPendingBrowseRequests.put(responseChannel, browseRequest);
    }

    @Override
    public void command(Player player, String[] cmd, Map<String, Object> params) {
        ResponseHandler callback = mRequestMap.get(cmd[0]);
        request(new Request(player, cmd, params, callback));
    }

    @Override
    public void requestPlayerStatus(Player player) {
        Request request = new Request(player, new String[]{"status", "-", "1"}, Collections.singletonMap("tags", (Object) SONGTAGS));
        publishMessage(request, CHANNEL_SLIM_REQUEST, playerStatusResponseChannel(player), null);
    }

    @Override
    public void subscribePlayerStatus(final Player player, final String subscriptionType) {
        Map<String, Object> params = new HashMap<>();
        params.put("subscribe:", subscriptionType);
        params.put("tags:", SONGTAGS);
        Request request = new Request(player, new String[]{"status", "-", "1"}, params);
        publishMessage(request, CHANNEL_SLIM_SUBSCRIBE, playerStatusResponseChannel(player), new PublishListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                super.onMessage(channel, message);
                if (message.isSuccessful()) {
                    player.getPlayerState().setSubscriptionType(subscriptionType);
                }
            }
        });
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
    private static final int MSG_PUBLISH_RESPONSE_RECIEVED = 4;
    private class CliHandler extends Handler {
        CliHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_PUBLISH: {
                    PublishMessage message = (PublishMessage) msg.obj;
                    _publishMessage(message.request, message.channel, message.responseChannel, message.publishListener);
                    break;
                }
                case MSG_DISCONNECT:
                    mBayeuxClient.disconnect();
                    break;
                case MSG_HANDSHAKE_TIMEOUT:
                    Log.w(TAG, "LMS handshake timeout");
                    disconnect(false);
                    break;
                case MSG_PUBLISH_RESPONSE_RECIEVED: {
                    mCurrentCommand = false;
                    PublishMessage message = mCommandQueue.poll();
                    if (message != null)
                        _publishMessage(message.request, message.channel, message.responseChannel, message.publishListener);
                    break;
                }
            }
        }
    }


    private static class Request {
        final private ResponseHandler callback;
        final private Player player;
        final private String[] cmd;
        final private Map<String, Object> params;

        private Request(Player player, String[] cmd, Map<String, Object> params, ResponseHandler callback) {
            this.player = player;
            this.cmd = cmd;
            this.params = params;
            this.callback = callback;
        }

        private Request(Player player, List<String> cmd, Map<String, Object> params, ResponseHandler callback) {
            this(player, cmd.toArray(new String[cmd.size()]), params, callback);
        }

        private Request(Player player, String[] cmd, Map<String, Object> params) {
            this(player, cmd, params, null);
        }

        private Request(Player player, String[] cmd, ResponseHandler callback) {
            this(player, cmd, Collections.<String, Object>emptyMap(), callback);
        }

        private Request(Player player, String... cmd) {
            this(player, cmd, Collections.<String, Object>emptyMap());
        }

        private Request(String... cmd) {
            this(null, cmd);
        }
    }

    private static class PublishMessage {
        final Request request;
        final String channel;
        final String responseChannel;
        final PublishListener publishListener;

        private PublishMessage(Request request, String channel, String responseChannel, PublishListener publishListener) {
            this.request = request;
            this.channel = channel;
            this.responseChannel = responseChannel;
            this.publishListener = publishListener;
        }
    }
}
