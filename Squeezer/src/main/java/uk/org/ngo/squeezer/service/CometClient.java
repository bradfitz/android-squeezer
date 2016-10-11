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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.collect.ImmutableMap;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.event.PlayersChanged;

public class CometClient extends BaseClient {
    private static final String TAG = CometClient.class.getSimpleName();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<>();
    private final AtomicReference<Integer> httpPort = new AtomicReference<>();

    /** Client to the comet server. */
    @Nullable
    private BayeuxClient mBayeuxClient;

    /** {@link java.util.regex.Pattern} that splits strings on colon. */
    private static final Pattern mColonSplitPattern = Pattern.compile(":");

    /** {@link java.util.regex.Pattern} that splits strings on spaces. */
    private static final Pattern mSpaceSplitPattern = Pattern.compile(" ");

    /** {@link java.util.regex.Pattern} that splits strings on forward slash. */
    private static final Pattern mSlashSplitPattern = Pattern.compile("/");

    /** The channel to publish one-shot requests to. */
    private static final String CHANNEL_SLIM_REQUEST = "/slim/request";

    /** Map from a command ("players") to the listener class for responses. */
    private  final Map<String, SlimCommand> mCommandMap;

    /** The format string for the channel to listen to for responses to one-shot requests. */
    private static final String CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT = "/%s/slim/request/%s";

    /** The channel to publish subscription requests to. */
    private static final String CHANNEL_SLIM_SUBSCRIBE = "/slim/subscribe";

    /** The format string for the channel to listen to for responses to playerstatus requests. */
    private static final String CHANNEL_PLAYER_STATUS_RESPONSE_FORMAT = "/%s/slim/playerstatus/%s";

    private Map<String, Player> mPlayers = new HashMap<>();

    private final Map<String, ClientSessionChannel.MessageListener> mPendingRequests
            = new ConcurrentHashMap<>();

    private final Map<String, BrowseRequest<? extends Item>> mPendingBrowseRequests
            = new ConcurrentHashMap<>();

    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private volatile int mCorrelationId = 0;

    CometClient(@NonNull EventBus eventBus) {
        super(eventBus);

        mCommandMap = ImmutableMap.<String, SlimCommand>builder()
                .put("players", new SlimCommand(new PlayersListener()))
                .put("artists", new SlimCommand(new ArtistsListener()))
                .put("albums", new SlimCommand(new AlbumsListener()))
                .put("songs", new SlimCommand(new SongsListener()))
                .put("genres", new SlimCommand(new GenresListener()))
                .put("years", new SlimCommand(new YearsListener()))

                //XXX status is both request and subscribe
                .put("status", new SlimCommand() {
                    private ClientSessionChannel.MessageListener statusListener = new StatusListener();

                    @Override
                    public String[] getChannels(Player player, String... cmd) {
                        if (isCurrentPlaylistRequest(cmd)) return super.getChannels(player, cmd);

                        String[] channels = new String[2];
                        boolean subscribe = isSubscribe(cmd);
                        // If this is not a subscription, use the request channel.
                        channels[0] = subscribe ? CHANNEL_SLIM_SUBSCRIBE : CHANNEL_SLIM_REQUEST;
                        channels[1] = String.format(CHANNEL_PLAYER_STATUS_RESPONSE_FORMAT, mBayeuxClient.getId(), player.getId());

                        return channels;
                    }

                    @Override
                    public ClientSessionChannel.MessageListener getListener(String... cmd) {
                        return (isCurrentPlaylistRequest(cmd) ? statusListener : null);
                    }

                    private boolean isSubscribe(String... tokens) {
                        for (String token : tokens) {
                            if (token.contains("subscribe:")) return true;
                        }
                        return false;
                    }

                    private boolean isCurrentPlaylistRequest(String[] cmd) {
                        return !cmd[1].equals("-");
                    }
                })

                .build();
    }

    // Shims around ConnectionState methods.
    public void startConnect(final SqueezeService service, String hostPort, final String userName,
                             final String password) {
        mConnectionState.setConnectionState(ConnectionState.CONNECTION_STARTED);

        HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            // XXX: Handle this properly. Maybe startConnect() should throw exceptions if the
            // connection fails?
            e.printStackTrace();
            mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
            return;
        }

        // XXX: Need to split apart hostPort, and provide a config mechanism that can
        // distinguish between CLI and Comet.
        // XXX: Also need to deal with usernames and passwords, and HTTPS
        //String url = String.format("http://%s/cometd", hostPort);
        final String host = Util.parseHost(hostPort);
        currentHost.set(host);
        httpPort.set(9001);  // hardcoded for now.
        mUrlPrefix = "http://" + getCurrentHost() + ":" + getHttpPort();
        String url = mUrlPrefix + "/cometd";

        Map<String, Object> options = new HashMap<>();
        ClientTransport transport = new LongPollingTransport(options, httpClient);
        mBayeuxClient = new SqueezerBayeuxClient(url, transport);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mBayeuxClient.handshake();
                if (!mBayeuxClient.waitFor(10000, BayeuxClient.State.CONNECTED)) {
                    mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
                    return;  // XXX: Check if returning here is the right thing to do? Any other cleanup?
                }

                // Connected from this point on.
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

                mBayeuxClient.getChannel(String.format(CHANNEL_PLAYER_STATUS_RESPONSE_FORMAT, clientId, "*")).subscribe(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        parseStatus(message);
                    }
                });

                //mConnectionState.startConnect(service, mEventBus, mExecutor, this, hostPort, userName, password);

                // There's no persistent connection to manage.  Run the connection state machine
                // so that the UI gets in to a consistent state
                fetchPlayers();

                // XXX: "listen 1" -- serverstatus?

                // Learn server capabilites.

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanMusicfolder("1".equals(message.getDataAsMap().get("_can")));
                    }
                }, "can", "musicfolder", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanRandomplay("1".equals(message.getDataAsMap().get("_can")));
                    }
                }, "can", "randomplay", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanFavorites("1".equals(message.getDataAsMap().get("_can")));
                    }
                }, "can", "favorites", "items", "?");

                request(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        mConnectionState.setCanMyApps("1".equals(message.getDataAsMap().get("_can")));
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

                // XXX: Skipped "pref httpport ?"

//                sendCommandImmediately(
//                        "listen 1", // subscribe to all server notifications
//                        "can musicfolder ?", // learn music folder browsing support
//                        "can randomplay ?", // learn random play function functionality
//                        "can favorites items ?", // learn support for "Favorites" plugin
//                        "can myapps items ?", // learn support for "MyApps" plugin
//                        "pref httpport ?", // learn the HTTP port (needed for images)
//                        "pref jivealbumsort ?", // learn the preferred album sort order
//                        "pref mediadirs ?", // learn the base path(s) of the server music library
//
//                        // Fetch the version number. This must be the last thing
//                        // fetched, as seeing the result triggers the
//                        // "handshake is complete" logic elsewhere.
//                        "version ?"
            }
        });
    }

    private void parseStatus(Message message) {
        String[] channelParts = mSlashSplitPattern.split(message.getChannel());
        String playerId = channelParts[channelParts.length - 1];
        Player player = mPlayers.get(playerId);

        // XXX: Can we ever see a status for a player we don't know about?
        // XXX: Maybe the better thing to do is to add it.
        if (player == null)
            return;

        Map<String, String> tokenMap = new HashMap<>();
        Object data = message.getData();
        if (data instanceof Map) {
            Map<String, Object> messageData = message.getDataAsMap();
            for (Map.Entry<String, Object> entry : messageData.entrySet()) {
                Object value = entry.getValue();
                tokenMap.put(entry.getKey(), value != null && !(value instanceof String) ? value.toString() : (String)value);
            }

            Song song = null;
            Object[] item_data = (Object[]) messageData.get("playlist_loop");
            if (item_data != null && item_data.length > 0) {
                Map<String, String> record = (Map<String, String>) item_data[0];
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !(value instanceof String)) {
                        record.put(entry.getKey(), value.toString());
                    }
                }
                song = new Song(record);
            }
            parseStatus(player, song, tokenMap);
        } else {
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

    abstract class ItemListener<T extends Item> extends BaseListHandler<T> implements ClientSessionChannel.MessageListener {
        protected void parseMessage(String countName, String itemLoopName, Message message) {
            @SuppressWarnings("unchecked")
            BrowseRequest<T> browseRequest = (BrowseRequest<T>) mPendingBrowseRequests.get(message.getChannel());
            if (browseRequest == null) {
                return;
            }

            mPendingBrowseRequests.remove(message.getChannel());
            clear();
            Map<String, Object> data = message.getDataAsMap();
            int count = ((Long) data.get(countName)).intValue();
            Object[] item_data = (Object[]) data.get(itemLoopName);
            if (item_data != null) {
                for (Object item_d : item_data) {
                    Map<String, String> record = (Map<String, String>) item_d;
                    for (Map.Entry<String, String> entry : record.entrySet()) {
                        Object value = entry.getValue();
                        if (value != null && !(value instanceof String)) {
                            record.put(entry.getKey(), value.toString());
                        }
                    }
                    add(record);
                }
            }

            // Process the lists for all the registered handlers
            final boolean fullList = browseRequest.isFullList();
            final int start = browseRequest.getStart();
            final int end = start + browseRequest.getItemsPerResponse();
            int max = 0;
            //XXX support returned parameters
            browseRequest.getCallback().onItemsReceived(count, start, null, getItems(), getDataType());
            if (count > max) {
                max = count;
            }

            // Check if we need to order more items
            if ((fullList || end % mPageSize != 0) && end < max) {
                int itemsPerResponse = (end + mPageSize > max ? max - end : fullList ? mPageSize : mPageSize - browseRequest.getItemsPerResponse());
                //XXX support playerid and prefix
                internalRequestItems(browseRequest.update(end, itemsPerResponse));
            }
        }

        protected void parseMessage(String itemLoopName, Message message) {
            parseMessage("count", itemLoopName, message);
        }
    }

    private class PlayersListener extends ItemListener<Player> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            // XXX: Sanity check that the message contains an ID and players_loop
            parseMessage("players_loop", message);
        }
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
    }

    private class SongsListener extends ItemListener<Song> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("titles_loop", message);
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

    private class StatusListener extends ItemListener<Song> {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            parseMessage("playlist_tracks", "playlist_loop", message);
        }
    }

    @Override
    public void disconnect(boolean loginFailed) {
        mBayeuxClient.disconnect();
        mConnectionState.disconnect(loginFailed);
        mPlayers.clear();
    }

    @Override
    public void sendCommandImmediately(String... commands) {

    }

    @Override
    public void sendCommand(String command) {
        sendPlayerCommand(null, command);
    }

    @Override
    public void sendPlayerCommand(Player player, String command) {
        String[] tokens = mSpaceSplitPattern.split(command);
        String cmd = tokens[0];
        SlimCommand slimCommand = mCommandMap.get(cmd);
        if (slimCommand == null) slimCommand = new SlimCommand();
        sendCometMessage(player, slimCommand, tokens);
    }

    private int getHttpPort() {
        return httpPort.get();
    }

    private String getCurrentHost() {
        return currentHost.get();
    }

    @Override
    public String[] getMediaDirs() {
        return mConnectionState.getMediaDirs();
    }


    /**
     * Queries for all players known by the server.
     * </p>
     * Posts a PlayersChanged message if the list of players has changed.
     */
    // XXX Copied from CliClient.
    private void fetchPlayers() {
        requestItems("players", -1, new IServiceItemListCallback<Player>() {
            private final HashMap<String, Player> players = new HashMap<>();

            @Override
            public void onItemsReceived(int count, int start, Map<String, String> parameters,
                                        List<Player> items, Class<Player> dataType) {
                for (Player player : items) {
                    players.put(player.getId(), player);
                }

                // If all players have been received then determine the new active player.
                if (start + items.size() >= count) {
                    if (players.equals(mPlayers)) {
                        return;
                    }

                    mPlayers.clear();
                    mPlayers.putAll(players);

                    // XXX: postSticky?
                    mEventBus.postSticky(new PlayersChanged(mPlayers));
                }
            }

            // XXX modified to change CliClient.this -> JsonClient.this.
            @Override
            public Object getClient() {
                return CometClient.this;
            }
        });
    }

    @Override
    public void onLineReceived(String serverLine) {

    }

    @Override
    public String getPreferredAlbumSort() {
        return mConnectionState.getPreferredAlbumSort();
    }

    @Override
    public void cancelClientRequests(Object object) {

    }

    private class SlimCommand {
        private final ClientSessionChannel.MessageListener listener;

        /** We don't wan't a response => no message listener */
        public SlimCommand() {
            this.listener = null;
        }

        /** Callback for the request response */
        public SlimCommand(ClientSessionChannel.MessageListener listener) {
            this.listener = listener;
        }

        /** 0: request channel, 1: response channel */
        public String[] getChannels(Player player, String ... cmd) {
            String channels[] = new String[2];
            channels[0] = CHANNEL_SLIM_REQUEST;
            channels[1] = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, mBayeuxClient.getId(), mCorrelationId++);

            return channels;
        }

        public ClientSessionChannel.MessageListener getListener(String... cmd) {
            return listener;
        }
    }

    private static class BrowseRequest<T extends Item> {
        private final Player player;
        private final String cmd;
        private final boolean fullList;
        private int start;
        private int itemsPerResponse;
        private final List<String> parameters;
        private final IServiceItemListCallback<T> callback;

        public BrowseRequest(Player player, String cmd, int start, int itemsPerResponse, List<String> parameters, IServiceItemListCallback<T> callback) {
            this.player = player;
            this.cmd = cmd;
            this.fullList = (start < 0);
            this.start = (fullList ? 0 : start);
            this.itemsPerResponse = (start == 0 ? 1 : itemsPerResponse);
            this.parameters = parameters;
            this.callback = callback;
        }

        public BrowseRequest update(int start, int itemsPerResponse) {
            this.start = start;
            this.itemsPerResponse = itemsPerResponse;
            return this;
        }

        public String getCmd() {
            return cmd;
        }

        public boolean isFullList() {
            return (fullList);
        }

        public int getStart() {
            return start;
        }

        public int getItemsPerResponse() {
            return itemsPerResponse;
        }

        public List<String> getParameters() {
            return (parameters == null ? Collections.<String>emptyList() : parameters);
        }

        public IServiceItemListCallback<T> getCallback() {
            return callback;
        }
    }

    private String request(ClientSessionChannel.MessageListener callback, String... cmd) {
        String responseChannel = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, mBayeuxClient.getId(), mCorrelationId++);
        mPendingRequests.put(responseChannel, callback);
        sendCometMessage(null, CHANNEL_SLIM_REQUEST, responseChannel, cmd);

        return responseChannel;
    }

    private String sendCometMessage(final Player player, SlimCommand slimCommand, String... cmd) {
        String channels[] = slimCommand.getChannels(player, cmd);
        sendCometMessage(player, channels[0], channels[1], cmd);
        ClientSessionChannel.MessageListener listener = slimCommand.getListener(cmd);
        if (listener != null) mPendingRequests.put(channels[1], listener);
        return channels[1];
    }

    private void sendCometMessage(final Player player, String channel, final String responseChannel, String... cmd) {
        List<Object> request = new ArrayList<>();
        request.add(player == null ? "" : player.getId());
        request.add(cmd);

        Map<String, Object> data = new HashMap<>();
        data.put("request", request);
        data.put("response", responseChannel);

        mBayeuxClient.getChannel(channel).publish(data);
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p>
     * Items are returned to the caller via the specified callback.
     * <p>
     * See {@link ItemListener#parseMessage(String, Message)} for details.
     *
     * @param player player or null
     * @param cmd Identifies the type of items
     * @param start First item to return
     * @param pageSize No of items to return
     * @param parameters Item specific parameters for the request
     * @see ItemListener#parseMessage(String, Message)
     */

    private <T extends Item> void internalRequestItems(Player player, String cmd, int start, int pageSize,
                                      List<String> parameters, final IServiceItemListCallback<T> callback) {
        final BrowseRequest<T> browseRequest = new BrowseRequest<>(player, cmd, start, pageSize, parameters, callback);
        internalRequestItems(browseRequest);
    }

    private <T extends Item> void internalRequestItems(final BrowseRequest<T> browseRequest) {
        final SlimCommand command = mCommandMap.get(browseRequest.getCmd());

        final String[] request = new String[browseRequest.getParameters().size() + 3];
        request[0] = browseRequest.getCmd();
        request[1] = String.valueOf(browseRequest.getStart());
        request[2] = String.valueOf(browseRequest.getItemsPerResponse());
        for (int i = 0; i < browseRequest.getParameters().size(); i++) {
            request[i+3] = browseRequest.getParameters().get(i);
        }

        if (Looper.getMainLooper() != Looper.myLooper()) {
            mPendingBrowseRequests.put(sendCometMessage(browseRequest.player, command, request), browseRequest);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mPendingBrowseRequests.put(sendCometMessage(browseRequest.player, command, request), browseRequest);
                }
            });
        }
    }

    @Override
    public void requestItems(String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        internalRequestItems(null, cmd, start, mPageSize, parameters, callback);
    }

    @Override
    public void requestItems(String cmd, int start, IServiceItemListCallback callback) {
        internalRequestItems(null, cmd, start, mPageSize, null, callback);
    }



    @Override
    public void requestItems(String cmd, int start, int pageSize, IServiceItemListCallback callback) {
        internalRequestItems(null, cmd, start, pageSize, null, callback);
    }

    @Override
    public void requestPlayerItems(@Nullable Player player, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        if (player == null) {
            return;
        }
        internalRequestItems(player, cmd, start, mPageSize, parameters, callback);
    }
}
