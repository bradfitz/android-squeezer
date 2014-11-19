/*
 * Copyright (c) 2014 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.service.event;

import android.support.annotation.Nullable;

import java.util.List;

import uk.org.ngo.squeezer.model.Player;

/**
 * Event sent when either the currently active player has changed, or the list
 * of players connected to the server has changed.
 */
public class PlayersChanged {
    /** The players connected to the Squeezeserver. May be the empty list. */
    public final List<Player> mPlayers;

    /**
     * The player currently controlled by Squeezer. May be null, if no players
     * are currently being controlled.
     */
    @Nullable
    public final Player mActivePlayer;

    public PlayersChanged(List<Player> players, @Nullable Player activePlayer) {
        mPlayers = players;
        mActivePlayer = activePlayer;
    }
}
