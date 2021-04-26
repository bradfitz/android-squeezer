/*
 * Copyright (c) 2019 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.model;

import androidx.annotation.NonNull;

import java.util.Arrays;

/** Holds a menu status message from slimserver. */
public class MenuStatusMessage {
    public static final String ADD = "add";
    public static final String REMOVE = "remove";

    /** each entry contains a table that needs insertion into the menu */
    @NonNull
    public JiveItem[] menuItems;

    // directive for these items is in chunk.data[3]
    @NonNull
    public String menuDirective;

    // the player ID this notification is for is in chunk.data[4]
    @NonNull
    public String playerId;

    public MenuStatusMessage(@NonNull String playerId, @NonNull String menuDirective, @NonNull JiveItem[] menuItems) {
        this.playerId = playerId;
        this.menuDirective = menuDirective;
        this.menuItems = menuItems;
    }

    @Override
    public String toString() {
        return "MenuStatusMessage{" +
                "playerId='" + playerId + '\'' +
                ", menuDirective='" + menuDirective + '\'' +
                ", menuItems=" + Arrays.toString(menuItems) +
                '}';
    }
}
