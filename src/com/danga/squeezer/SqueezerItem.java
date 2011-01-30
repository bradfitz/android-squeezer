package com.danga.squeezer;

import android.os.Parcelable;

/**
 * <p>
 * Base class for SqueezeServer data. Specializations must implement all the necessary boilerplate
 * code. This is okay for now, because we only have few data types.
 * </p>
 * @author Kurt Aaholst
 */
public abstract class SqueezerItem implements Parcelable {

	private String id;
	public void setId(String id) { this.id = id; }
	public String getId() { return id; }
	
	public int describeContents() {
		return 0;
	}
	
	@Override
	public int hashCode() {
		return (getId() != null ? getId().hashCode() : 0);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null) return false;
		if (o.getClass() != getClass()) return false; // There is no guarantee that SqueezerServer items have globally unique id's
		if (getId() != null) return getId().equals(((SqueezerItem)o).getId());
		return false;
	}

}
