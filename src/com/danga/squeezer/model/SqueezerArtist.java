package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.SqueezerItem;

public class SqueezerArtist extends SqueezerItem {
	private String name;
	public String getName() { return name; }
	public SqueezerArtist setName(String name) { this.name = name; return this; }

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
