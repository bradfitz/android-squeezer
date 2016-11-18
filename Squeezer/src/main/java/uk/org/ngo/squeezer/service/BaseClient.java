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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.MusicChanged;
import uk.org.ngo.squeezer.service.event.PlayStatusChanged;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
import uk.org.ngo.squeezer.service.event.PlayersChanged;
import uk.org.ngo.squeezer.service.event.PowerStatusChanged;
import uk.org.ngo.squeezer.service.event.RepeatStatusChanged;
import uk.org.ngo.squeezer.service.event.ShuffleStatusChanged;
import uk.org.ngo.squeezer.service.event.SongTimeChanged;

abstract class BaseClient implements SlimClient {
    static final String ALBUMTAGS = "alyj";

    /**
     * Information that will be requested about songs.
     * <p>
     * a: artist name<br/>
     * C: compilation (1 if true, missing otherwise)<br/>
     * d: duration, in seconds<br/>
     * e: album ID<br/>
     * j: coverart (1 if available, missing otherwise)<br/>
     * J: artwork_track_id (if available, missing otherwise)<br/>
     * K: URL to remote artwork<br/>
     * l: album name<br/>
     * s: artist id<br/>
     * t: tracknum, if known<br/>
     * x: 1, if this is a remote track<br/>
     * y: song year<br/>
     * u: Song file url
     */
    // This should probably be a field in Song.
    static final String SONGTAGS = "aCdejJKlstxyu";

    // Where we connected (or are connecting) to:
    final AtomicReference<String> currentHost = new AtomicReference<>();
    final AtomicReference<Integer> httpPort = new AtomicReference<>();

    final ConnectionState mConnectionState;

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    final Map<String, Player> mPlayers = new HashMap<>();

    /** Executor for off-main-thread work. */
    @NonNull
    final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(1);

    /** Shared event bus for status changes. */
    @NonNull final EventBus mEventBus;

    /** The prefix for URLs for downloads and cover art. */
    String mUrlPrefix;

    final int mPageSize = Squeezer.getContext().getResources().getInteger(R.integer.PageSize);

    BaseClient(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
        mConnectionState = new ConnectionState(eventBus);
    }

    protected abstract void sendCommandImmediately(Player player, String command);
    protected abstract <T extends Item> void internalRequestItems(BrowseRequest<T> browseRequest);


    @Override
    public void command(final Player player, final String command) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            sendCommandImmediately(player, command);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sendCommandImmediately(player, command);
                }
            });
        }
    }

    @Override
    public void command(final String command) {
        command(null, command);
    }

    @Override
    public void playerCommand(Player player, String cmd) {
        if (player == null) return;
        command(player, cmd);
    }


    private <T extends Item> void internalRequestItems(Player player, Plugin plugin, String cmd, int start, int pageSize,
                                                          final IServiceItemListCallback<T> callback, List<String> parameters) {
        final BrowseRequest<T> browseRequest = new BrowseRequest<>(player, plugin, cmd, start, pageSize, callback, parameters);
        internalRequestItems(browseRequest);
    }

    @Override
    public <T extends Item> void requestItems(Player player, Plugin plugin, String cmd, int start, int pageSize, IServiceItemListCallback<T> callback, String... parameters) {
        internalRequestItems(player, plugin, cmd, start, pageSize, callback, Arrays.asList(parameters));
    }

    @Override
    public <T extends Item> void requestItems(Player player, String cmd, int start, int pageSize, IServiceItemListCallback<T> callback, String... parameters) {
        internalRequestItems(player, null, cmd, start, pageSize, callback, Arrays.asList(parameters));
    }

    @Override
    public <T extends Item> void requestItems(String cmd, int start, int pageSize, IServiceItemListCallback<T> callback, String... parameters) {
        requestItems(null, cmd, start, pageSize, callback, parameters);
    }

    @Override
    public <T extends Item> void requestItems(String cmd, int start, IServiceItemListCallback<T> callback, String... parameters) {
        requestItems(cmd, start, mPageSize, callback, parameters);
    }

    @Override
    public <T extends Item> void requestItems(String cmd, int start, IServiceItemListCallback<T> callback, List<String> parameters) {
        internalRequestItems(null, null, cmd, start, mPageSize, callback, parameters);
    }

    @Override
    public <T extends Item> void requestPlayerItems(Player player, Plugin plugin, String cmd, int start, int pageSize, IServiceItemListCallback<T> callback, List<String> parameters) {
        if (player == null) {
            return;
        }
        internalRequestItems(player, plugin, cmd, start, pageSize, callback, parameters);
    }

    @Override
    public <T extends Item> void requestPlayerItems(Player player, String cmd, int start, IServiceItemListCallback<T> callback, String... parameters) {
        requestPlayerItems(player, null, cmd, start, mPageSize, callback, Arrays.asList(parameters));
    }

    @Override
    public <T extends Item> void requestPlayerItems(Player player, Plugin plugin, String cmd, int start, IServiceItemListCallback<T> callback, List<String> parameters) {
        requestPlayerItems(player, plugin, cmd, start, mPageSize, callback, parameters);
    }

    public void initialize() {
        mEventBus.postSticky(new ConnectionChanged(ConnectionState.DISCONNECTED));
    }

    public boolean isConnected() {
        return mConnectionState.isConnected();
    }

    public boolean isConnectInProgress() {
        return mConnectionState.isConnectInProgress();
    }

    @Override
    public String getServerVersion() {
        return mConnectionState.getServerVersion();
    }

    public String[] getMediaDirs() {
        return mConnectionState.getMediaDirs();
    }

    public String getPreferredAlbumSort() {
        return mConnectionState.getPreferredAlbumSort();
    }

    /**
     * Queries for all players known by the server.
     * </p>
     * Posts a PlayersChanged message if the list of players has changed.
     */
    void fetchPlayers() {
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

            @Override
            public Object getClient() {
                return this;
            }
        });
    }

    int getHttpPort() {
        return httpPort.get();
    }

    String getCurrentHost() {
        return currentHost.get();
    }



    void parseStatus(final Player player, Song song, Map<String, String> tokenMap) {
        PlayerState playerState = player.getPlayerState();

        addArtworkUrlTag(tokenMap);
        addDownloadUrlTag(tokenMap);

        boolean unknownRepeatStatus = playerState.getRepeatStatus() == null;
        boolean unknownShuffleStatus = playerState.getShuffleStatus() == null;

        boolean changedPower = playerState.setPoweredOn(Util.parseDecimalIntOrZero(tokenMap.get("power")) == 1);
        boolean changedShuffleStatus = playerState.setShuffleStatus(tokenMap.get("playlist shuffle"));
        boolean changedRepeatStatus = playerState.setRepeatStatus(tokenMap.get("playlist repeat"));
        boolean changedSleep = playerState.setSleep(Util.parseDecimalIntOrZero(tokenMap.get("will_sleep_in")));
        boolean changedSleepDuration = playerState.setSleepDuration(Util.parseDecimalIntOrZero(tokenMap.get("sleep")));
        if (song == null) song = new Song(tokenMap);
        boolean changedSong = playerState.setCurrentSong(song);
        boolean changedSongDuration = playerState.setCurrentSongDuration(Util.parseDecimalIntOrZero(tokenMap.get("duration")));
        boolean changedSongTime = playerState.setCurrentTimeSecond(Util.parseDecimalIntOrZero(tokenMap.get("time")));
        boolean changedVolume = playerState.setCurrentVolume(Util.parseDecimalIntOrZero(tokenMap.get("mixer volume")));
        boolean changedSyncMaster = playerState.setSyncMaster(tokenMap.get("sync_master"));
        boolean changedSyncSlaves = playerState.setSyncSlaves(Splitter.on(",").omitEmptyStrings().splitToList(Strings.nullToEmpty(tokenMap.get("sync_slaves"))));

        player.setPlayerState(playerState);

        // Kept as its own method because other methods call it, unlike the explicit
        // calls to the callbacks below.
        updatePlayStatus(player, tokenMap.get("mode"));

        // XXX: Handled by onEvent(PlayStatusChanged) in the service.
        //updatePlayerSubscription(player, calculateSubscriptionTypeFor(player));

        // Note to self: The problem here is that with second-to-second updates enabled
        // the playerlistactivity callback will be called every second.  Thinking that
        // a better approach would be for clients to register a single callback and a
        // bitmask of events they're interested in based on the change* variables.
        // Each callback would be called a maximum of once, with the new player and a
        // bitmask that corresponds to which changes happened (so the client can
        // distinguish between the types of changes).

        // Might also be worth investigating Otto as an event bus instead.

        // Quick and dirty fix -- only call onPlayerStateReceived for changes to the
        // player state (ignore changes to Song, SongDuration, SongTime).

        if (changedPower || changedSleep || changedSleepDuration || changedVolume
                || changedSong || changedSyncMaster || changedSyncSlaves) {
            mEventBus.post(new PlayerStateChanged(player, playerState));
        }

        // Power status
        if (changedPower) {
            mEventBus.post(new PowerStatusChanged(
                    player,
                    !player.getPlayerState().isPoweredOn(),
                    !player.getPlayerState().isPoweredOn()));
        }

        // Current song
        if (changedSong) {
            mEventBus.postSticky(new MusicChanged(player, playerState));
        }

        // Shuffle status.
        if (changedShuffleStatus) {
            mEventBus.post(new ShuffleStatusChanged(player,
                    unknownShuffleStatus, playerState.getShuffleStatus()));
        }

        // Repeat status.
        if (changedRepeatStatus) {
            mEventBus.post(new RepeatStatusChanged(player,
                    unknownRepeatStatus, playerState.getRepeatStatus()));
        }

        // Position in song
        if (changedSongDuration || changedSongTime) {
            mEventBus.post(new SongTimeChanged(player,
                    playerState.getCurrentTimeSecond(),
                    playerState.getCurrentSongDuration()));
        }
    }

    /**
     * Adds a <code>artwork_url</code> entry for the item passed in.
     * <p>
     * If an <code>artwork_url</code> entry already exists and is absolute it is preserved.
     * If it exists but is relative it is canonicalised.  Otherwise it is synthesised from
     * the <code>artwork_track_id</code> tag (if it exists) otherwise the item's <code>id</code>.
     *
     * @param record The record to modify.
     */
    void addArtworkUrlTag(Map<String, String> record) {
        String artworkUrl = record.get("artwork_url");

        // Nothing to do if the artwork_url tag already exists and is absolute.
        if (artworkUrl != null && artworkUrl.startsWith("http")) {
            return;
        }

        // If artworkUrl is non-null it must be relative. Canonicalise it and return.
        if (artworkUrl != null) {
            record.put("artwork_url", mUrlPrefix + "/" + artworkUrl);
            return;
        }

        // Need to generate an artwork_url value.

        // Prefer using the artwork_track_id entry to generate the URL
        String artworkTrackId = record.get("artwork_track_id");

        if (artworkTrackId != null) {
            record.put("artwork_url", mUrlPrefix + "/music/" + artworkTrackId + "/cover.jpg");
            return;
        }

        // If coverart exists but artwork_track_id is missing then use the item's ID.
        if ("1".equals(record.get("coverart"))) {
            record.put("artwork_url", mUrlPrefix + "/music/" + record.get("id") + "/cover.jpg");
            return;
        }
    }

    /**
     * Adds a <code>download_url</code> entry for the item passed in.
     *
     * @param record The record to modify.
     */
    void addDownloadUrlTag(Map<String, String> record) {
        record.put("download_url", mUrlPrefix + "/music/" + record.get("id") + "/download");
    }

    void updatePlayStatus(Player player, String playStatus) {
        // Handle unknown states.
        if (!playStatus.equals(PlayerState.PLAY_STATE_PLAY) &&
                !playStatus.equals(PlayerState.PLAY_STATE_PAUSE) &&
                !playStatus.equals(PlayerState.PLAY_STATE_STOP)) {
            return;
        }

        PlayerState playerState = player.getPlayerState();

        if (playerState.setPlayStatus(playStatus)) {
            mEventBus.post(new PlayStatusChanged(playStatus, player));
        }
    }


    static class BrowseRequest<T extends Item> {
        private final Player player;
        private final Plugin plugin;
        private final String request;
        private final boolean fullList;
        private int start;
        private int itemsPerResponse;
        private final List<String> parameters;
        private final IServiceItemListCallback<T> callback;

        BrowseRequest(Player player, Plugin plugin, String cmd, int start, int itemsPerResponse, IServiceItemListCallback<T> callback, List<String> parameters) {
            this.player = player;
            this.plugin = plugin;
            this.request = cmd;
            this.fullList = (start < 0);
            this.start = (fullList ? 0 : start);
            this.itemsPerResponse = (start == 0 ? 1 : itemsPerResponse);
            this.callback = callback;
            this.parameters = parameters;
        }

        public BrowseRequest update(int start, int itemsPerResponse) {
            this.start = start;
            this.itemsPerResponse = itemsPerResponse;
            return this;
        }

        public Player getPlayer() {
            return player;
        }

        public Plugin getPlugin() {
            return plugin;
        }

        public String getRequest() {
            return request;
        }

        boolean isFullList() {
            return (fullList);
        }

        public int getStart() {
            return start;
        }

        int getItemsPerResponse() {
            return itemsPerResponse;
        }

        public List<String> getParameters() {
            return (parameters == null ? Collections.<String>emptyList() : parameters);
        }

        public IServiceItemListCallback<T> getCallback() {
            return callback;
        }
    }

}
