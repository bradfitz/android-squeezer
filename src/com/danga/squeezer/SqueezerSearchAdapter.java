package com.danga.squeezer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.itemlists.SqueezerArtistView;
import com.danga.squeezer.itemlists.SqueezerGenreView;
import com.danga.squeezer.itemlists.SqueezerSearchAlbumView;
import com.danga.squeezer.itemlists.SqueezerSearchSongView;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerSearchAdapter extends BaseExpandableListAdapter {
	private int[] groupIcons = { R.drawable.icon_ml_songs, R.drawable.icon_ml_albums, R.drawable.icon_ml_artist, R.drawable.icon_ml_genres};

	private SqueezerSearchActivity activity;
	
	private SqueezerItemAdapter<? extends SqueezerItem>[] childAdapters;
	private Map<Class<?>,  SqueezerItemAdapter<? extends SqueezerItem>> childAdapterMap = new HashMap<Class<?>, SqueezerItemAdapter<? extends SqueezerItem>>();


	public SqueezerSearchAdapter(SqueezerSearchActivity activity) {
		this.activity = activity;
		SqueezerItemAdapter<?>[] adapters = {
			new SqueezerItemAdapter<SqueezerSong>(new SqueezerSearchSongView(activity)),
			new SqueezerItemAdapter<SqueezerAlbum>(new SqueezerSearchAlbumView(activity)),
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
	public <T extends SqueezerItem> void updateItems(Class<T> clazz, int count, int max, int start, List<T> items) {
		//TODO find the actual type based on runtime type of items
		SqueezerItemAdapter<T> adapter = (SqueezerItemAdapter<T>)childAdapterMap.get(clazz);
		adapter.update(count, max, start, items);
		notifyDataSetChanged();
	}

	public boolean isFullyLoaded() {
		for (SqueezerItemAdapter<? extends SqueezerItem> itemAdapter: childAdapters)
			if (!itemAdapter.isFullyLoaded()) return false;
		return true;
	}

	public int getMaxCount() {
		int count = 0;
		for (SqueezerItemAdapter<? extends SqueezerItem> itemAdapter: childAdapters)
			if (itemAdapter.getCount() > count) count = itemAdapter.getCount();
		return count;
	}


	public SqueezerItem getChild(int groupPosition, int childPosition) {
		return childAdapters[groupPosition].getItem(childPosition);
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

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View row = activity.getLayoutInflater().inflate(R.layout.group_item, null);

		TextView label = (TextView) row.findViewById(R.id.label);
		label.setText(childAdapters[groupPosition].getHeader(childAdapters[groupPosition].getCount()));

		ImageView icon = (ImageView) row.findViewById(R.id.icon);
		icon.setImageResource(groupIcons[groupPosition]);

		return (row);
	}

	public boolean hasStableIds() {
		return false;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}