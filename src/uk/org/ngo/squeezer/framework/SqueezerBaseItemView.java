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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public abstract class SqueezerBaseItemView<T extends SqueezerItem> implements SqueezerItemView<T> {
    protected static final int CONTEXTMENU_BROWSE_ALBUMS = 1;

    private final SqueezerItemListActivity mActivity;
    private final LayoutInflater mLayoutInflater;

    private SqueezerItemAdapter<T> mAdapter;
	private Class<T> mItemClass;
    private Creator<T> mCreator;

    public SqueezerBaseItemView(SqueezerItemListActivity activity) {
        this.mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
    }

    public SqueezerItemListActivity getActivity() {
        return mActivity;
    }

    public SqueezerItemAdapter<T> getAdapter() {
        return mAdapter;
    }

    public void setAdapter(SqueezerItemAdapter<T> adapter) {
        this.mAdapter = adapter;
    }

    public LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getItemClass() {
        if (mItemClass == null) {
            mItemClass = (Class<T>) ReflectUtil.getGenericClass(getClass(), SqueezerItemView.class,
                    0);
            if (mItemClass == null)
                throw new RuntimeException("Could not read generic argument for: " + getClass());
        }
        return mItemClass;
    }

    @SuppressWarnings("unchecked")
    public Creator<T> getCreator() {
        if (mCreator == null) {
            Field field;
            try {
                field = getItemClass().getField("CREATOR");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                mCreator = (Creator<T>) field.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mCreator;
    }

    protected String getTag() {
        return getClass().getSimpleName();
    }

    public View getAdapterView(View convertView, T item) {
        return getDefaultAdapterView(convertView, item);
    }

    /**
     * Create a view with a textview for the item description, and a button
     * that, when clicked, displays the context menu.
     */
    public View getDefaultAdapterView(View convertView, T item) {
        ViewHolder viewHolder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.label1 = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.btnContextMenu = (ImageButton) convertView.findViewById(R.id.context_menu);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.label1.setText(item.getName());

        viewHolder.btnContextMenu.setVisibility(View.VISIBLE);
        viewHolder.btnContextMenu.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                v.showContextMenu();
            }
        });

        return convertView;
    }

    public View getAdapterView(View convertView, String label) {
        return Util.getListItemView(mLayoutInflater, R.layout.textview, convertView, label);
    }

    /**
     * A ViewHolder for listitems that consist of a single textview and an
     * imagebutton for the context menu.
     */
    private static class ViewHolder {
        TextView label1;
        ImageButton btnContextMenu;
    }

    /**
     * The default context menu handler handles some common actions. Each action
     * must be set up in
     * {@link #setupContextMenu(android.view.ContextMenu, int, SqueezerItem)}
     */
    public boolean doItemContext(MenuItem menuItem, int index, T selectedItem)
            throws RemoteException {
        switch (menuItem.getItemId()) {
            case R.id.browse_songs:
                SqueezerSongListActivity.show(mActivity, selectedItem);
                return true;

            case CONTEXTMENU_BROWSE_ALBUMS:
                SqueezerAlbumListActivity.show(mActivity, selectedItem);
                return true;

            case R.id.browse_artists:
                SqueezerArtistListActivity.show(mActivity, selectedItem);
                return true;

            case R.id.play_now:
                if (mActivity.play((SqueezerPlaylistItem) selectedItem))
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.ITEM_PLAYING, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;

            case R.id.add_to_playlist:
                if (mActivity.add((SqueezerPlaylistItem) selectedItem))
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.ITEM_ADDED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;

            case R.id.play_next:
                if (mActivity.insert((SqueezerPlaylistItem) selectedItem))
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.ITEM_INSERTED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }

}
