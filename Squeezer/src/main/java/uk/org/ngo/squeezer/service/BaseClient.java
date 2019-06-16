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

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.common.base.Splitter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.event.MusicChanged;
import uk.org.ngo.squeezer.service.event.PlayStatusChanged;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
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

    final static int mPageSize = Squeezer.getContext().getResources().getInteger(R.integer.PageSize);

    final AtomicReference<String> username = new AtomicReference<>();
    final AtomicReference<String> password = new AtomicReference<>();

    final ConnectionState mConnectionState;

    /** Shared event bus for status changes. */
    @NonNull final EventBus mEventBus;

    /** The prefix for URLs for downloads and cover art. */
    String mUrlPrefix;

    BaseClient(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
        mConnectionState = new ConnectionState(eventBus);
    }

    @Override
    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    @Override
    public <T extends Item> void requestItems(Player player, String[] cmd, Map<String, Object> params, int start, int pageSize, IServiceItemListCallback<T> callback) {
        final BaseClient.BrowseRequest<T> browseRequest = new BaseClient.BrowseRequest<>(player, cmd, params, start, pageSize, callback);
        internalRequestItems(browseRequest);
    }

    protected abstract <T extends Item> void internalRequestItems(BrowseRequest<T> browseRequest);

    @Override
    public String getUsername() {
        return username.get();
    }

    @Override
    public String getPassword() {
        return password.get();
    }



    void parseStatus(final Player player, Song song, Map<String, Object> tokenMap) {
        PlayerState playerState = player.getPlayerState();

        addArtworkUrlTag(tokenMap);
        addDownloadUrlTag(tokenMap);

        boolean unknownRepeatStatus = playerState.getRepeatStatus() == null;
        boolean unknownShuffleStatus = playerState.getShuffleStatus() == null;

        boolean changedPower = playerState.setPoweredOn(Util.getInt(tokenMap, "power") == 1);
        boolean changedShuffleStatus = playerState.setShuffleStatus(Util.getString(tokenMap, "playlist shuffle"));
        boolean changedRepeatStatus = playerState.setRepeatStatus(Util.getString(tokenMap, "playlist repeat"));
        playerState.setCurrentPlaylistTracksNum(Util.getInt(tokenMap, "playlist_tracks"));
        playerState.setCurrentPlaylistIndex(Util.getInt(tokenMap, "playlist_cur_index"));
        playerState.setCurrentPlaylist(Util.getString(tokenMap, "playlist_name"));
        boolean changedSleep = playerState.setSleep(Util.getInt(tokenMap, "will_sleep_in"));
        boolean changedSleepDuration = playerState.setSleepDuration(Util.getInt(tokenMap, "sleep"));
        if (song == null) song = new Song(tokenMap);
        boolean changedSong = playerState.setCurrentSong(song);
        boolean changedSongDuration = playerState.setCurrentSongDuration(Util.getInt(tokenMap, "duration"));
        boolean changedSongTime = playerState.setCurrentTimeSecond(Util.getInt(tokenMap, "time"));
        boolean changedVolume = playerState.setCurrentVolume(Util.getInt(tokenMap, "mixer volume"));
        boolean changedSyncMaster = playerState.setSyncMaster(Util.getString(tokenMap, "sync_master"));
        boolean changedSyncSlaves = playerState.setSyncSlaves(Splitter.on(",").omitEmptyStrings().splitToList(Util.getStringOrEmpty(tokenMap, "sync_slaves")));

        player.setPlayerState(playerState);

        // Kept as its own method because other methods call it, unlike the explicit
        // calls to the callbacks below.
        updatePlayStatus(player, Util.getString(tokenMap, "mode"));

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
    void addArtworkUrlTag(Map<String, Object> record) {
        String artworkUrl = Util.getString(record, "artwork_url");

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
        String artworkTrackId = Util.getString(record, "artwork_track_id");

        if (artworkTrackId != null) {
            record.put("artwork_url", mUrlPrefix + "/music/" + artworkTrackId + "/cover.jpg");
            return;
        }

        // If coverart exists but artwork_track_id is missing then use the item's ID.
        if ("1".equals(record.get("coverart"))) {
            record.put("artwork_url", mUrlPrefix + "/music/" + record.get("id") + "/cover.jpg");
            //return;
        }
    }

    /**
     * Adds a <code>download_url</code> entry for the item passed in.
     *
     * @param record The record to modify.
     */
    void addDownloadUrlTag(Map<String, Object> record) {
        record.put("download_url", mUrlPrefix + "/music/" + record.get("id") + "/download");
    }

    private void updatePlayStatus(Player player, String playStatus) {
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

    /** Make sure the icon/image tag is an absolute URL. */
    @SuppressWarnings("unchecked")
    void fixImageTag(Map<String, Object> record) {
        fixImageTag("icon", record);
        fixImageTag("icon-id", record);
    }

    /** Make sure the icon/image tag is an absolute URL. */
    private static final Pattern HEX_PATTERN = Pattern.compile("^\\p{XDigit}+$");
    void fixImageTag(String imageTag, Map<String, Object> record) {
        Object data = record.get(imageTag);
        if (data == null) {
            return;
        }

        String image = data.toString();
        if (HEX_PATTERN.matcher(image).matches()) {
            // if the iconId is a hex digit, this is a coverid or remote track id(a negative id)
            image = "/music/" + image + "/cover";
        }

        if (Uri.parse(image).isAbsolute()) {
            return;
        }

        record.put(imageTag, mUrlPrefix + (image.startsWith("/") ? image : "/" + image));
    }

    protected static class BrowseRequest<T extends Item> {
        private final Player player;
        private final String[] cmd;
        private final boolean fullList;
        private int start;
        private int itemsPerResponse;
        private final Map<String, Object> params;
        private final IServiceItemListCallback<T> callback;

        BrowseRequest(Player player, String[] cmd, Map<String, Object> params, int start, int itemsPerResponse, IServiceItemListCallback<T> callback) {
            this.player = player;
            this.cmd = cmd;
            this.fullList = (start < 0);
            this.start = (fullList ? 0 : start);
            this.itemsPerResponse = itemsPerResponse;
            this.callback = callback;
            this.params = (params == null ? Collections.<String, Object>emptyMap() : params);
        }

        public BrowseRequest update(int start, int itemsPerResponse) {
            this.start = start;
            this.itemsPerResponse = itemsPerResponse;
            return this;
        }

        public Player getPlayer() {
            return player;
        }

        public String[] getCmd() {
            return cmd;
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

        public Map<String, Object> getParams() {
            return params;
        }

        public IServiceItemListCallback<T> getCallback() {
            return callback;
        }
    }
}
