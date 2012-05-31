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

package uk.org.ngo.squeezer.framework;

import java.lang.reflect.Field;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.ReflectUtil;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Represents the view hierarchy for a single {@link SqueezerItem} subclass.
 * <p>
 * The view has a context menu.
 * 
 *  @param <T> the SqueezerItem subclass this view represents.
 */
public abstract class SqueezerBaseItemView<T extends SqueezerItem> implements SqueezerItemView<T> {
    protected static final int CONTEXTMENU_BROWSE_ALBUMS = 1;

	private final SqueezerItemListActivity activity;
	private SqueezerItemAdapter<T> adapter;
	private Class<T> itemClass;
	private Creator<T> creator;

	public SqueezerBaseItemView(SqueezerItemListActivity activity) {
		this.activity = activity;
	}

	public SqueezerItemListActivity getActivity() {
		return activity;
	}

	public SqueezerItemAdapter<T> getAdapter() {
		return adapter;
	}

	public void setAdapter(SqueezerItemAdapter<T> adapter) {
		this.adapter = adapter;
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

    public View getAdapterView(View convertView, String label) {
        return Util.getListItemView(getActivity(), convertView, label);
    }


	/**
	 * The default context menu handler handles some common actions.
	 * Each action must be set up in {@link #setupContextMenu(android.view.ContextMenu, int, SqueezerItem)}
	 */
	public boolean doItemContext(MenuItem menuItem, int index, T selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
            case R.id.browse_songs:
                SqueezerSongListActivity.show(activity, selectedItem);
                return true;

		case CONTEXTMENU_BROWSE_ALBUMS:
			SqueezerAlbumListActivity.show(activity, selectedItem);
			return true;

            case R.id.browse_artists:
                SqueezerArtistListActivity.show(activity, selectedItem);
                return true;

            case R.id.play_now:
                if (activity.play((SqueezerPlaylistItem) selectedItem))
                    Toast.makeText(activity,
                            activity.getString(R.string.ITEM_PLAYING, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;

            case R.id.add_to_playlist:
                if (activity.add((SqueezerPlaylistItem) selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_ADDED, selectedItem.getName()), Toast.LENGTH_SHORT).show();
                return true;

            case R.id.play_next:
                if (activity.insert((SqueezerPlaylistItem) selectedItem))
                    Toast.makeText(activity,
                            activity.getString(R.string.ITEM_INSERTED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;
		}
		return false;
	}

}