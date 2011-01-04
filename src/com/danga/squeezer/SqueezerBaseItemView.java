package com.danga.squeezer;


import java.lang.reflect.Field;

import android.app.Activity;
import android.os.Parcelable.Creator;

public abstract class SqueezerBaseItemView<T extends SqueezerItem> implements SqueezerItemView<T> {
	private Activity activity;
	private Class<T> itemClass;
	private Creator<T> creator;

	public SqueezerBaseItemView(Activity activity) {
		this.activity = activity;
	}

	public Activity getActivity() {
		return activity;
	}
	
	@SuppressWarnings("unchecked")
	public Class<T> getItemClass() {
		if (itemClass  == null) {
			itemClass = (Class<T>) ReflectUtil.getGenericClass(getClass(), SqueezerItemView.class, 0);
			if (itemClass  == null)
				throw new RuntimeException("Could not read generic argument for: " + getClass());
		}
		return itemClass;
	}

	@SuppressWarnings("unchecked")
	public Creator<T> getCreator() {
		if (creator == null) {
			Field field;
			try {
				field = getItemClass().getField("CREATOR");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			try {
				creator = (Creator<T>) field.get(null);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return creator;
	}
	
}