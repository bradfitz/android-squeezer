/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.itemlist;

import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.util.ImageFetcher;

class PlayerListAdapter extends BaseExpandableListAdapter implements View.OnCreateContextMenuListener {

    private final PlayerListActivity mActivity;

    private final ItemAdapter<? extends Item>[] mChildAdapters;

    private final Map<Class<? extends Item>, ItemAdapter<? extends Item>> mChildAdapterMap
            = new HashMap<Class<? extends Item>, ItemAdapter<? extends Item>>();

    public PlayerListAdapter(PlayerListActivity activity, ImageFetcher imageFetcher) {
        mActivity = activity;

        ItemAdapter<?>[] adapters = {
//                new ItemAdapter<Song>(new SongViewWithArt(activity), imageFetcher),
//                new ItemAdapter<Album>(new AlbumView(activity), imageFetcher),
//                new ItemAdapter<Artist>(new ArtistView(activity)),
//                new ItemAdapter<Genre>(new GenreView(activity)),
        };

        mChildAdapters = adapters;
        for (ItemAdapter<? extends Item> itemAdapter : mChildAdapters) {
            mChildAdapterMap.put(itemAdapter.getItemView().getItemClass(), itemAdapter);
        }
    }

    public void onChildClick(int groupPosition, int childPosition) {
        mChildAdapters[groupPosition].onItemSelected(childPosition);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        ExpandableListView.ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        long packedPosition = contextMenuInfo.packedPosition;
        if (ExpandableListView.getPackedPositionType(packedPosition)
                == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);

            AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = new AdapterView.AdapterContextMenuInfo(
                    contextMenuInfo.targetView, childPosition, contextMenuInfo.id);

            mChildAdapters[groupPosition].onCreateContextMenu(menu, v, adapterContextMenuInfo);
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true; // Should be false, but then there is no divider
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        return mChildAdapters[groupPosition].getView(childPosition, convertView, parent);
    }

    @Override
    public int getGroupCount() {
        return 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
