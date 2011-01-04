/**
 * 
 */
package com.danga.squeezer;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Simple list adapter to display corresponding lists of images and labels.
 * 
 * @author Kurt Aaholst
 */
public class IconRowAdapter extends BaseAdapter {
	private Activity activity;
	private int rowLayout = R.layout.icon_small_row_layout;
	private int iconId = R.id.icon;
	private int textId = R.id.label;

	private int[] images;
	private String[] items;

	public int getCount() {
		return items.length;
	}
	
	public int getImage(int position) {
		return images[position];
	}

	public String getItem(int position) {
		return items[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public IconRowAdapter(Activity context, String[] items, int[] images) {
		this.items = items;
		this.activity = context;
		this.images = images;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = getActivity().getLayoutInflater().inflate(rowLayout, null);
		TextView label = (TextView) row.findViewById(textId);
		ImageView icon = (ImageView) row.findViewById(iconId);
		
		label.setText((CharSequence) items[position]);
		icon.setImageResource(images[position]);

		return row;
	}

	public Activity getActivity() {
		return activity;
	}

}