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

import android.support.annotation.NonNull;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.MusicChanged;
import uk.org.ngo.squeezer.service.event.PlayStatusChanged;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
import uk.org.ngo.squeezer.service.event.PowerStatusChanged;
import uk.org.ngo.squeezer.service.event.RepeatStatusChanged;
import uk.org.ngo.squeezer.service.event.ShuffleStatusChanged;
import uk.org.ngo.squeezer.service.event.SongTimeChanged;

abstract class BaseClient implements SlimClient {

    protected final ConnectionState mConnectionState;

    /** Executor for off-main-thread work. */
    @NonNull
    protected final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(1);

    /** Shared event bus for status changes. */
    @NonNull protected final EventBus mEventBus;

    /** The prefix for URLs for downloads and cover art. */
    protected String mUrlPrefix;

    protected final int mPageSize = Squeezer.getContext().getResources().getInteger(R.integer.PageSize);

    BaseClient(EventBus eventBus) {
        mEventBus = eventBus;
        mConnectionState = new ConnectionState(eventBus);
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



    protected void parseStatus(final Player player, Song song, Map<String, String> tokenMap) {
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
    protected void addArtworkUrlTag(Map<String, String> record) {
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
    protected void addDownloadUrlTag(Map<String, String> record) {
        record.put("download_url", mUrlPrefix + "/music/" + record.get("id") + "/download");
    }

    protected void updatePlayStatus(Player player, String playStatus) {
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

}
