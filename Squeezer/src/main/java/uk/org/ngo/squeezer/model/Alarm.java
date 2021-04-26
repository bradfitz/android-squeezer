/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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
import java.util.Set;
import java.util.TreeSet;

import uk.org.ngo.squeezer.Util;


public class Alarm extends Item {

    @Override
    public String getName() {
        return String.valueOf(tod);
    }

    public Alarm(Map<String, Object> record) {
        setId(getString(record, "id"));
        tod = getInt(record, "time");
        setDow(getString(record, "dow"));
        enabled = getInt(record, "enabled") == 1;
        repeat = getInt(record, "repeat") == 1;
        playListId = getString(record, "url");
        if ("CURRENT_PLAYLIST".equals(playListId)) playListId = "";
    }

    private int tod;
    public int getTod() {
        return tod;
    }
    public void setTod(int tod) {
        this.tod = tod;
    }

    private boolean repeat;
    public boolean isRepeat() {
        return repeat;
    }
    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    private boolean enabled;
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String playListId;
    public String getPlayListId() {
        return playListId;
    }
    public void setPlayListId(String playListId) {
        this.playListId = playListId;
    }

    private Set<Integer> dow = new TreeSet<>();

    private void setDow(String dowString) {
        dow.clear();
        String[] days = dowString.split(",");
        for (String day : days) {
            dow.add(Util.getInt(day));
        }
    }

    public boolean isDayActive(int day) {
        return dow.contains(day);
    }

    public void setDay(int day) {
        dow.add(day);
    }

    public void clearDay(int day) {
        dow.remove(day);
    }

    private String serializeDow() {
        StringBuilder sb = new StringBuilder();
        for (int day : dow) {
            if (sb.length() == 0) sb.append(',');
            sb.append(day);
        }
        return sb.toString();
    }

    public static final Creator<Alarm> CREATOR = new Creator<Alarm>() {
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }

        public Alarm createFromParcel(Parcel source) {
            return new Alarm(source);
        }
    };

    private Alarm(Parcel source) {
        setId(source.readString());
        tod = source.readInt();
        setDow(source.readString());
        enabled = (source.readInt() != 0);
        repeat = (source.readInt() != 0);
        playListId = source.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeInt(tod);
        dest.writeString(serializeDow());
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(repeat ? 1 : 0);
        dest.writeString(playListId);
    }

    @NonNull
    @Override
    public String toString() {
        return "id=" + getId() + ", tod=" + getName();
    }

}
