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

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.List;

import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Player;

/**
 * Interface implemented by all network clients of the server.
 */
interface IClient {

    void initialize();  // XXX Call this onCreate()?

    // XXX: Document.
    void startConnect(final SqueezeService service, String hostPort, final String userName,
                      final String password);

    // XXX: Document
    void disconnect(boolean loginFailed);

    /**
     * Send the supplied commands to the SqueezeboxServer.
     * <p>
     * <b>All</b> data to the server goes through this method
     * <p>
     * <b>Note</b> don't call this from the main (UI) thread. If you are unsure if you are on the
     * main thread, then use {@link #sendCommand(String)} instead.
     *
     * @param commands List of commands to send
     */
    @WorkerThread
    void sendCommandImmediately(String... commands);

    /**
     * Send the supplied command to the SqueezeboxServer.
     * <p>
     * This method takes care to avoid performing network operations on the main thread. Use {@link
     * #sendCommandImmediately(String...)} if you are sure you are not on the main thread (eg if
     * called from the listening thread).
     *
     * @param command Command to send
     */
    void sendCommand(final String command);

    /**
     * Send the specified command for the specified player to the SqueezeboxServer
     *
     * @param command The command to send
     */
    void sendPlayerCommand(final Player player, final String command);

    void onLineReceived(String serverLine);

    String[] getMediaDirs();

    boolean isConnected();

    boolean isConnectInProgress();

    String getPreferredAlbumSort();
    String getServerVersion();

    void cancelClientRequests(Object object);

    void requestItems(String cmd, int start, List<String> parameters, IServiceItemListCallback callback);

    void requestItems(String cmd, int start, IServiceItemListCallback callback);

    void requestItems(String cmd, int start, int pageSize, IServiceItemListCallback callback);

    void requestPlayerItems(@Nullable Player player, String cmd, int start, List<String> parameters, IServiceItemListCallback callback);

}