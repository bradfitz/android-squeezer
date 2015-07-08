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
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

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
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.ClientRequest;
import uk.org.ngo.squeezer.model.ClientRequestParameters;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.PlayersChanged;

public class JsonClient extends BaseClient {
    private static final String TAG = JsonClient.class.getSimpleName();

    private Map<String, Player> mPlayers;
    private final Map<Integer, IServiceItemListCallback> mPendingRequests
            = new ConcurrentHashMap<Integer, IServiceItemListCallback>();

    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private int mCorrelationId = 0;

    JsonClient(@NonNull EventBus eventBus) {
        super(eventBus);
    }

    @Override
    public void disconnect(boolean loginFailed) {

    }

    @Override
    public void sendCommandImmediately(String... commands) {

    }

    @Override
    public void sendCommand(String... commands) {

    }

    @WorkerThread
    public void sendCommandImmediately(@NonNull ClientRequest clientRequest) {
        try {
            String request = LoganSquare.serialize(clientRequest);
            Log.d("JSON", "Sending " + request);
            // XXX - hardcoded URL
            URL url = new URL("http://10.0.2.2:9000/jsonrpc.js");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                Writer writer = new PrintWriter(urlConnection.getOutputStream());
                writer.write(request);
                writer.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                Log.d("JSON", "Received " + in.readLine());
            } finally {
                urlConnection.disconnect();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // XXX Very similar to sendCommand(String ...) in CliClient.
    public void sendCommand(@NonNull final ClientRequest clientRequest) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            sendCommandImmediately(clientRequest);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sendCommandImmediately(clientRequest);
                }
            });
        }
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

    // Shims around ConnectionState methods.
    public void startConnect(final SqueezeService service, String hostPort, final String userName,
                      final String password) {
        //mConnectionState.startConnect(service, mEventBus, mExecutor, this, hostPort, userName, password);

        // There's no persistent connection to manage.  Run the connection state machine
        // so that the UI gets in to a consistent state
        mConnectionState.setConnectionState(mEventBus, ConnectionState.CONNECTION_STARTED);
        mConnectionState.setConnectionState(mEventBus, ConnectionState.CONNECTION_COMPLETED);
        mConnectionState.setConnectionState(mEventBus, ConnectionState.LOGIN_STARTED);
        mConnectionState.setConnectionState(mEventBus, ConnectionState.LOGIN_COMPLETED);
        fetchPlayers();
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
                return JsonClient.this;
            }
        });
    }

    @Override
    public void onLineReceived(String serverLine) {

    }

    @Override
    public String getPreferredAlbumSort() {
        return null;
    }

    @Override
    public void cancelClientRequests(Object object) {

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
    private void internalRequestItems(String playerId, String cmd, int start, int pageSize, List<String> parameters, IServiceItemListCallback callback) {
        mPendingRequests.put(mCorrelationId, callback);

        ClientRequestParameters clientRequestParameters = new ClientRequestParameters(
                playerId, cmd, start, pageSize, parameters.toArray(new String[parameters.size()]));

        ClientRequest clientRequest = new ClientRequest(mCorrelationId++, "slim.request",
                clientRequestParameters);

        sendCommand(clientRequest);
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
