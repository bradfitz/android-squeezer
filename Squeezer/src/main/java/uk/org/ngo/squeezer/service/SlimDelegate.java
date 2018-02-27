/*
 * Copyright (c) 2017 Kurt Aaholst <kaaholst@gmail.com>
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

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Map;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

class SlimDelegate {

    /** Shared event bus for status changes. */
    @NonNull private final EventBus mEventBus;
    @NonNull private final BaseClient mClient;


    SlimDelegate(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
        mClient = new CometClient(eventBus);
    }

    void startConnect(SqueezeService service, String host, int cliPort, int httpPort, String userName, String password) {
        mClient.startConnect(service, host, cliPort, httpPort, userName, password);
    }

    void disconnect(boolean loginFailed) {
        mClient.disconnect(loginFailed);
    }

    void cancelClientRequests(Object client) {
        mClient.cancelClientRequests(client);
    }

    void initialize() {
        mEventBus.postSticky(new ConnectionChanged(ConnectionState.DISCONNECTED));
    }


    public void command(final Player player, final String[] command, Map<String, Object> params) {
        mClient.command(player, command, params);
    }

    public void command(final Player player, final String[] command) {
        command(player, command, Collections.<String, Object>emptyMap());
    }

    public void command(final String[] command) {
        command(null, command);
    }

    public void command(String[] command, Map<String, Object> params) {
        command(null, command, params);
    }


    <T extends Item> void requestItems(Player player, String[] cmd, Map<String, Object> params, int start, IServiceItemListCallback<T> callback) {
        mClient.internalRequestItems(player, cmd, params, start, callback);
    }

    <T extends Item> void requestItems(Player player, String cmd, Map<String, Object> params, int start, IServiceItemListCallback<T> callback) {
        mClient.internalRequestItems(player, new String[]{cmd}, params, start, callback);

    }

    <T extends Item> void requestItems(String[] cmd, int start, int pageSize, IServiceItemListCallback<T> callback) {
        mClient.internalRequestItems(null, cmd, null, start, pageSize, callback);
    }

    <T extends Item> void requestItems(String[] cmd, Map<String, Object> params, int start, IServiceItemListCallback<T> callback) {
        mClient.internalRequestItems(null,cmd, params, start, callback);

    }

    <T extends Item> void requestItems(String cmd, Map<String, Object> params, int start, IServiceItemListCallback<T> callback) {
        mClient.internalRequestItems(null, new String[]{cmd}, params, start, callback);
    }

    <T extends Item> void requestItems(String cmd, int start, IServiceItemListCallback<T> callback) {
        mClient.internalRequestItems(null, new String[]{cmd}, null, start, callback);
    }


    void requestPlayerStatus(Player player) {
        mClient.requestPlayerStatus(player);
    }

    void subscribePlayerStatus(Player player, String subscriptionType) {
        mClient.subscribePlayerStatus(player, subscriptionType);
    }


    boolean isConnected() {
        return mClient.getConnectionState().isConnected();
    }

    boolean isConnectInProgress() {
        return mClient.getConnectionState().isConnectInProgress();
    }

    String getServerVersion() {
        return mClient.getConnectionState().getServerVersion();
    }

    String[] getMediaDirs() {
        return mClient.getConnectionState().getMediaDirs();
    }

    String getPreferredAlbumSort() {
        return mClient.getConnectionState().getPreferredAlbumSort();
    }
}
