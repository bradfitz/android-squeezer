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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


public class Alarm extends Item {

    @Override
    public String getName() {
        return String.valueOf(tod);
    }

    public Alarm(Map<String, String> record) {
        setId(record.get("id"));
        tod = Util.parseDecimalIntOrZero(record.get("time"));
        setDow(record.get("dow"));
        enabled = Util.parseDecimalIntOrZero(record.get("enabled")) == 1;
        repeat = Util.parseDecimalIntOrZero(record.get("repeat")) == 1;
        url = record.get("url");
        if ("CURRENT_PLAYLIST".equals(url)) url = "";
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

    private String url;
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    private Set<Integer> dow = new TreeSet<Integer>();

    private void setDow(String dowString) {
        dow.clear();
        String[] days = dowString.split(",");
        for (String day : days) {
            dow.add(Util.parseDecimalIntOrZero(day));
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
        url = source.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeInt(tod);
        dest.writeString(serializeDow());
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(repeat ? 1 : 0);
        dest.writeString(url);
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", tod=" + getName();
    }

}
