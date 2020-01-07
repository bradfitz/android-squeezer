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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Plugin;

class SlimDelegate {

    /** Shared event bus for status changes. */
    @NonNull private final EventBus mEventBus;
    @NonNull private final SlimClient mClient;


    SlimDelegate(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
        mClient = new CometClient(eventBus);
    }

    void startConnect(SqueezeService service) {
        mClient.startConnect(service);
    }

    void disconnect() {
        mClient.disconnect();
    }

    void cancelClientRequests(Object client) {
        mClient.cancelClientRequests(client);
    }


    void requestPlayerStatus(Player player) {
        mClient.requestPlayerStatus(player);
    }

    void subscribePlayerStatus(Player player, PlayerState.PlayerSubscriptionType subscriptionType) {
        mClient.subscribePlayerStatus(player, subscriptionType);
    }

    void subscribeDisplayStatus(Player player, boolean subscribe) {
        mClient.subscribeDisplayStatus(player, subscribe);
    }

    void subscribeMenuStatus(Player player, boolean subscribe) {
        mClient.subscribeMenuStatus(player, subscribe);
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

    Command command(Player player) {
        return new Command(mClient, player);
    }

    Command command() {
        return new Command(mClient);
    }

    /** If there is an active player call {@link #command(Player)} with the active player */
    Command activePlayerCommand() {
        return new PlayerCommand(mClient, mClient.getConnectionState().getActivePlayer());
    }

    <T extends Item> Request requestItems(Player player, int start, IServiceItemListCallback<T> callback) {
        return new Request<>(mClient, player, start, callback);
    }

    <T extends Item> Request requestItems(Player player, IServiceItemListCallback<T> callback) {
        return new Request<>(mClient, player, 0, 200, callback);
    }

    <T extends Item> Request requestItems(IServiceItemListCallback<T> callback) {
        return new Request<>(mClient, 0, 200, callback);
    }

    public Player getActivePlayer() {
        return mClient.getConnectionState().getActivePlayer();
    }

    void setActivePlayer(Player player) {
        mClient.getConnectionState().setActivePlayer(player);
    }

    Player getPlayer(String playerId) {
        return mClient.getConnectionState().getPlayer(playerId);
    }

    public Map<String, Player> getPlayers() {
        return mClient.getConnectionState().getPlayers();
    }

    void clearHomeMenu() {
        mClient.getConnectionState().clearHomeMenu();
    }

    void addToHomeMenu(int count, List<Plugin> items) {
        mClient.getConnectionState().addToHomeMenu(count, items);
    }

    public String getUsername() {
        return mClient.getUsername();
    }

    public String getPassword() {
        return mClient.getPassword();
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

        public Command params(Map<String, Object> params) {
            this.params.putAll(params);
            return this;
        }

        Command param(String tag, Object value) {
            params.put(tag, value);
            return this;
        }

        protected void exec() {
            slimClient.command(player, cmd.toArray(new String[0]), params);
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

        @Override
        protected void exec() {
            slimClient.requestItems(player, cmd.toArray(new String[0]), params, start, pageSize, callback);
        }
    }
}
