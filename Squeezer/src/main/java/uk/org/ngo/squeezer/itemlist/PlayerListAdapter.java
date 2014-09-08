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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.util.ImageFetcher;

class PlayerListAdapter extends BaseExpandableListAdapter implements View.OnCreateContextMenuListener {

    private final PlayerListActivity mActivity;

    private final List<ItemAdapter<Player>> mChildAdapters = new ArrayList<ItemAdapter<Player>>();

    private Player mActivePlayer;

    private List<Player> mPlayers = new ImmutableList.Builder<Player>().build();

    private final ImageFetcher mImageFetcher;

    public PlayerListAdapter(PlayerListActivity activity, ImageFetcher imageFetcher) {
        mActivity = activity;
        mImageFetcher = imageFetcher;
    }

    public void onChildClick(int groupPosition, int childPosition) {
        mChildAdapters.get(groupPosition).onItemSelected(childPosition);
    }

    public void clear() {
        mChildAdapters.clear();
        notifyDataSetChanged();
    }

    public void updatePlayers(List<Player> newPlayers, Player activePlayer) {
        mPlayers = ImmutableList.copyOf(newPlayers);
        mActivePlayer = activePlayer;

        clear();

        List<Player> players = new ArrayList<Player>();

        // Make a copy of the players we know about, ignoring unconnected ones.
        for (Player player : mPlayers) {
            if (!player.getConnected())
                continue;

            players.add(player);
        }

        // Map master player IDs to the list of the players they control.
        ListMultimap<String, String> syncGroups = ArrayListMultimap.create();

        String playerId;
        PlayerState playerState;
        String syncMaster;
        List<String> syncSlaves;
        List<Player> masterPlayers = new ArrayList<Player>();

        // Iterate over all the players to build the sync groups.
        for (Player player : players) {
            playerId = player.getId();
            playerState = mActivity.getPlayerState(playerId);


            // Ignore players that do not have a sync master, they don't participate in
            // a sync group.
            syncMaster = playerState.getSyncMaster();
            if (syncMaster == null)
                continue;

            // Ignore players that are not the master for their group.
            if (! syncMaster.equals(playerId))
                continue;

            syncGroups.putAll(playerId, playerState.getSyncSlaves());
            masterPlayers.add(player);
        }

        // Sort the list by ID to be stable.
        Collections.sort(masterPlayers, Player.compareById);

        // Iterate over each sync group master to create the child adapters.
        for (Player masterPlayer : masterPlayers) {
            ItemAdapter<Player> childAdapter = new ItemAdapter<Player>(new PlayerView(mActivity), mImageFetcher);
            List<Player> syncGroupPlayers = new ArrayList<Player>();

            // Add the master player as the first player in this group.
            syncGroupPlayers.add(masterPlayer);

            // Find all the slaves, add them to this list, and remove them from players.
            for (String id : syncGroups.get(masterPlayer.getId())) {
                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);

                    if (player.getId().equals(id)) {
                        syncGroupPlayers.add(player);
                        players.remove(i);
                        break;
                    }
                }
            }

            // Add all the found players in this sync group to this adapter.
            childAdapter.update(syncGroupPlayers.size(), 0, syncGroupPlayers);
            mChildAdapters.add(childAdapter);
        }

        // Sort the remaining list of players to be stable.
        Collections.sort(players, Player.compareById);

        // Any players still in players is not in a sync group.  Add each one to its own group.
        for (Player player : players) {
            // If any players are synced to this one then it's a master, and can be ignored.
            if (player.getPlayerState().getSyncSlaves().size() != 0)
                continue;

            ItemAdapter<Player> childAdapter = new ItemAdapter<Player>(new PlayerView(mActivity), mImageFetcher);
            childAdapter.update(1, 0, ImmutableList.of(player));
            mChildAdapters.add(childAdapter);
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

    /**
     * Generates a 64 bit group ID by using the 48 bits of the ID of the first player in
     * the group OR'd with 0x00FF000000000000.
     * <p/>
     * {@inheritDoc}
     * @param groupPosition
     * @return
     */
    @Override
    public long getGroupId(int groupPosition) {
        return mChildAdapters.get(groupPosition).getItem(0).getIdAsLong() | 0x00FF000000000000L;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return mChildAdapters.get(groupPosition).getItem(childPosition).getIdAsLong();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View row = mActivity.getLayoutInflater().inflate(R.layout.group_player, parent, false);

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
