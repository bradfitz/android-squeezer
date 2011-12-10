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

import uk.org.ngo.squeezer.framework.SqueezerItem;

import android.os.Parcel;


public class SqueezerArtist extends SqueezerItem {
	private String name;
	@Override public String getName() { return name; }
	public SqueezerArtist setName(String name) { this.name = name; return this; }

	public SqueezerArtist(String artistId, String artist) {
		setId(artistId);
		setName(artist);
	}

	public SqueezerArtist(Map<String, String> record) {
		setId(record.containsKey("contributor_id") ? record.get("contributor_id") : record.get("id"));
		name = record.containsKey("contributor") ? record.get("contributor") : record.get("artist");
	}

	public static final Creator<SqueezerArtist> CREATOR = new Creator<SqueezerArtist>() {
		public SqueezerArtist[] newArray(int size) {
			return new SqueezerArtist[size];
		}

		public SqueezerArtist createFromParcel(Parcel source) {
			return new SqueezerArtist(source);
		}
	};
	private SqueezerArtist(Parcel source) {
		setId(source.readString());
		name = source.readString();
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
	}


	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name;
	}

}
