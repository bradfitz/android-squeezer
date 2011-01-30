package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.SqueezerArtworkItem;
import com.danga.squeezer.Util;

public class SqueezerSong extends SqueezerArtworkItem {

	private String name;
	public String getName() { return name; }
	public SqueezerSong setName(String name) { this.name = name; return this; }

	private String artist;
	public String getArtist() { return artist; }
	public void setArtist(String model) { this.artist = model; }
	
	private String album;
	public String getAlbum() { return album; }
	public void setAlbum(String album) { this.album = album; }
	
	private int year;
	public int getYear() { return year; }
	public void setYear(int year) { this.year = year; }
	
	public SqueezerSong(Map<String, String> record) {
		setId(record.containsKey("track_id") ? record.get("track_id") : record.get("playlist index"));
		setName(record.containsKey("track") ? record.get("track") : record.get("title"));
		setArtist(record.get("artist"));
		setAlbum(record.get("album"));
		setYear(Util.parseDecimalIntOrZero(record.get("year")));
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
		setArtwork_track_id(source.readString());
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeString(album);
		dest.writeInt(year);
		dest.writeString(getArtwork_track_id());
	}
	
	
	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
