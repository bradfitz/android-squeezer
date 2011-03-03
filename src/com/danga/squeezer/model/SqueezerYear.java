package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.SqueezerItem;

public class SqueezerYear extends SqueezerItem {

	public SqueezerYear(Map<String, String> record) {
		setId(record.get("year"));
	}
	
	public static final Creator<SqueezerYear> CREATOR = new Creator<SqueezerYear>() {
		public SqueezerYear[] newArray(int size) {
			return new SqueezerYear[size];
		}

		public SqueezerYear createFromParcel(Parcel source) {
			return new SqueezerYear(source);
		}
	};
	private SqueezerYear(Parcel source) {
		setId(source.readString());
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
	}

	@Override
	public String getName() {
		return getId();
	}
	
	@Override
	public String toString() {
		return "year=" + getId();
	}
	
}
