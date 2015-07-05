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

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


public class AlarmPlaylist extends Item {

    private String title;

    @Override
    public String getName() {
        return title;
    }

    private String category;
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    private boolean singleton;
    public boolean isSingleton() {
        return singleton;
    }

    public AlarmPlaylist() {
    }

    public AlarmPlaylist(Map<String, String> record) {
        setId(record.get("url"));
        title = record.get("title");
        category = record.get("category");
        singleton = Util.parseDecimalIntOrZero(record.get("singleton")) == 1;
    }

    public static final Creator<AlarmPlaylist> CREATOR = new Creator<AlarmPlaylist>() {
        public AlarmPlaylist[] newArray(int size) {
            return new AlarmPlaylist[size];
        }

        public AlarmPlaylist createFromParcel(Parcel source) {
            return new AlarmPlaylist(source);
        }
    };

    private AlarmPlaylist(Parcel source) {
        setId(source.readString());
        title = source.readString();
        category = source.readString();
        singleton = (source.readInt() == 1);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(title);
        dest.writeString(category);
        dest.writeInt(singleton ? 1 : 0);
    }

    @Override
    public String toString() {
        return "url=" + getId() + ", title=" + getName();
    }
}
