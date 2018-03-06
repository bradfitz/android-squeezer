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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.framework.FilterItem;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Plugin;
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

    void requestItems(Player player, String[] cmd, Map<String, Object> params, int start, IServiceItemListCallback<Plugin> callback) {
        mClient.requestItems(player, cmd, params, start, (start == 0 ? 1 : BaseClient.mPageSize), callback);
    }

    void command(Player player, String[] cmd, Map<String, Object> params) {
        mClient.command(player, cmd, params);
    }

    Command command(Player player) {
        return new Command(mClient, player);
    }

    Command command() {
        return new Command(mClient);
    }

    /** Like #command({@link Player} but does nothing if player is null */
    Command playerCommand(Player player) {
        return new PlayerCommand(mClient, player);
    }

    <T extends Item> Request requestItems(Player player, int start, IServiceItemListCallback<T> callback) {
        return new Request<>(mClient, player, start, callback);
    }

    <T extends Item> Request requestItems(int start, int pageSize, IServiceItemListCallback<T> callback) {
        return new Request<>(mClient, start, pageSize, callback);
    }

    <T extends Item> Request requestItems(int start, IServiceItemListCallback<T> callback) {
        return new Request<>(mClient, start, callback);
    }

    static class Command {
        final SlimClient slimClient;
        final protected Player player;
        final protected List<String> cmd = new ArrayList<>();
        final protected Map<String, Object> params = new HashMap<>();

        private Command(SlimClient slimClient, Player player) {
            this.slimClient = slimClient;
            this.player = player;
        }

        private Command(SlimClient slimClient) {
            this(slimClient, null);
        }

        Command cmd(String... commandTerms) {
            cmd.addAll(Arrays.asList(commandTerms));
            return this;
        }

        Command param(String tag, Object value) {
            params.put(tag, value);
            return this;
        }

        Command songTags() {
            return param("tags", BaseClient.SONGTAGS);
        }

        Command albumTags() {
            return param("tags", BaseClient.ALBUMTAGS);
        }

        public Command sort(String sortOrder) {
            return param("sort", sortOrder);
        }

        public Command search(String searchString) {
            if (searchString != null && searchString.length() > 0) {
                params.put("search", searchString);
            }
            return this;
        }

        Command filter(FilterItem... filters) {
            for (FilterItem filter : filters)
                if (filter != null) {
                    params.put(filter.getFilterTag(), filter.getId());
                }
            return this;
        }

        Command playlistParam(PlaylistItem item) {
            return param(item.getPlaylistTag(), item.getId());
        }

        protected void exec() {
            slimClient.command(player, cmd.toArray(new String[cmd.size()]), params);
        }
    }

    static class PlayerCommand extends Command {

        private PlayerCommand(SlimClient slimClient, Player player) {
            super(slimClient, player);
        }

        @Override
        protected void exec() {
            if (player != null) super.exec();
        }
    }

    static class Request<T extends Item> extends Command {
        private final IServiceItemListCallback<T> callback;
        private final int start;
        private final int pageSize;

        private Request(SlimClient slimClient, Player player, int start, int pageSize, IServiceItemListCallback<T> callback) {
            super(slimClient, player);
            this.callback = callback;
            this.start = start;
            this.pageSize = pageSize;
        }

        private Request(SlimClient slimClient, Player player, int start, IServiceItemListCallback<T> callback) {
            this(slimClient, player, start, (start == 0 ? 1 : BaseClient.mPageSize), callback);
        }

        private Request(SlimClient slimClient, int start, int pageSize, IServiceItemListCallback<T> callback) {
            this(slimClient, null, start, pageSize, callback);
        }

        private Request(SlimClient slimClient, int start, IServiceItemListCallback<T> callback) {
            this(slimClient, null, start, callback);
        }

        @Override
        protected void exec() {
            slimClient.requestItems(player, cmd.toArray(new String[cmd.size()]), params, start, pageSize, callback);
        }
    }
}
