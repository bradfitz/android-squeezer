/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer;

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
	private final Activity activity;
	private final int rowLayout = R.layout.icon_large_row_layout;
	private final int iconId = R.id.icon;
	private final int textId = R.id.label;

	private final int[] images;
	private final String[] items;

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

		label.setText(items[position]);
		icon.setImageResource(images[position]);

		return row;
	}

	public Activity getActivity() {
		return activity;
	}

}