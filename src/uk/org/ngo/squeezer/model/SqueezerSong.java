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
import uk.org.ngo.squeezer.framework.SqueezerArtworkItem;
import uk.org.ngo.squeezer.service.ISqueezeService;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class SqueezerSong extends SqueezerArtworkItem {

    @Override
    public String getPlaylistTag() {
        return "track_id";
    }

	private String name;
	@Override public String getName() { return name; }
	public SqueezerSong setName(String name) { this.name = name; return this; }

	private String artist;
	public String getArtist() { return artist; }
	public void setArtist(String artist) { this.artist = artist; }

	private String album;
	public String getAlbum() { return album; }
	public void setAlbum(String album) { this.album = album; }

	private int year;
	public int getYear() { return year; }
	public void setYear(int year) { this.year = year; }

	private String artist_id;
	public String getArtist_id() { return artist_id; }
	public void setArtist_id(String artist_id) { this.artist_id = artist_id; }

	private String album_id;
	public String getAlbum_id() { return album_id; }
	public void setAlbum_id(String album_id) { this.album_id = album_id; }

	private boolean remote;
	public boolean isRemote() { return remote; }
	public void setRemote(boolean remote) { this.remote = remote; }


	private String artwork_url;
	public String getArtwork_url() { return artwork_url; }
	public void setArtwork_url(String artworkUrl) { artwork_url = artworkUrl; }

    public String getArtworkUrl(ISqueezeService service) {
		if (getArtwork_track_id() != null) {
			try {
				if (service == null)
					return null;
				return service.getAlbumArtUrl(getArtwork_track_id());
			} catch (RemoteException e) {
				Log.e(getClass().getSimpleName(), "Error requesting album art url: " + e);
			}
		}
		return getArtwork_url();
    }

	public SqueezerSong(Map<String, String> record) {
		if (getId() == null) setId(record.get("track_id"));
		if (getId() == null) setId(record.get("id"));
		setName(record.containsKey("track") ? record.get("track") : record.get("title"));
		setArtist(record.get("artist"));
		setAlbum(record.get("album"));
		setYear(Util.parseDecimalIntOrZero(record.get("year")));
		setArtist_id(record.get("artist_id"));
		setAlbum_id(record.get("album_id"));
		setRemote(Util.parseDecimalIntOrZero(record.get("remote")) != 0);
		setArtwork_url(record.get("artwork_url"));
		setArtwork_track_id(record.get("artwork_track_id"));
	}

	public static final Creator<SqueezerSong> CREATOR = new Creator<SqueezerSong>() {
		public SqueezerSong[] newArray(int size) {
			return new SqueezerSong[size];
		}

		public SqueezerSong createFromParcel(Parcel source) {
			return new SqueezerSong(source);
		}
	};
	private SqueezerSong(Parcel source) {
		setId(source.readString());
		name = source.readString();
		artist = source.readString();
		album = source.readString();
		year = source.readInt();
		artist_id = source.readString();
		album_id = source.readString();
		setArtwork_track_id(source.readString());
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeString(album);
		dest.writeInt(year);
		dest.writeString(artist_id);
		dest.writeString(album_id);
		dest.writeString(getArtwork_track_id());
	}


	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
