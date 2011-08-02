package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.Util;
import com.danga.squeezer.framework.SqueezerArtworkItem;

public class SqueezerAlbum extends SqueezerArtworkItem {

	private String name;
	@Override public String getName() { return name; }
	public SqueezerAlbum setName(String name) { this.name = name; return this; }

	private String artist;
	public String getArtist() { return artist; }
	public void setArtist(String model) { this.artist = model; }

	private int year;
	public int getYear() { return year; }
	public void setYear(int year) { this.year = year; }
	
	public SqueezerAlbum(String albumId, String album) {
		setId(albumId);
		setName(album);
	}

	public SqueezerAlbum(Map<String, String> record) {
		setId(record.containsKey("album_id") ? record.get("album_id") : record.get("id"));
		setName(record.get("album"));
		setArtist(record.get("artist"));
		setYear(Util.parseDecimalIntOrZero(record.get("year")));
		setArtwork_track_id(record.get("artwork_track_id"));
	}
	
	public static final Creator<SqueezerAlbum> CREATOR = new Creator<SqueezerAlbum>() {
		public SqueezerAlbum[] newArray(int size) {
			return new SqueezerAlbum[size];
		}
		
		public SqueezerAlbum createFromParcel(Parcel source) {
			return new SqueezerAlbum(source);
		}
	};
	
	private SqueezerAlbum(Parcel source) {
		setId(source.readString());
		name = source.readString();
		artist = source.readString();
		year = source.readInt();
		setArtwork_track_id(source.readString());
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeInt(year);
		dest.writeString(getArtwork_track_id());
	}
	

	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
