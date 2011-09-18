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

public class SqueezerPlugin extends SqueezerItem {
	private String name;
	@Override public String getName() { return name; }
	public SqueezerPlugin setName(String name) { this.name = name; return this; }

	private String icon;
	public String getIcon() { return icon; }
	public void setIcon(String icon) { this.icon = icon; }

	private int weight;
	public int getWeight() { return weight; }
	public void setWeight(int weight) { this.weight = weight; }

	private String type;
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public boolean isSearchable() { return "xmlbrowser_search".equals(type); }

	public SqueezerPlugin(Map<String, String> record) {
		setId(record.get("cmd"));
		name = record.get("name");
		type = record.get("type");
		icon = record.get("icon");
		weight = Util.parseDecimalIntOrZero(record.get("weight"));
	}

	public static final Creator<SqueezerPlugin> CREATOR = new Creator<SqueezerPlugin>() {
		public SqueezerPlugin[] newArray(int size) {
			return new SqueezerPlugin[size];
		}

		public SqueezerPlugin createFromParcel(Parcel source) {
			return new SqueezerPlugin(source);
		}
	};
	private SqueezerPlugin(Parcel source) {
		setId(source.readString());
		name = source.readString();
		type = source.readString();
		icon = source.readString();
		weight = source.readInt();
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(type);
		dest.writeString(icon);
		dest.writeInt(weight);
	}

	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name;
	}

}
