package com.danga.squeezer.model;

import java.util.Map;

import com.danga.squeezer.SqueezeItem;

import android.os.Parcel;
import android.os.Parcelable;

public class SqueezeArtist extends SqueezeItem {
	private String name;

	public SqueezeArtist() {
	}

	public SqueezeArtist(Map<String, String> record) {
		setId(record.get("id"));
		name = record.get("artist");
	}
	
	public static final Parcelable.Creator<SqueezeArtist> CREATOR = new Creator<SqueezeArtist>() {
		public SqueezeArtist[] newArray(int size) {
			return new SqueezeArtist[size];
		}

		public SqueezeArtist createFromParcel(Parcel source) {
			return new SqueezeArtist(source);
		}
	};
	private SqueezeArtist(Parcel source) {
		setId(source.readString());
		name = source.readString();
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
	}
	
	public String getName() {
		return name;
	}
	public SqueezeArtist setName(String name) {
		this.name = name;
		return this;
	}
	
	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name;
	}
	
}
