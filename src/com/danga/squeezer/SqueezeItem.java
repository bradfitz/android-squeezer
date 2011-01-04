package com.danga.squeezer;

import android.os.Parcelable;

/**
 * <p>
 * Base class for SqueezeServer data. Specializations must implement all the necessary boilerplate
 * code. This is okay for now, because we only have few data types.
 * </p>
 * @author Kurt Aaholst
 */
public abstract class SqueezeItem implements Parcelable {
	private String id;

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public int describeContents() {
		return 0;
	}

}
