/**
 * 
 */
package com.danga.squeezer;

import java.util.List;

import android.app.Activity;
import android.widget.Adapter;
import android.widget.BaseAdapter;


/**
 * <p>
 * A generic base class for an adapter to list items of a particular SqueezeServer data type. The data type
 * is defined by the generic type argument, and must be an extension of {@link SqueezeItem}.<br/>
 * In many cases just implement the {@link Adapter#getView(int, android.view.View, android.view.ViewGroup)} medthod. 
 * </p>
 * @param <T>	Denotes the class of the items this class should list
 * @author Kurt Aaholst
 */
public abstract class SqueezerBaseListAdapter<T extends SqueezeItem> extends BaseAdapter {
	private Activity activity;
	private T[] items;

	public int getCount() {
		return items.length;
	}
	
	public T getItem(int position) {
		return items[position];
	}

	public Activity getActivity() {
		return activity;
	}

	public long getItemId(int position) {
		return position;
	}

	public SqueezerBaseListAdapter(Activity activity, T[] items) {
		this.items = items;
		this.activity = activity;
	}

	public void update(int pos, List<T> list) {
		for (T t: list) {
			items[pos++] = t;
		}
		notifyDataSetChanged();
	}

}