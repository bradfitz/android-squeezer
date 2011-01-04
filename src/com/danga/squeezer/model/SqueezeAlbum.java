package com.danga.squeezer.model;

import java.util.Map;

import com.danga.squeezer.SqueezeItem;
import com.danga.squeezer.Util;

import android.os.Parcel;
import android.os.Parcelable;

public class SqueezeAlbum extends SqueezeItem {
	private String name;
	private String artist;
	private int year;
	private long artwork_track_id;

	public SqueezeAlbum() {
	}
	
	public SqueezeAlbum(Map<String, String> record) {
		setId(record.get("id"));
		setName(record.get("album"));
		setArtist(record.get("artist"));
		setYear(Util.parseDecimalIntOrZero(record.get("year"), 0));
		setArtwork_track_id(Util.parseDecimalIntOrZero(record.get("artwork_track_id"), -1));
	}
	
	public static final Parcelable.Creator<SqueezeAlbum> CREATOR = new Creator<SqueezeAlbum>() {
		public SqueezeAlbum[] newArray(int size) {
			return new SqueezeAlbum[size];
		}
		
		public SqueezeAlbum createFromParcel(Parcel source) {
			return new SqueezeAlbum(source);
		}
	};
	private SqueezeAlbum(Parcel source) {
		setId(source.readString());
		name = source.readString();
		artist = source.readString();
		year = source.readInt();
		artwork_track_id = source.readLong();
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeInt(year);
		dest.writeLong(artwork_track_id);
	}
	
	public String getName() {
		return name;
	}
	public SqueezeAlbum setName(String name) {
		this.name = name;
		return this;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String model) {
		this.artist = model;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public void setArtwork_track_id(long artwork_track_id) {
		this.artwork_track_id = artwork_track_id;
	}
	public long getArtwork_track_id() {
		return artwork_track_id;
	}

	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
