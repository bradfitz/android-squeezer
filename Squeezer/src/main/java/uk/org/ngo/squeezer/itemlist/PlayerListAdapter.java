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
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerSyncGroup;
import uk.org.ngo.squeezer.util.ImageFetcher;

class PlayerListAdapter extends BaseExpandableListAdapter implements View.OnCreateContextMenuListener {

    private final PlayerListActivity mActivity;

    private final List<ItemAdapter<Player>> mChildAdapters = new ArrayList<ItemAdapter<Player>>();

    private Player mActivePlayer;

    private List<Player> mPlayers;

    private List<PlayerSyncGroup> mPlayerSyncGroups;

    private ImageFetcher mImageFetcher;

    public PlayerListAdapter(PlayerListActivity activity, ImageFetcher imageFetcher) {
        mActivity = activity;
        mImageFetcher = imageFetcher;
    }

    public void onChildClick(int groupPosition, int childPosition) {
        mChildAdapters.get(groupPosition).onItemSelected(childPosition);
    }

    public void clear() {
        for (ItemAdapter<Player> itemAdapter : mChildAdapters) {
            itemAdapter.clear();
        }
        mChildAdapters.clear();
    }

    public void updatePlayers(List<Player> players, Player activePlayer) {
        mPlayers = players;
        mActivePlayer = activePlayer;
        update();
    }

    public void updateSyncGroups(List<PlayerSyncGroup> playerSyncGroups) {
        mPlayerSyncGroups = playerSyncGroups;
        update();
    }

    private void update() {
        clear();
        // How many adapters are needed?

        // No sync groups -- one adapter per player.
        if (true) { // (mPlayerSyncGroups.size() == 0) {
            for (Player player : mPlayers) {
                ItemAdapter<Player> childAdapter = new ItemAdapter<Player>(new PlayerView(mActivity), mImageFetcher);
                childAdapter.update(1, 0, ImmutableList.of(player));
                mChildAdapters.add(childAdapter);
                childAdapter.notifyDataSetChanged();
            }
        }

        notifyDataSetChanged();
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

            mChildAdapters.get(groupPosition).onCreateContextMenu(menu, v, adapterContextMenuInfo);
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true; // Should be false, but then there is no divider
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        return mChildAdapters.get(groupPosition).getView(childPosition, convertView, parent);
    }

    @Override
    public int getGroupCount() {
        return mChildAdapters.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildAdapters.get(groupPosition).getCount();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mChildAdapters.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return (Player) mChildAdapters.get(groupPosition).getItem(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View row = mActivity.getLayoutInflater().inflate(R.layout.group_item, parent, false);

        TextView label = (TextView) row.findViewById(R.id.label);

        ItemAdapter<Player> adapter = mChildAdapters.get(groupPosition);
        List<String> playerNames = new ArrayList<String>();
        for (int i = 0; i < adapter.getCount(); i++) {
            Player p = adapter.getItem(i);
            playerNames.add(p.getName());
        }
        String header = Joiner.on(", ").join(playerNames);
        label.setText(mActivity.getString(R.string.player_group_header, header));

        return row;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
