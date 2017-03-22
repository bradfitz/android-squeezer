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

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;


public class Playlist extends PlaylistItem {

    @Override
    public String getPlaylistTag() {
        return "playlist_id";
    }

    @Override
    public String getFilterTag() {
        return "playlist_id";
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public Playlist setName(String name) {
        this.name = name;
        return this;
    }

    public Playlist(Map<String, Object> record) {
        setId(getString(record, record.containsKey("playlist_id") ? "playlist_id" : "id"));
        name = getString(record, "playlist");
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }

        @Override
        public Playlist createFromParcel(Parcel source) {
            return new Playlist(source);
        }
    };

    private Playlist(Parcel source) {
        setId(source.readString());
        name = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
    }

}
