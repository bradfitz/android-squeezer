package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.framework.SqueezerItem;

public class SqueezerGenre extends SqueezerItem {
	private String name;
	@Override public String getName() { return name; }
	public SqueezerGenre setName(String name) { this.name = name; return this; }

	public SqueezerGenre(Map<String, String> record) {
		setId(record.containsKey("genre_id") ? record.get("genre_id") : record.get("id"));
		name = record.get("genre");
	}
	
	public static final Creator<SqueezerGenre> CREATOR = new Creator<SqueezerGenre>() {
		public SqueezerGenre[] newArray(int size) {
			return new SqueezerGenre[size];
		}

		public SqueezerGenre createFromParcel(Parcel source) {
			return new SqueezerGenre(source);
		}
	};
	private SqueezerGenre(Parcel source) {
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
