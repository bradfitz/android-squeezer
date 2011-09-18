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

package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.Util;
import com.danga.squeezer.framework.SqueezerItem;

public class SqueezerPlayer extends SqueezerItem {

	private String ip;
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }

	private String name;
	@Override public String getName() { return name; }
	public SqueezerPlayer setName(String name) { this.name = name;  return this; }

	private boolean canpoweroff;
	public boolean isCanpoweroff() { return canpoweroff; }
	public void setCanpoweroff(boolean canpoweroff) { this.canpoweroff = canpoweroff; }

	private String model;
	public String getModel() { return model; }
	public void setModel(String model) { this.model = model; }

	public SqueezerPlayer(Map<String, String> record) {
		setId(record.get("playerid"));
		ip = record.get("ip");
		name = record.get("name");
		model = record.get("model");
		canpoweroff = Util.parseDecimalIntOrZero(record.get("canpoweroff")) == 1;
	}

	public static final Creator<SqueezerPlayer> CREATOR = new Creator<SqueezerPlayer>() {
		public SqueezerPlayer[] newArray(int size) {
			return new SqueezerPlayer[size];
		}

		public SqueezerPlayer createFromParcel(Parcel source) {
			return new SqueezerPlayer(source);
		}
	};
	private SqueezerPlayer(Parcel source) {
		setId(source.readString());
		ip = source.readString();
		name = source.readString();
		model = source.readString();
		canpoweroff = (source.readByte() == 1);
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(ip);
		dest.writeString(name);
		dest.writeString(model);
		dest.writeByte(canpoweroff ? (byte)1 : (byte)0);
	}


	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", model=" + model + ", canpoweroff=" + canpoweroff + ", ip=" + ip;
	}

}
