/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.framework;

/**
 * Items that can be added to Squeezerserver playlists (anything that can be
 * passed to the <code>playlistcontrol</code> command) should derive from this
 * class and implement {@link #getPlaylistTag()} to provide the correct playlist
 * tag.
 * <p>
 * See {@link SqueezerBaseActivity#playlistControl}.
 * 
 * @author nik
 */
public abstract class SqueezerPlaylistItem extends SqueezerItem {
    /**
     * Fetches the tag that represents this item in a
     * <code>playlistcontrol</code> command.
     * 
     * @return the tag, e.g., "album_id".
     */
    abstract public String getPlaylistTag();
}
