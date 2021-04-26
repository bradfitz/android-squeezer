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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.HttpClientTransport;
import org.cometd.client.transport.TransportListener;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.B64Code;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
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
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.AlertWindow;
import uk.org.ngo.squeezer.model.DisplayMessage;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.CurrentPlaylistItem;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.SlimCommand;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.event.AlertEvent;
import uk.org.ngo.squeezer.service.event.DisplayEvent;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.model.MenuStatusMessage;
import uk.org.ngo.squeezer.service.event.PlayerPrefReceived;
import uk.org.ngo.squeezer.service.event.PlayerVolume;
import uk.org.ngo.squeezer.service.event.RegisterSqueezeNetwork;
import uk.org.ngo.squeezer.util.Reflection;

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

    /** The channel to publish unsubscribe requests to. */
    private static final String CHANNEL_SLIM_UNSUBSCRIBE = "/slim/unsubscribe";

    /** The format string for the channel to listen to for server status events. */
    private static final String CHANNEL_SERVER_STATUS_FORMAT = "/%s/slim/serverstatus";

    /** The format string for the channel to listen to for player status events. */
    private static final String CHANNEL_PLAYER_STATUS_FORMAT = "/%s/slim/playerstatus/%s";

    /** The format string for the channel to listen to for display status events. */
    private static final String CHANNEL_DISPLAY_STATUS_FORMAT = "/%s/slim/displaystatus/%s";

    /** The format string for the channel to listen to for menu status events. */
    private static final String CHANNEL_MENU_STATUS_FORMAT = "/%s/slim/menustatus/%s";

    // Maximum time for wait replies for server capabilities
    private static final long HANDSHAKE_TIMEOUT = 4000;


    /** Handler for off-main-thread work. */
    @NonNull
    private final Handler mBackgroundHandler;

    /** Map from an item request command ("players") to the listener class for responses. */
    private final Map<Class<?>, ItemListener<?>> mItemRequestMap;

    /** Map from a request to the listener class for responses. */
    private  final Map<String, ResponseHandler> mRequestMap;

    /** Client to the comet server. */
    @Nullable
    private BayeuxClient mBayeuxClient;

    private final Map<String, Request> mPendingRequests
            = new ConcurrentHashMap<>();

    private final Map<String, BrowseRequest<?>> mPendingBrowseRequests
            = new ConcurrentHashMap<>();

    private final Queue<PublishMessage> mCommandQueue = new LinkedList<>();
    private boolean mCurrentCommand;

    private final PublishListener mPublishListener = new PublishListener();

    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private volatile int mCorrelationId = 0;

    CometClient(@NonNull EventBus eventBus) {
        super(eventBus);

        HandlerThread handlerThread = new HandlerThread(SqueezeService.class.getSimpleName());
        handlerThread.start();
        mBackgroundHandler = new CliHandler(handlerThread.getLooper());

        List<ItemListener<?>> itemListeners = Arrays.asList(
                new AlarmsListener(),
                new AlarmPlaylistsListener(),
                new SongListener(),
                new MusicFolderListener(),
                new JiveItemListener()
        );
        ImmutableMap.Builder<Class<?>, ItemListener<?>> builder = ImmutableMap.builder();
        for (ItemListener<?> itemListener : itemListeners) {
            builder.put(itemListener.getDataType(), itemListener);
        }
        mItemRequestMap = builder.build();

        mRequestMap = ImmutableMap.<String, ResponseHandler>builder()
                .put("playerpref", (player, request, message) -> {
                    //noinspection WrongConstant
                    String p2 = (String) message.getDataAsMap().get("_p2");
                    mEventBus.post(new PlayerPrefReceived(player, request.cmd.get(1), p2 != null ? p2 : request.cmd.get(2)));
                })
                .put("mixer", (player, request, message) -> {
                    if (request.cmd.get(1).equals("volume")) {
                        String volume = (String) message.getDataAsMap().get("_volume");
                        if (volume != null) {
                            int newVolume = Integer.valueOf(volume);
                            player.getPlayerState().setCurrentVolume(newVolume);
                            mEventBus.post(new PlayerVolume(newVolume, player));
                        } else {
                            command(player, new String[]{"mixer", "volume", "?"}, Collections.emptyMap());
                        }
                    }
                })
                .build();
    }

    // Shims around ConnectionState methods.
    @Override
    public void startConnect(final SqueezeService service) {
        Log.i(TAG, "startConnect()");
        // Start the background connect
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                final Preferences preferences = new Preferences(service);
                preferences.setManualDisconnect(false);
                final Preferences.ServerAddress serverAddress = preferences.getServerAddress();
                final String username = preferences.getUsername(serverAddress);
                final String password = preferences.getPassword(serverAddress);
                Log.i(TAG, "Connecting to: " + username + "@" + serverAddress.address());

                if (!mEventBus.isRegistered(CometClient.this)) {
                    mEventBus.register(CometClient.this);
                }
                mConnectionState.setConnectionState(ConnectionState.CONNECTION_STARTED);
                final boolean isSqueezeNetwork = serverAddress.squeezeNetwork;

                final HttpClient httpClient = new HttpClient();
                httpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, "Squeezer-squeezer/" + SqueezerBayeuxExtension.getRevision()));
                try {
                    httpClient.start();
                } catch (Exception e) {
                    mConnectionState.setConnectionError(ConnectionError.START_CLIENT_ERROR);
                    return;
                }

                CometClient.this.username.set(username);
                CometClient.this.password.set(password);

                mUrlPrefix = "http://" + serverAddress.address();
                final String url = mUrlPrefix + "/cometd";
                try {
                    // Neither URLUtil.isValidUrl nor Patterns.WEB_URL works as expected
                    // Not even create of URL and URI throws reliably so we add some extra checks
                    URI uri = new URL(url).toURI();
                    if (!(
                            TextUtils.equals(uri.getHost(), serverAddress.host())
                                    && uri.getPort() == serverAddress.port()
                                    && TextUtils.equals(uri.getPath(), "/cometd")
                    )) {
                        throw new IllegalArgumentException("Invalid url: " + url);
                    }
                } catch (Exception e) {
                    mConnectionState.setConnectionError(ConnectionError.INVALID_URL);
                    return;
                }

                // Set the VM-wide authentication handler (needed by image fetcher and other using
                // the standard java http API)
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                });

                Map<String, Object> options = new HashMap<>();
                options.put(HttpClientTransport.MAX_NETWORK_DELAY_OPTION, LONG_POLLING_TIMEOUT);
                ClientTransport clientTransport;
                if (!isSqueezeNetwork) {
                    clientTransport = new HttpStreamingTransport(url, options, httpClient) {
                        @Override
                        protected void customize(org.eclipse.jetty.client.api.Request request) {
                            if (username != null && password != null) {
                                String authorization = B64Code.encode(username + ":" + password);
                                request.header(HttpHeader.AUTHORIZATION, "Basic " + authorization);
                            }
                        }
                    };
                } else {
                    clientTransport = new HttpStreamingTransport(url, options, httpClient) {
                        // SN only replies the first connect message
                        private boolean hasSendConnect;

                        @Override
                        public void send(TransportListener listener, List<Message.Mutable> messages) {
                            boolean isConnect = Channel.META_CONNECT.equals(messages.get(0).getChannel());
                            if (!(isConnect && hasSendConnect)) {
                                super.send(listener, messages);
                                if (isConnect) {
                                    hasSendConnect = true;
                                }
                            }
                        }
                    };
                }
                mBayeuxClient = new SqueezerBayeuxClient(url, clientTransport);
                mBayeuxClient.addExtension(new SqueezerBayeuxExtension());
                mBayeuxClient.getChannel(Channel.META_HANDSHAKE).addListener((ClientSessionChannel.MessageListener) (channel, message) -> {
                    if (message.isSuccessful()) {
                        onConnected(isSqueezeNetwork);
                    } else {
                        Log.w(TAG, channel + ": " + message.getJSON());

                        // The bayeux protocol handle failures internally.
                        // This current client libraries are however incompatible with LMS as new messages to the
                        // meta channels are ignored.
                        // So we disconnect here so we can create a new connection.
                        Map<String, Object> failure = Util.getRecord(message, "failure");
                        Message failedMessage = (failure != null) ? (Message) failure.get("message") : null;
                        if ("forced reconnect".equals(failedMessage.get("error"))) {
                            disconnect(ConnectionState.RECONNECT);
                        } else {
                            Object httpCodeValue = (failure != null) ? failure.get("httpCode") : null;
                            int httpCode = (httpCodeValue instanceof Integer) ? (int) httpCodeValue : -1;

                            disconnect((httpCode == 401) ? ConnectionError.LOGIN_FALIED : ConnectionError.CONNECTION_ERROR);
                        }
                    }
                });
                mBayeuxClient.getChannel(Channel.META_CONNECT).addListener((ClientSessionChannel.MessageListener) (channel, message) -> {
                    if (!message.isSuccessful() && (getAdviceAction(message.getAdvice()) == null)) {
                        // Advices are handled internally by the bayeux protocol, so skip these here
                        Log.w(TAG, channel + ": " + message.getJSON());
                        disconnect();
                    }
                });

                mBayeuxClient.handshake();
            }

            private void onConnected(boolean isSqueezeNetwork) {
                Log.i(TAG, "Connected, start learning server capabilities");
                mCurrentCommand = false;
                mConnectionState.setConnectionState(ConnectionState.CONNECTION_COMPLETED);

                String clientId = mBayeuxClient.getId();

                mBayeuxClient.getChannel(String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "*")).subscribe((channel, message) -> {
                    Request request = mPendingRequests.get(message.getChannel());
                    if (request != null) {
                        request.callback.onResponse(request.player, request, message);
                        mPendingRequests.remove(message.getChannel());
                    }
                });

                mBayeuxClient.getChannel(String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId)).subscribe(CometClient.this::parseServerStatus);

                mBayeuxClient.getChannel(String.format(CHANNEL_PLAYER_STATUS_FORMAT, clientId, "*")).subscribe(CometClient.this::parsePlayerStatus);

                mBayeuxClient.getChannel(String.format(CHANNEL_DISPLAY_STATUS_FORMAT, clientId, "*")).subscribe(CometClient.this::parseDisplayStatus);

                mBayeuxClient.getChannel(String.format(CHANNEL_MENU_STATUS_FORMAT, clientId, "*")).subscribe(CometClient.this::parseMenuStatus);

                // Request server status
                publishMessage(serverStatusRequest(), CHANNEL_SLIM_REQUEST, String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId), null);

                // Subscribe to server changes
                {
                    Request request = serverStatusRequest().param("subscribe", "60");
                    publishMessage(request, CHANNEL_SLIM_SUBSCRIBE, String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId), null);
                }

                // Set a timeout for the handshake
                mBackgroundHandler.removeMessages(MSG_HANDSHAKE_TIMEOUT);
                mBackgroundHandler.sendEmptyMessageDelayed(MSG_HANDSHAKE_TIMEOUT, HANDSHAKE_TIMEOUT);

                if (isSqueezeNetwork) {
                    if (needRegister()) {
                        mEventBus.post(new RegisterSqueezeNetwork());
                    }
                }
            }
        });
    }

    private boolean needRegister() {
        return mBayeuxClient.getId().startsWith("1X");
    }


    private void parseServerStatus(ClientSessionChannel channel, Message message) {
        Map<String, Object> data = message.getDataAsMap();

        // We can't distinguish between no connected players and players not received
        // so we check the server version which is also set from server status
        boolean firstTimePlayersReceived = (getConnectionState().getServerVersion() == null);

        getConnectionState().setMediaDirs(Util.getStringArray(data, Player.Pref.MEDIA_DIRS));
        getConnectionState().setServerVersion((String) data.get("version"));
        Object[] item_data = (Object[]) data.get("players_loop");
        final HashMap<String, Player> players = new HashMap<>();
        if (item_data != null) {
            for (Object item_d : item_data) {
                Map<String, Object> record = (Map<String, Object>) item_d;
                if (!record.containsKey(Player.Pref.DEFEAT_DESTRUCTIVE_TTP) &&
                        data.containsKey(Player.Pref.DEFEAT_DESTRUCTIVE_TTP)) {
                    record.put(Player.Pref.DEFEAT_DESTRUCTIVE_TTP, data.get(Player.Pref.DEFEAT_DESTRUCTIVE_TTP));
                }
                Player player = new Player(record);
                players.put(player.getId(), player);
            }
        }

        Map<String, Player> currentPlayers = mConnectionState.getPlayers();
        if (firstTimePlayersReceived || !players.equals(currentPlayers)) {
            mConnectionState.setPlayers(players);
        } else {
            for (Player player : players.values()) {
                PlayerState currentPlayerState = currentPlayers.get(player.getId()).getPlayerState();
                if (!player.getPlayerState().prefs.equals(currentPlayerState.prefs)) {
                    currentPlayerState.prefs = player.getPlayerState().prefs;
                    postPlayerStateChanged(player);
                }
            }
        }
    }

    private void parsePlayerStatus(ClientSessionChannel channel, Message message) {
        String[] channelParts = mSlashSplitPattern.split(message.getChannel());
        String playerId = channelParts[channelParts.length - 1];
        Player player = mConnectionState.getPlayer(playerId);

        // XXX: Can we ever see a status for a player we don't know about?
        // XXX: Maybe the better thing to do is to add it.
        if (player == null)
            return;

        Map<String, Object> messageData = message.getDataAsMap();

        CurrentPlaylistItem currentSong = null;
        Object[] item_data = (Object[]) messageData.get("item_loop");
        if (item_data != null && item_data.length > 0) {
            Map<String, Object> record = (Map<String, Object>) item_data[0];

            patchUrlPrefix(record);
            record.put("base", messageData.get("base"));
            currentSong = new CurrentPlaylistItem(record);
            record.remove("base");
        }
        parseStatus(player, currentSong, messageData);
    }

    @Override
    protected void postSongTimeChanged(Player player) {
        super.postSongTimeChanged(player);
        if (player.getPlayerState().isPlaying()) {
            mBackgroundHandler.removeMessages(MSG_TIME_UPDATE);
            mBackgroundHandler.sendEmptyMessageDelayed(MSG_TIME_UPDATE, 1000);
        }
    }

    @Override
    protected void postPlayerStateChanged(Player player) {
        super.postPlayerStateChanged(player);
        if (player.getPlayerState().getSleepDuration() > 0) {
            android.os.Message message = mBackgroundHandler.obtainMessage(MSG_STATE_UPDATE, player);
            mBackgroundHandler.removeMessages(MSG_STATE_UPDATE);
            mBackgroundHandler.sendMessageDelayed(message, 1000);
        }
    }

    private void parseDisplayStatus(ClientSessionChannel channel, Message message) {
        Map<String, Object> display = Util.getRecord(message.getDataAsMap(), "display");
        if (display != null) {
            String type = Util.getString(display, "type");
            if ("alertWindow".equals(type)) {
                AlertWindow alertWindow = new AlertWindow(display);
                mEventBus.post(new AlertEvent(alertWindow));
            } else {
                display.put("urlPrefix", mUrlPrefix);
                DisplayMessage displayMessage = new DisplayMessage(display);
                mEventBus.post(new DisplayEvent(displayMessage));
            }
        }
    }

    private void parseMenuStatus(ClientSessionChannel channel, Message message) {
        Object[] data = (Object[]) message.getData();

        // each chunk.data[2] contains a table that needs insertion into the menu
        Object[] item_data = (Object[]) data[1];
        JiveItem[] menuItems = new JiveItem[item_data.length];
        for (int i = 0; i < item_data.length; i++) {
            Map<String, Object> record = (Map<String, Object>) item_data[i];
            patchUrlPrefix(record);
            menuItems[i] = new JiveItem(record);
        }

        // directive for these items is in chunk.data[3]
        String menuDirective = (String) data[2];

        // the player ID this notification is for is in chunk.data[4]
        String playerId = (String) data[3];

        mConnectionState.menuStatusEvent(new MenuStatusMessage(playerId, menuDirective, menuItems));
    }

    /**
     * Add endpoint to fetch further info from a slimserver item
     */
    private void patchUrlPrefix(Map<String, Object> record) {
        record.put("urlPrefix", mUrlPrefix);
        Map<String, Object> window = (Map<String, Object>) record.get("window");
        if (window != null) {
            window.put("urlPrefix", mUrlPrefix);
        }
    }

    private interface ResponseHandler {
        void onResponse(Player player, Request request, Message message);
    }

    private class PublishListener implements ClientSessionChannel.MessageListener {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (!message.isSuccessful()) {
                // TODO remote logging and possible other handling
                Log.e(TAG, channel + ": " + message.getJSON());
            }
            mBackgroundHandler.sendEmptyMessage(MSG_PUBLISH_RESPONSE_RECIEVED);
        }
    }

    private abstract class ItemListener<T> extends BaseListHandler<T> implements ResponseHandler {
        void parseMessage(String countName, String itemLoopName, Message message) {
            @SuppressWarnings("unchecked")
            BrowseRequest<T> browseRequest = (BrowseRequest<T>) mPendingBrowseRequests.get(message.getChannel());
            if (browseRequest == null) {
                return;
            }

            mPendingBrowseRequests.remove(message.getChannel());
            clear();
            Map<String, Object> data = message.getDataAsMap();
            int count = Util.getInt(data.get(countName));
            Map<String, Object> baseRecord = (Map<String, Object>) data.get("base");
            if (baseRecord != null) {
                patchUrlPrefix(baseRecord);
            }
            Object[] item_data = (Object[]) data.get(itemLoopName);
            if (item_data != null) {
                for (Object item_d : item_data) {
                    Map<String, Object> record = (Map<String, Object>) item_d;
                    patchUrlPrefix(record);
                    if (baseRecord != null) record.put("base", baseRecord);
                    add(record);
                    record.remove("base");
                }
            }

            // Process the lists for all the registered handlers
            final boolean fullList = browseRequest.isFullList();
            final int start = browseRequest.getStart();
            final int end = start + getItems().size();
            int max = 0;
            patchUrlPrefix(data);
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

    private class SongListener extends ItemListener<Song> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            switch (request.getRequest()) {
                default:
                    parseMessage("titles_loop", message);
                    break;
                case "playlists tracks":
                    parseMessage("playlisttracks_loop", message);
                    break;
                case "status":
                    parseMessage("playlist_tracks", "playlist_loop", message);
                    break;
            }
            parseMessage("titles_loop", message);
        }
    }

    private class MusicFolderListener extends ItemListener<MusicFolderItem> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("folder_loop", message);
        }
    }

    private class JiveItemListener extends ItemListener<JiveItem> {
        @Override
        public void onResponse(Player player, Request request, Message message) {
            parseMessage("item_loop", message);
        }
    }

    public void onEvent(@SuppressWarnings("unused") HandshakeComplete event) {
        mBackgroundHandler.removeMessages(MSG_HANDSHAKE_TIMEOUT);
    }

    @Override
    public void disconnect() {
        disconnect(ConnectionState.DISCONNECTED);
    }

    private void disconnect(@ConnectionState.ConnectionStates int connectionState) {
        if (mBayeuxClient != null) mBackgroundHandler.sendEmptyMessage(MSG_DISCONNECT);
        mConnectionState.setConnectionState(connectionState);
    }

    private void disconnect(ConnectionError connectionError) {
        if (mBayeuxClient != null) mBackgroundHandler.sendEmptyMessage(MSG_DISCONNECT);
        mConnectionState.setConnectionError(connectionError);
    }

    @Override
    public void cancelClientRequests(Object client) {
        for (Map.Entry<String, BrowseRequest<?>> entry : mPendingBrowseRequests.entrySet()) {
            if (entry.getValue().getCallback().getClient() == client) {
                mPendingBrowseRequests.remove(entry.getKey());
            }
        }
    }

    private void exec(ResponseHandler callback, String... cmd) {
        exec(request(callback, cmd));
    }

    private String exec(Request request) {
        String responseChannel = String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, mBayeuxClient.getId(), mCorrelationId++);
        if (request.callback != null) mPendingRequests.put(responseChannel, request);
        publishMessage(request, CHANNEL_SLIM_REQUEST, responseChannel, null);
        return responseChannel;
    }

    /** If request is null, this is an unsubscribe to the suplied response channel */
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
            Map<String, Object> data = new HashMap<>();
            if (request != null) {
                data.put("request", request.slimRequest());
                data.put("response", responseChannel);
            } else {
                data.put("unsubscribe", responseChannel);
            }
            mBayeuxClient.getChannel(channel).publish(data, publishListener != null ? publishListener : this.mPublishListener);
        } else
            mCommandQueue.add(new PublishMessage(request, channel, responseChannel, publishListener));
    }

    @Override
    protected  <T> void internalRequestItems(final BrowseRequest<T> browseRequest) {
        Class<?> callbackClass = Reflection.getGenericClass(browseRequest.getCallback().getClass(), IServiceItemListCallback.class, 0);
        ItemListener listener = mItemRequestMap.get(callbackClass);
        if (listener == null) {
            throw new RuntimeException("No handler defined for '" + browseRequest.getCallback().getClass() + "'");
        }

        Request request = request(browseRequest.getPlayer(), listener, browseRequest.cmd())
                .page(browseRequest.getStart(), browseRequest.getItemsPerResponse())
                .params(browseRequest.params);
        mPendingBrowseRequests.put(exec(request), browseRequest);
    }

    @Override
    public void command(Player player, String[] cmd, Map<String, Object> params) {
        ResponseHandler callback = mRequestMap.get(cmd[0]);
        exec(request(player, callback, cmd).params(params));
    }

    @Override
    public void requestPlayerStatus(Player player) {
        Request request = statusRequest(player);
        publishMessage(request, CHANNEL_SLIM_REQUEST, subscribeResponseChannel(player, CHANNEL_PLAYER_STATUS_FORMAT), null);
    }

    @Override
    public void subscribePlayerStatus(final Player player, final PlayerState.PlayerSubscriptionType subscriptionType) {
        Request request = statusRequest(player)
                .param("subscribe", subscriptionType.getStatus());
        publishMessage(request, CHANNEL_SLIM_SUBSCRIBE, subscribeResponseChannel(player, CHANNEL_PLAYER_STATUS_FORMAT), new PublishListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                super.onMessage(channel, message);
                if (message.isSuccessful()) {
                    player.getPlayerState().setSubscriptionType(subscriptionType);
                }
            }
        });
    }

    @Override
    public void subscribeDisplayStatus(Player player, boolean subscribe) {
        Request request = request(player, "displaystatus").param("subscribe", subscribe ? "showbriefly" : "");
        publishMessage(request, CHANNEL_SLIM_SUBSCRIBE, subscribeResponseChannel(player, CHANNEL_DISPLAY_STATUS_FORMAT), mPublishListener);
    }

    @Override
    public void subscribeMenuStatus(Player player, boolean subscribe) {
        if (subscribe)
            subscribeMenuStatus(player);
        else
            unsubscribeMenuStatus(player);
    }

    private void subscribeMenuStatus(Player player) {
        Request request = request(player, "menustatus");
        publishMessage(request, CHANNEL_SLIM_SUBSCRIBE, subscribeResponseChannel(player, CHANNEL_MENU_STATUS_FORMAT), null);
    }

    private void unsubscribeMenuStatus(Player player) {
        publishMessage(null, CHANNEL_SLIM_UNSUBSCRIBE, subscribeResponseChannel(player, CHANNEL_MENU_STATUS_FORMAT), null);
    }

    private String subscribeResponseChannel(Player player, String format) {
        return String.format(format, mBayeuxClient.getId(), player.getId());
    }

    private static String getAdviceAction(Map<String, Object> advice) {
        String action = null;
        if (advice != null && advice.containsKey(Message.RECONNECT_FIELD))
            action = (String)advice.get(Message.RECONNECT_FIELD);
        return action;
    }

    private static final int MSG_PUBLISH = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_HANDSHAKE_TIMEOUT = 3;
    private static final int MSG_PUBLISH_RESPONSE_RECIEVED = 4;
    private static final int MSG_TIME_UPDATE = 5;
    private static final int MSG_STATE_UPDATE = 6;
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
                    Log.w(TAG, "LMS handshake timeout: " + mConnectionState);
                    disconnect();
                    break;
                case MSG_PUBLISH_RESPONSE_RECIEVED: {
                    mCurrentCommand = false;
                    PublishMessage message = mCommandQueue.poll();
                    if (message != null)
                        _publishMessage(message.request, message.channel, message.responseChannel, message.publishListener);
                    break;
                }
                case MSG_TIME_UPDATE: {
                    Player activePlayer = mConnectionState.getActivePlayer();
                    if (activePlayer != null) {
                        postSongTimeChanged(activePlayer);
                    }
                    break;
                }
                case MSG_STATE_UPDATE: {
                    Player player = (Player) msg.obj;
                    postPlayerStateChanged(player);
                    break;
                }
            }
        }
    }

    @NonNull
    private Request serverStatusRequest() {
        return request("serverstatus")
                .defaultPage()
                .param("prefs", Player.Pref.MEDIA_DIRS + ", " + Player.Pref.DEFEAT_DESTRUCTIVE_TTP)
                .param("playerprefs", Player.Pref.PLAY_TRACK_ALBUM + "," + Player.Pref.DEFEAT_DESTRUCTIVE_TTP);
    }

    @NonNull
    private Request statusRequest(Player player) {
        return request(player, "status")
                .currentSong()
                .param("menu", "menu")
                .param("useContextMenu", "1");
    }

    private Request request(Player player, ResponseHandler callback, String... cmd) {
        return new Request(player, callback, cmd);
    }

    private Request request(Player player, String... cmd) {
        return new Request(player, null, cmd);
    }

    private Request request(ResponseHandler callback, String... cmd) {
        return new Request(null, callback, cmd);
    }

    private Request request(String... cmd) {
        return new Request(null, null, cmd);
    }

    private static class Request extends SlimCommand {
        private static final Joiner joiner = Joiner.on(" ");

        private final ResponseHandler callback;
        private final Player player;
        private PagingParams page;

        private Request(Player player, ResponseHandler callback, String... cmd) {
            this.player = player;
            this.callback = callback;
            this.cmd(cmd);
        }

        public Request param(String param, Object value) {
            super.param(param, value);
            return this;
        }

        public Request params(Map<String, Object> params) {
            super.params(params);
            return this;
        }

        private Request page(int start, int page) {
            this.page = new PagingParams(String.valueOf(start), String.valueOf(page));
            return this;
        }

        private Request defaultPage() {
            page = PagingParams._default;
            return this;
        }

        private Request currentSong() {
            page = PagingParams.status;
            return this;
        }

        public String getRequest() {
            return joiner.join(cmd);
        }

        List<Object> slimRequest() {
            List<Object> slimRequest = new ArrayList<>();

            slimRequest.add(player == null ? "" : player.getId());
            List<String> inner = new ArrayList<>();
            slimRequest.add(inner);
            inner.addAll(cmd);
            for (Map.Entry<String, Object> parameter : params.entrySet()) {
                if (parameter.getValue() == null) inner.add(parameter.getKey());
            }
            if (page != null) {
                inner.add(page.start);
                inner.add(page.page);
            }
            for (Map.Entry<String, Object> parameter : params.entrySet()) {
                if (parameter.getValue() != null) inner.add(parameter.getKey() + ":" + parameter.getValue());
            }

            return slimRequest;
        }
    }

    private static class PagingParams {
        private static final PagingParams status = new PagingParams("-", "1");
        private static final PagingParams _default = new PagingParams("0", "255");

        private final String start;
        private final String page;

        private PagingParams(String start, String page) {
            this.start = start;
            this.page = page;
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
