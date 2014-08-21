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


public class Player extends Item {

    private final String ip;

    public String getIp() {
        return ip;
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public Player setName(String name) {
        this.name = name;
        return this;
    }

    private final boolean canpoweroff;

    public boolean isCanpoweroff() {
        return canpoweroff;
    }

    private final String model;

    public String getModel() {
        return model;
    }

    /** Is the player connected? */
    private boolean mConnected;

    public void setConnected(boolean connected) {
        mConnected = connected;
    }

    public boolean getConnected() {
        return mConnected;
    }

    public Player(Map<String, String> record) {
        setId(record.get("playerid"));
        ip = record.get("ip");
        name = record.get("name");
        model = record.get("model");
        canpoweroff = Util.parseDecimalIntOrZero(record.get("canpoweroff")) == 1;
        mConnected = Util.parseDecimalIntOrZero(record.get("connected")) == 1;
    }

    public static final Creator<Player> CREATOR = new Creator<Player>() {
        public Player[] newArray(int size) {
            return new Player[size];
        }

        public Player createFromParcel(Parcel source) {
            return new Player(source);
        }
    };

    private Player(Parcel source) {
        setId(source.readString());
        ip = source.readString();
        name = source.readString();
        model = source.readString();
        canpoweroff = (source.readByte() == 1);
        mConnected = (source.readByte() == 1);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(ip);
        dest.writeString(name);
        dest.writeString(model);
        dest.writeByte(canpoweroff ? (byte) 1 : (byte) 0);
        dest.writeByte(mConnected ? (byte) 1 : (byte) 0);
    }


    @Override
    public String toString() {
        return "id=" + getId() + ", name=" + name + ", model=" + model + ", canpoweroff="
                + canpoweroff + ", ip=" + ip + ", connected=" + mConnected;
    }

}
