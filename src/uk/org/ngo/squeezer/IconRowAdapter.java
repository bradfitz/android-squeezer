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

import java.util.ArrayList;
import java.util.List;

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

    /** Rows to display in the list. */
    List<IconRow> mRows = new ArrayList<IconRow>();

	public int getCount() {
        return mRows.size();
	}

	public int getImage(int position) {
        return mRows.get(position).getIcon();
	}

    public CharSequence getItem(int position) {
        return mRows.get(position).getText();
	}

	public long getItemId(int position) {
        return mRows.get(position).getId();
	}

    /**
     * Creates an IconRowAdapter where the id of each item corresponds to its
     * index in <code>items</code>.
     * <p>
     * <code>items</code> and <code>icons</item> must be the same size.
     * 
     * @param context
     * @param items Item text.
     * @param images Image resources.
     */
    public IconRowAdapter(Activity context, CharSequence[] items, int[] icons) {
        this.activity = context;

        // Convert to a list of IconRow.
        for (int i = 0; i < items.length; i++) {
            mRows.add(new IconRow(i, items[i], icons[i]));
        }
    }

    /**
     * Creates an IconRowAdapter from the list of <code>rows</code>.
     * 
     * @param context
     * @param rows Rows to appear in the list.
     */
    public IconRowAdapter(Activity context, List<IconRow> rows) {
        this.activity = context;
        mRows = rows;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = getActivity().getLayoutInflater().inflate(rowLayout, null);
		TextView label = (TextView) row.findViewById(textId);
		ImageView icon = (ImageView) row.findViewById(iconId);

        label.setText(mRows.get(position).getText());
        icon.setImageResource(mRows.get(position).getIcon());

		return row;
	}

	public Activity getActivity() {
		return activity;
	}

    /**
     * Helper class to represent a row. Each row has an identifier, a string,
     * and an icon.
     * <p>
     * The identifier should be unique across all rows in a given
     * {@link IconRowAdapter}, and will be used as the <code>id</code> paramter
     * to the <code>OnItemClickListener</code>.
     */
    public static class IconRow {
        private long mId;
        private CharSequence mText;
        private int mIcon;

        IconRow(long id, CharSequence text, int icon) {
            mId = id;
            mText = text;
            mIcon = icon;
        }

        public long getId() {
            return mId;
        }

        public void setId(long id) {
            mId = id;
        }

        public CharSequence getText() {
            return mText;
        }

        public void setText(String text) {
            mText = text;
        }

        public int getIcon() {
            return mIcon;
        }

        public void setIcon(int icon) {
            mIcon = icon;
        }
    }
}