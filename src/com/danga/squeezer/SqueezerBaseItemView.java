package com.danga.squeezer;

import java.lang.reflect.Field;

import android.os.RemoteException;
import android.os.Parcelable.Creator;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.danga.squeezer.itemlists.SqueezerAlbumListActivity;
import com.danga.squeezer.itemlists.SqueezerArtistListActivity;
import com.danga.squeezer.itemlists.SqueezerSongListActivity;

public abstract class SqueezerBaseItemView<T extends SqueezerItem> implements SqueezerItemView<T> {
	private SqueezerBaseActivity activity;
	private Class<T> itemClass;
	private Creator<T> creator;

	public SqueezerBaseItemView(SqueezerBaseActivity activity) {
		this.activity = activity;
	}

	public SqueezerBaseActivity getActivity() {
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

	public View getAdapterView(View convertView, T item) {
		return Util.getListItemView(getActivity(), convertView, item.getName());
	}
	
	public boolean doItemContext(T selectedItem, MenuItem menuItem) throws RemoteException {
		switch (menuItem.getItemId()) {
		case CONTEXTMENU_BROWSE_SONGS:
			SqueezerSongListActivity.show(activity, selectedItem);
			return true;
		case CONTEXTMENU_BROWSE_ALBUMS:
			SqueezerAlbumListActivity.show(activity, selectedItem);
			return true;
		case CONTEXTMENU_BROWSE_ARTISTS:
			SqueezerArtistListActivity.show(activity, selectedItem);
			return true;
		case CONTEXTMENU_PLAY_ITEM:
			if (activity.play(selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_PLAYING, selectedItem.getName()), Toast.LENGTH_SHORT).show();
			return true;
		case CONTEXTMENU_ADD_ITEM:
			if (activity.add(selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_ADDED, selectedItem.getName()), Toast.LENGTH_SHORT).show();
			return true;
		case CONTEXTMENU_INSERT_ITEM:
			if (activity.insert(selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_INSERTED, selectedItem.getName()), Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

}