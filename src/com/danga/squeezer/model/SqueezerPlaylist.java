package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.SqueezerItem;

public class SqueezerPlaylist extends SqueezerItem {
	private String name;
	@Override public String getName() { return name; }
	public SqueezerPlaylist setName(String name) { this.name = name; return this; }

	public SqueezerPlaylist(Map<String, String> record) {
		setId(record.containsKey("playlist_id") ? record.get("playlist_id") : record.get("id"));
		name = record.get("playlist");
	}
	
	public static final Creator<SqueezerPlaylist> CREATOR = new Creator<SqueezerPlaylist>() {
		public SqueezerPlaylist[] newArray(int size) {
			return new SqueezerPlaylist[size];
		}

		public SqueezerPlaylist createFromParcel(Parcel source) {
			return new SqueezerPlaylist(source);
		}
	};
	private SqueezerPlaylist(Parcel source) {
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
