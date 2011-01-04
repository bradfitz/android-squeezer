package com.danga.squeezer.model;

import java.util.Map;

import com.danga.squeezer.SqueezeItem;
import com.danga.squeezer.Util;

import android.os.Parcel;
import android.os.Parcelable;

public class SqueezeSong extends SqueezeItem {
	private String name;
	private String artist;
	private String album;
	private int year;

	public SqueezeSong() {
	}
	
	public SqueezeSong(Map<String, String> record) {
		setId(record.get("playlist index"));
		setName(record.get("title"));
		setArtist(record.get("artist"));
		setAlbum(record.get("album"));
		setYear(Util.parseDecimalIntOrZero(record.get("year"), 0));
	}
	
	public static final Parcelable.Creator<SqueezeSong> CREATOR = new Creator<SqueezeSong>() {
		public SqueezeSong[] newArray(int size) {
			return new SqueezeSong[size];
		}
		
		public SqueezeSong createFromParcel(Parcel source) {
			return new SqueezeSong(source);
		}
	};
	private SqueezeSong(Parcel source) {
		setId(source.readString());
		name = source.readString();
		artist = source.readString();
		album = source.readString();
		year = source.readInt();
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeString(album);
		dest.writeInt(year);
	}
	
	public String getName() {
		return name;
	}
	public SqueezeSong setName(String name) {
		this.name = name;
		return this;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String model) {
		this.artist = model;
	}
	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAlbum() {
		return album;
	}

	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	
	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
