/*
 * Copyright (c) 2012 Google Inc.
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

import java.util.Map;

import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;
import android.os.Parcel;

/**
 * Encapsulate a music folder item on the Squeezerserver.
 * <p>
 * An item has a name and a type. The name is free text, the type may be one of
 * "track", "folder", "playlist", or "unknown".
 * 
 * @author nik
 */
public class SqueezerMusicFolderItem extends SqueezerPlaylistItem {

    @Override
    public String getPlaylistTag() {
        if (type.equals("track")) {
            return "track_id";
        }

        if (type.equals("playlist")) {
            return "playlist_id";
        }

        if (type.equals("folder")) {
            return "folder_id";
        }

        return "Unknown_type_in_getTag()";
    }

    private String name;

    /** The folder item's type, "track", "folder", "playlist", "unknown". */
    private String type;

    @Override
    public String getName() {
        return name;
    }

    public SqueezerMusicFolderItem setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public SqueezerMusicFolderItem setType(String type) {
        this.type = type;
        return this;
    }

    public SqueezerMusicFolderItem(String musicFolderId, String musicFolder) {
        setId(musicFolderId);
        setName(musicFolder);
    }

    public SqueezerMusicFolderItem(Map<String, String> record) {
        setId(record.get("id"));
        name = record.get("filename");
        type = record.get("type");
    }

    public static final Creator<SqueezerMusicFolderItem> CREATOR = new Creator<SqueezerMusicFolderItem>() {
        public SqueezerMusicFolderItem[] newArray(int size) {
            return new SqueezerMusicFolderItem[size];
        }

        public SqueezerMusicFolderItem createFromParcel(Parcel source) {
            return new SqueezerMusicFolderItem(source);
        }
    };

    private SqueezerMusicFolderItem(Parcel source) {
        setId(source.readString());
        name = source.readString();
        type = source.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
        dest.writeString(type);
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", name=" + name + ", type=" + type;
    }
}
