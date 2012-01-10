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

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.SqueezerItem;

import android.os.Parcel;


public class SqueezerPluginItem extends SqueezerItem {
	private String name;
	@Override public String getName() { return name; }
	public SqueezerPluginItem setName(String name) { this.name = name; return this; }

	private String description;
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	private String image;
	public String getImage() { return image; }
	public void setImage(String icon) { this.image = icon; }

	private boolean hasitems;
	public boolean isHasitems() { return hasitems; }
	public void setHasitems(boolean hasitems) { this.hasitems = hasitems; }

	private String type;
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }

	public SqueezerPluginItem(Map<String, String> record) {
		setId(record.get("id"));
		name = record.containsKey("name") ? record.get("name") : record.get("title");
		description = record.get("description");
		type = record.get("type");
		image = record.get("image");
		hasitems = (Util.parseDecimalIntOrZero(record.get("hasitems")) != 0);
	}

	public static final Creator<SqueezerPluginItem> CREATOR = new Creator<SqueezerPluginItem>() {
		public SqueezerPluginItem[] newArray(int size) {
			return new SqueezerPluginItem[size];
		}

		public SqueezerPluginItem createFromParcel(Parcel source) {
			return new SqueezerPluginItem(source);
		}
	};
	private SqueezerPluginItem(Parcel source) {
		setId(source.readString());
		name = source.readString();
		description = source.readString();
		type = source.readString();
		image = source.readString();
		hasitems = (source.readInt() != 0);
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(description);
		dest.writeString(type);
		dest.writeString(image);
		dest.writeInt(hasitems ? 1 : 0);
	}

	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name;
	}

}
