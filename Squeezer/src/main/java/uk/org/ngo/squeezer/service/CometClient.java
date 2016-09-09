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
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.common.collect.ImmutableMap;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.BuildConfig;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.ClientRequest;
import uk.org.ngo.squeezer.model.ClientRequestParameters;
import uk.org.ngo.squeezer.model.ClientResponse;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayersChanged;

public class CometClient extends BaseClient {
    private static final String TAG = CometClient.class.getSimpleName();

    /** Client to the comet server. */
    @Nullable
    private BayeuxClient mBayeuxClient;

    private static final ClientSessionChannel.MessageListener mLogJsonListener = new LogJsonListener();

    /** The channel to publish one-shot requests to. */
    private static final String CHANNEL_SLIM_REQUEST = "/slim/request";

    /** Map from a command ("players") to the listener class for responses. */
    private  final Map<String, ItemListener> mRequestMap;

    /** Map from a command ("players") to the response channel that should be used for that command. */
    private static Map<String, String> mResponseChannel = new HashMap<>();

    /** Wildcard subscription. */
     private static final String WILDCARD_SUBSCRIBTION_FORMAT = "/%s/**";

    /** The format string for the channel to listen to for responses to one-shot requests. */
    private static final String CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT = "/%s/slim/request/%s";

    /** The channel to publish subscription requests to. */
    private static final String CHANNEL_SLIM_SUBSCRIBE = "/slim/subscribe";

    /** The format string for the channel to listen to for responses to playerstatus requests. */
    private static final String CHANNEL_PLAYER_STATUS_RESPONSE_FORMAT = "/%s/slim/playerstatus/%s";

    /** Server capabilities */
    private boolean mCanMusicfolder = false;
    private boolean mCanRandomplay = false;
    private boolean mCanFavorites = false;
    private boolean mCanApps = false;

    private String mPreferredAlbumSort = "album";
    private String[] mMediaDirs;

    /** Channels */
    private ClientSessionChannel mRequestChannel;

    private Map<String, Player> mPlayers = new HashMap<>();
    private final Map<String, IServiceItemListCallback> mPendingRequests
            = new ConcurrentHashMap<>();

    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private int mCorrelationId = 0;

    CometClient(@NonNull EventBus eventBus) {
        super(eventBus);

        mRequestMap = ImmutableMap.<String, ItemListener>builder()
                .put("players", new PlayersListener())
                .put("artists", new ArtistsListener())
                .put("albums", new AlbumsListener())
                .build();
    }

    // Shims around ConnectionState methods.
    public void startConnect(final SqueezeService service, String hostPort, final String userName,
                             final String password) {
        mConnectionState.setConnectionState(mEventBus, ConnectionState.CONNECTION_STARTED);

        HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
        } catch (Exception e) {
            // XXX: Handle this properly. Maybe startConnect() should throw exceptions if the
            // connection fails?
            e.printStackTrace();
            mConnectionState.setConnectionState(mEventBus, ConnectionState.CONNECTION_FAILED);
            return;
        }

        // XXX: Need to split apart hostPort, and provide a config mechanism that can
        // distinguish between CLI and Comet.
        // XXX: Also need to deal with usernames and passwords, and HTTPS
        //String url = String.format("http://%s/cometd", hostPort);
        String url = "http://192.168.0.13:9001/cometd";

        Map<String, Object> options = new HashMap<>();
        ClientTransport transport = new LongPollingTransport(options, httpClient);
        mBayeuxClient = new BayeuxClient(url, transport) {
            @Override
            public void onSending(List<? extends Message> messages) {
                super.onSending(messages);
                if (BuildConfig.DEBUG) {
                    for (Message message : messages) {
                        if ("/meta/connect".equals(message.getChannel())) {
                            return;
                        }
                        Log.d(TAG, "SEND: " + message.getJSON());
                    }
                }
            }
        };

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mBayeuxClient.handshake(mLogJsonListener);
                if (!mBayeuxClient.waitFor(10000, BayeuxClient.State.CONNECTED)) {
                    mConnectionState.setConnectionState(mEventBus, ConnectionState.CONNECTION_FAILED);
                    return;  // XXX: Check if returning here is the right thing to do? Any other cleanup?
                }

                // Connected from this point on.
                mConnectionState.setConnectionState(mEventBus, ConnectionState.CONNECTION_COMPLETED);
                mConnectionState.setConnectionState(mEventBus, ConnectionState.LOGIN_STARTED);
                mConnectionState.setConnectionState(mEventBus, ConnectionState.LOGIN_COMPLETED);

                mResponseChannel.clear();

                // Have the client listen for replies on all channels we care about, and make sure
                // the listeners are hooked up to each channel.
                //
                // Note: If this was a table, it would be
                // R = "players" (the command)
                // C = "/%s/slim/request/players" (the response channel, with clientId to be filled in)
                // V = new PlayerListener (the listener)
                String clientId = mBayeuxClient.getId();
                for (String cmd : mRequestMap.keySet()) {
                    String responseChannel = String.format(
                            CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, cmd
                    );

                    mResponseChannel.put(cmd, responseChannel);
                    mRequestMap.get(cmd);
                    mBayeuxClient.getChannel(responseChannel).addListener(mRequestMap.get(cmd));
                    mBayeuxClient.getChannel(responseChannel).addListener(mLogJsonListener);
                }

                mBayeuxClient.getChannel(String.format(WILDCARD_SUBSCRIBTION_FORMAT, clientId)).subscribe(mLogJsonListener);

                //mConnectionState.startConnect(service, mEventBus, mExecutor, this, hostPort, userName, password);

                // There's no persistent connection to manage.  Run the connection state machine
                // so that the UI gets in to a consistent state
                fetchPlayers();

                // XXX: "listen 1" -- serverstatus?

                // Learn server capabilites. The responses to "can" requests do not include
                // the feature that was being request, so each one needs its own response channel.
                String chnCanMusicfolder = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "can_musicfolder");
                String chnCanRandomplay = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "can_randomplay");
                String chnCanFavorites = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "can_favorites");
                String chnCanMyapps = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "can_myapps");

                String chnPrefAlbumSort = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "pref_jivealbumsort");
                String chnPrefMediadirs = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "pref_mediadirs");

                String chnVersion = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "version");

                // The responses to pref requests do not include the preference being returned either.

                mBayeuxClient.getChannel(chnCanMusicfolder).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        mCanMusicfolder = "1".equals(message.getDataAsMap().get("_can"));
                    }
                });

                mBayeuxClient.getChannel(chnCanRandomplay).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        mCanRandomplay = "1".equals(message.getDataAsMap().get("_can"));
                    }
                });

                mBayeuxClient.getChannel(chnCanFavorites).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        mCanFavorites = "1".equals(message.getDataAsMap().get("_can"));
                    }
                });

                mBayeuxClient.getChannel(chnCanMyapps).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        mCanApps = "1".equals(message.getDataAsMap().get("_can"));
                    }
                });

                mBayeuxClient.getChannel(chnPrefMediadirs).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        //mMediaDirs = (String[]) message.getDataAsMap().get("_p2");
                    }
                });

                mBayeuxClient.getChannel(chnPrefAlbumSort).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        if (message.isSuccessful()) {
                            mPreferredAlbumSort = (String) message.getDataAsMap().get("_p2");
                        }
                    }
                });

                mBayeuxClient.getChannel(chnVersion).addListener(new LogJsonListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        super.onMessage(channel, message);
                        mEventBus.postSticky(new HandshakeComplete(
                                mCanFavorites, mCanMusicfolder,
                                mCanApps, mCanRandomplay,
                                "7.6"));
                    }
                });

                mRequestChannel = mBayeuxClient.getChannel(CHANNEL_SLIM_REQUEST);

                mRequestChannel.publish(new Request("can", "musicfolder", "?").getData(chnCanMusicfolder), mLogJsonListener);
                mRequestChannel.publish(new Request("can", "randomplay", "?").getData(chnCanRandomplay), mLogJsonListener);
                mRequestChannel.publish(new Request("can", "favorites", "items", "?").getData(chnCanFavorites), mLogJsonListener);
                mRequestChannel.publish(new Request("can", "myapps", "items", "?").getData(chnCanMyapps), mLogJsonListener);

                // XXX: Skipped "pref httpport ?"

                mRequestChannel.publish(new Request("pref", "jivealbumsort", "?").getData(chnPrefAlbumSort), mLogJsonListener);
                mRequestChannel.publish(new Request("pref", "mediadirs", "?").getData(chnPrefMediadirs), mLogJsonListener);

                mRequestChannel.publish(new Request("version", "?").getData(chnVersion), mLogJsonListener);

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

    abstract class ItemListener implements ClientSessionChannel.MessageListener {}

    private static class LogJsonListener implements ClientSessionChannel.MessageListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            Log.v(TAG, String.format("RECV [%s] (%s): %s",
                    message.isSuccessful() ? "S" : "F", message.getChannel(), message.getJSON()));
//            Map<String, Object> data = message.getDataAsMap();
//            Log.v(TAG, "map: " + data);
//            Log.v(TAG, "id: " + message.getId());
//
//            Log.v(TAG, "values: " + data.get("players_loop"));
//            Log.v(TAG, "advice: " + message.getAdvice());
//
//            Object[] players_loop;
//            players_loop = (Object[]) data.get("players_loop");
//            Log.v(TAG, "players_loop: " + players_loop[0]);
//            HashMap h = (HashMap<String, String>) players_loop[0];
//            Log.v(TAG, "name: " + h.get("name"));
        }
    }

    private class PlayersListener extends ItemListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            // XXX: Sanity check that the message contains an ID and players_loop

            // Note: This is probably wrong, need to figure out result paging.
            IServiceItemListCallback callback = mPendingRequests.get(message.getId());
            if (callback == null) {
                return;
            }

            Map<String, Object> data = message.getDataAsMap();
            Object[] player_data = (Object[]) data.get("players_loop");

            List<Player> players = new ArrayList<>(player_data.length);

            for (Object player_d : player_data) {
                players.add(new Player((Map<String, String>) player_d));
            }

            callback.onItemsReceived(player_data.length, 0, null, players, Player.class);
        }
    }

    private class ArtistsListener extends ItemListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            // Note: This is probably wrong, need to figure out result paging.
            IServiceItemListCallback callback = mPendingRequests.get(message.getId());
            if (callback == null) {
                return;
            }

            Map<String, Object> data = message.getDataAsMap();
            Object[] item_data = (Object[]) data.get("artists_loop");

            List<Artist> items = new ArrayList<>(item_data.length);

            for (Object item_d : item_data) {
                Map<String, String> record = (Map<String, String>) item_d;
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !(value instanceof String)) {
                        record.put(entry.getKey(), value.toString());
                    }
                }
                items.add(new Artist(record));
            }

            callback.onItemsReceived(item_data.length, 0, null, items, Player.class);
        }
    }

    private class AlbumsListener extends ItemListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            // Note: This is probably wrong, need to figure out result paging.
            IServiceItemListCallback callback = mPendingRequests.get(message.getId());
            if (callback == null) {
                return;
            }

            Map<String, Object> data = message.getDataAsMap();
            Object[] item_data = (Object[]) data.get("albums_loop");

            List<Album> items = new ArrayList<>(item_data.length);

            for (Object item_d : item_data) {
                Map<String, String> record = (Map<String, String>) item_d;
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !(value instanceof String)) {
                        record.put(entry.getKey(), value.toString());
                    }
                }
                items.add(new Album(record));
            }

            callback.onItemsReceived(item_data.length, 0, null, items, Player.class);
        }
    }

    @Override
    public void disconnect(boolean loginFailed) {
        mBayeuxClient.disconnect();
        mConnectionState.disconnect(mEventBus, loginFailed);
        mPlayers.clear();
    }

    @Override
    public void sendCommandImmediately(String... commands) {

    }

    @Override
    public void sendCommand(String... commands) {

    }

    @Override
    public void sendPlayerCommand(Player player, String command) {

    }

    @Override
    public String[] getMediaDirs() {
        return new String[0];
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnectInProgress() {
        return false;
    }


    /**
     * Queries for all players known by the server.
     * </p>
     * Posts a PlayersChanged message if the list of players has changed.
     */
    // XXX Copied from CliClient.
    private void fetchPlayers() {
        requestItems("players", -1, new IServiceItemListCallback<Player>() {
            private final HashMap<String, Player> players = new HashMap<String, Player>();

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
        return "album";
    }

    @Override
    public void cancelClientRequests(Object object) {

    }

    // Should probably have a pool of these.
    private class Request {
        String[] cmd;

        Request(String... cmd) {
            this.cmd = cmd;
        }

//        Request(String player, String cmd, List<String> parameters) {
//            if (player != null) {
//                this.player = player;
//            }
//            this.cmd = new String[parameters.size() + 1];
//            this.cmd[0] = cmd;
//            for (int i = 1; i <= parameters.size(); i++) {
//                this.cmd[i] = parameters.get(i - 1);
//            }
//        }

        Map<String, Object> getData(String chnResponse) {
            Map<String, Object> data = new HashMap<>();

            List<Object> request = new ArrayList<>();
            request.add("");
            request.add(cmd);

            data.put("request", request);
            data.put("response", chnResponse);
            return data;
        }
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p>
     * Items are returned to the caller via the specified callback.
     * <p>
     * See {@link #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)} for details.
     *
     * @param playerId Id of the current player or null
     * @param cmd Identifies the type of items
     * @param start First item to return
     * @param pageSize No of items to return
     * @param parameters Item specific parameters for the request
     * @see #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)
     */
    private void internalRequestItems(String playerId, String cmd, int start, int pageSize,
                                     List<String> parameters, final IServiceItemListCallback callback) {

        final String responseChannel = mResponseChannel.get(cmd);
        if (responseChannel == null) {
            Log.e(TAG, "No response channel for request " + cmd);
            return;
        }


        // XXX: Check for NPE (and or save the channel earlier)
        if (playerId != null) {
            Log.e(TAG, "Haven't written code for players yet");
            return;
        }

        final String[] request = new String[parameters.size() + 1];
        request[0] = cmd;
        for (int i = 1; i <= parameters.size(); i++) {
            request[i] = parameters.get(i - 1);
        }

        if (Looper.getMainLooper() != Looper.myLooper()) {
            requestItemsImmediately(request, responseChannel, callback);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    requestItemsImmediately(request, responseChannel, callback);
                }
            });
        }

    }

    private void requestItemsImmediately(String[] request, String responseChannel, final IServiceItemListCallback callback) {
        mBayeuxClient.getChannel(CHANNEL_SLIM_REQUEST).publish(
                new Request(request).getData(responseChannel), new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message) {
                        if (message.isSuccessful()) {
                            mPendingRequests.put(message.getId(), callback);
                        } else {
                            Log.e(TAG, "Message failed: " + message.getJSON());
                        }

                    }
                }
        );
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p>
     * Items are requested in chunks of <code>R.integer.PageSize</code>, and returned
     * to the caller via the specified callback.
     * <p>
     * If start is zero, this will order one item, to quickly learn the number of items
     * from the server. When the server response with this item it is transferred to the
     * caller. The remaining items in the first page are then ordered, and transferred
     * to the caller when they arrive.
     * <p>
     * If start is < 0, it means the caller wants the entire list. They are ordered in
     * pages, and transferred to the caller as they arrive.
     * <p>
     * Otherwise request a page of items starting from start.
     * <p>
     * See {@link #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)} for details.
     *
     * @param playerId Id of the current player or null
     * @param cmd Identifies the type of items
     * @param start First item to return
     * @param parameters Item specific parameters for the request
     * @see #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)
     */
    private void internalRequestItems(String playerId, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        boolean full_list = (start < 0);

        if (full_list) {
            if (parameters == null)
                parameters = new ArrayList<String>();
            parameters.add("full_list:1");
        }

        internalRequestItems(playerId, cmd, (full_list ? 0 : start), (start == 0 ? 1 : mPageSize), parameters, callback);
    }

    @Override
    public void requestItems(String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        internalRequestItems(null, cmd, start, parameters, callback);
    }

    @Override
    public void requestItems(String cmd, int start, IServiceItemListCallback callback) {
        requestItems(cmd, start, null, callback);
    }



    @Override
    public void requestItems(String cmd, int start, int pageSize, IServiceItemListCallback callback) {

    }

    @Override
    public void requestPlayerItems(@Nullable Player player, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {

    }
}
