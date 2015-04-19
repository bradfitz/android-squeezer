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

import android.support.annotation.NonNull;

import uk.org.ngo.squeezer.model.Player;

/** Event sent when a player's volume has changed. */
public class PlayerVolume {
    /** The player's new volume. */
    public final int volume;

    /** The player that was affected. */
    @NonNull
    public final Player player;

    public PlayerVolume(int volume, @NonNull Player player) {
        this.volume = volume;
        this.player = player;
    }
}
