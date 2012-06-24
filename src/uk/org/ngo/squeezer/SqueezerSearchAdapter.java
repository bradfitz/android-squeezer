/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemAdapter;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumView;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistView;
import uk.org.ngo.squeezer.itemlists.SqueezerGenreView;
import uk.org.ngo.squeezer.itemlists.SqueezerSongView;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class SqueezerSearchAdapter extends BaseExpandableListAdapter {
    private final int[] groupIcons = {
            R.drawable.ic_songs, R.drawable.ic_albums, R.drawable.ic_artists, R.drawable.ic_genres
    };

	private SqueezerSearchActivity activity;

	private SqueezerItemAdapter<? extends SqueezerItem>[] childAdapters;
	private final Map<Class<? extends SqueezerItem>, SqueezerItemAdapter<? extends SqueezerItem>> childAdapterMap = new HashMap<Class<? extends SqueezerItem>, SqueezerItemAdapter<? extends SqueezerItem>>();


	public SqueezerSearchAdapter(SqueezerSearchActivity activity) {
		this.activity = activity;
		SqueezerItemAdapter<?>[] adapters = {
			new SqueezerItemAdapter<SqueezerSong>(new SqueezerSongView(activity) {
				@Override
				public View getAdapterView(View convertView, int index, SqueezerSong item) {
					return Util.getListItemView(getActivity(), convertView, item.getName());
				}
			}),
			new SqueezerItemAdapter<SqueezerAlbum>(new SqueezerAlbumView(activity) {
				@Override
				public View getAdapterView(View convertView, int index, SqueezerAlbum item) {
					return Util.getListItemView(getActivity(), convertView, item.getName());
				}
			}),
			new SqueezerItemAdapter<SqueezerArtist>(new SqueezerArtistView(activity)),
			new SqueezerItemAdapter<SqueezerGenre>(new SqueezerGenreView(activity)),
		};
		childAdapters = adapters;
		for (SqueezerItemAdapter<? extends SqueezerItem> itemAdapter: childAdapters)
			childAdapterMap.put(itemAdapter.getItemView().getItemClass(), itemAdapter);
	}

	public void clear() {
		for (SqueezerItemAdapter<? extends SqueezerItem> itemAdapter: childAdapters)
			itemAdapter.clear();
	}

	@SuppressWarnings("unchecked")
	public <T extends SqueezerItem> void updateItems(int count, int start, List<T> items, Class<T> clazz) {
		SqueezerItemAdapter<T> adapter = (SqueezerItemAdapter<T>)childAdapterMap.get(clazz);
		adapter.update(count, start, items);
		notifyDataSetChanged();
	}

	public int getMaxCount() {
		int count = 0;
		for (SqueezerItemAdapter<? extends SqueezerItem> itemAdapter: childAdapters)
			if (itemAdapter.getCount() > count) count = itemAdapter.getCount();
		return count;
	}

	public void setupContextMenu(ContextMenu menu, int groupPosition, int childPosition) {
		childAdapters[groupPosition].setupContextMenu(menu, childPosition);
	}

	public void doItemContext(MenuItem menuItem, int groupPosition, int childPosition) throws RemoteException {
		childAdapters[groupPosition].doItemContext(menuItem, childPosition);
	}

    public SqueezerPlaylistItem getChild(int groupPosition, int childPosition) {
        return (SqueezerPlaylistItem) childAdapters[groupPosition].getItem(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		return childAdapters[groupPosition].getView(childPosition, convertView, parent);
	}

	public int getChildrenCount(int groupPosition) {
		return childAdapters[groupPosition].getCount();
	}

	public Object getGroup(int groupPosition) {
		return childAdapters[groupPosition];
	}

	public int getGroupCount() {
		return childAdapters.length;
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        View row = activity.getLayoutInflater().inflate(R.layout.group_item, null);

        TextView label = (TextView) row.findViewById(R.id.label);
        label.setText(childAdapters[groupPosition].getHeader());

        // Build the icon to display next to the text.
        //
        // Take the normal icon (at 48dp) and scale it to 28dp, or 58% of its
        // original size. Then set it as the left-most compound drawable.

        Drawable icon = Squeezer.getContext().getResources().getDrawable(groupIcons[groupPosition]);
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        icon.setBounds(0, 0, (int) Math.ceil(w * 0.58), (int) Math.ceil(h * 0.58));

        label.setCompoundDrawables(icon, null, null, null);

        return (row);
    }

	public boolean hasStableIds() {
		return false;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}