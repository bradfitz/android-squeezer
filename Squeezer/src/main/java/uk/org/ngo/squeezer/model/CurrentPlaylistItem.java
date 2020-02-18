/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

import android.os.Parcel;
import androidx.annotation.NonNull;

import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;


public class CurrentPlaylistItem extends Item {

    @NonNull private final String track;

    @NonNull
    public String getTrack() {
        return track.isEmpty() ? getName() : track;
    }

    @NonNull private final String artist;

    @NonNull
    public String getArtist() {
        return artist;
    }

    @NonNull
    private final String album;

    @NonNull
    public String getAlbum() {
        return album;
    }

    public CurrentPlaylistItem(Map<String, Object> record) {
        super(record);
        track = getStringOrEmpty(record, "track");
        artist = getStringOrEmpty(record, "artist");
        album = getStringOrEmpty(record, "album");
    }

    public static final Creator<CurrentPlaylistItem> CREATOR = new Creator<CurrentPlaylistItem>() {
        @Override
        public CurrentPlaylistItem[] newArray(int size) {
            return new CurrentPlaylistItem[size];
        }

        @Override
        public CurrentPlaylistItem createFromParcel(Parcel source) {
            return new CurrentPlaylistItem(source);
        }
    };

    private CurrentPlaylistItem(Parcel source) {
        super(source);
        track = source.readString();
        artist = source.readString();
        album = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(track);
        dest.writeString(artist);
        dest.writeString(album);
    }
    /**
     * Extend the equality test by looking at additional track information.
     *
     * @param o The object to test.
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        // super.equals() has already checked that o is not null and is of the same class.
        CurrentPlaylistItem s = (CurrentPlaylistItem)o;

        if (! s.getTrack().equals(track)) {
            return false;
        }

        if (! s.getAlbum().equals(album)) {
            return false;
        }

        if (! s.getArtist().equals(artist)) {
            return false;
        }

        if (! s.getIcon().equals(getIcon())) {
            return false;
        }

        return true;
    }
}
