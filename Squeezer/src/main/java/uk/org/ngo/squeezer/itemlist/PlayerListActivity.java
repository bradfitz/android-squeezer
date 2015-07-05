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

package uk.org.ngo.squeezer.itemlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerSyncDialog;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
import uk.org.ngo.squeezer.service.event.PlayerVolume;


public class PlayerListActivity extends ItemListActivity implements
        PlayerSyncDialog.PlayerSyncDialogHost {
    private static final String CURRENT_PLAYER = "currentPlayer";

    private ExpandableListView mResultsExpandableListView;

    private PlayerListAdapter mResultsAdapter;

    private Player currentPlayer;
    private boolean mTrackingTouch;

    /** An update arrived while tracking touches. UI should be re-synced. */
    private boolean mUpdateWhileTracking = false;

    /** Map from player IDs to Players synced to that player ID. */
    private final Multimap<String, Player> mPlayerSyncGroups = HashMultimap.create();

    /**
     * Updates the adapter with the current players, and ensures that the list view is
     * expanded.
     */
    private void updateAndExpandPlayerList() {
        // Can't do anything if the adapter hasn't been set (pre-handshake).
        if (mResultsExpandableListView.getAdapter() == null) {
            return;
        }

        updateSyncGroups(getService().getPlayers(), getService().getActivePlayer());
        mResultsAdapter.setSyncGroups(mPlayerSyncGroups);

        for (int i = 0; i < mResultsAdapter.getGroupCount(); i++) {
            mResultsExpandableListView.expandGroup(i);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.item_list_players);
        if (savedInstanceState != null)
            currentPlayer = savedInstanceState.getParcelable(CURRENT_PLAYER);

        mResultsAdapter = new PlayerListAdapter(this);
        mResultsExpandableListView = (ExpandableListView) findViewById(R.id.expandable_list);

        mResultsExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                mResultsAdapter.onChildClick(groupPosition, childPosition);
                return true;
            }
        });

        // Disable collapsing the list.
        mResultsExpandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition,
                    long id) {
                return true;
            }
        });

        mResultsExpandableListView.setOnCreateContextMenuListener(mResultsAdapter);
        mResultsExpandableListView.setOnScrollListener(new ItemListActivity.ScrollListener());

        setIgnoreVolumeChange(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CURRENT_PLAYER, currentPlayer);
        super.onSaveInstanceState(outState);
    }

    @Override
    public final boolean onContextItemSelected(MenuItem item) {
        if (getService() != null) {
            ExpandableListView.ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListView.ExpandableListContextMenuInfo) item
                    .getMenuInfo();

            // If menuInfo is null we have a sub menu, we expect the adapter to have stored the position
            if (contextMenuInfo == null) {
                return mResultsAdapter.doItemContext(item);
            } else {

                long packedPosition = contextMenuInfo.packedPosition;
                int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
                if (ExpandableListView.getPackedPositionType(packedPosition)
                        == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    return mResultsAdapter.doItemContext(item, groupPosition, childPosition);
                }
            }
        }
        return false;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        // Do nothing -- the service has been tracking players from the time it
        // initially connected to the server.
    }

    public void onEventMainThread(HandshakeComplete event) {
        if (mResultsExpandableListView.getExpandableListAdapter() == null)
            mResultsExpandableListView.setAdapter(mResultsAdapter);
        updateAndExpandPlayerList();
    }

    public void onEventMainThread(PlayerStateChanged event) {
        if (!mTrackingTouch) {
            updateAndExpandPlayerList();
        } else {
            mUpdateWhileTracking = true;
        }
    }

    public void onEventMainThread(PlayerVolume event) {
        if (!mTrackingTouch) {
            mResultsAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Builds the list of lists that is a sync group.
     *
     * @param players List of players.
     * @param activePlayer The currently active player.
     */
    public void updateSyncGroups(List<Player> players, Player activePlayer) {
        Map<String, Player> connectedPlayers = new HashMap<String, Player>();

        // Make a copy of the players we know about, ignoring unconnected ones.
        for (Player player : players) {
            if (!player.getConnected())
                continue;

            connectedPlayers.put(player.getId(), player);
        }

        mPlayerSyncGroups.clear();

        // Iterate over all the connected players to build the list of master players.
        for (Player player : connectedPlayers.values()) {
            String playerId = player.getId();
            PlayerState playerState = player.getPlayerState();
            String syncMaster = playerState.getSyncMaster();

            // If a player doesn't have a sync master then it's in a group of its own.
            if (syncMaster == null) {
                mPlayerSyncGroups.put(playerId, player);
                continue;
            }

            // If the master is this player then add itself and all the slaves.
            if (playerId.equals(syncMaster)) {
                mPlayerSyncGroups.put(playerId, player);
                continue;
            }

            // Must be a slave. Add it under the master. This might have already
            // happened (in the block above), but might not. For example, it's possible
            // to have a player that's a syncslave of an player that is not connected.
            mPlayerSyncGroups.put(syncMaster, player);
        }
    }

    @Override
    @NonNull
    public Multimap<String, Player> getPlayerSyncGroups() {
        return mPlayerSyncGroups;
    }

    public PlayerState getPlayerState(String id) {
        return getService().getPlayerState(id);
    }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void setTrackingTouch(boolean trackingTouch) {
        mTrackingTouch = trackingTouch;
        if (!mTrackingTouch) {
            if (mUpdateWhileTracking) {
                mUpdateWhileTracking = false;
                updateAndExpandPlayerList();
            }
        }
    }

    public void playerRename(String newName) {
        ISqueezeService service = getService();
        if (service == null) {
            return;
        }

        service.playerRename(currentPlayer, newName);
        this.currentPlayer.setName(newName);
        mResultsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void clearItemAdapter() {
        mResultsAdapter.clear();
    }

    /**
     * Synchronises the slave player to the player with masterId.
     *
     * @param slave the player to sync.
     * @param masterId ID of the player to sync to.
     */
    @Override
    public void syncPlayerToPlayer(@NonNull Player slave, @NonNull String masterId) {
        getService().syncPlayerToPlayer(slave, masterId);
    }

    /**
     * Removes the player from any sync groups.
     *
     * @param player the player to be removed from sync groups.
     */
    @Override
    public void unsyncPlayer(@NonNull Player player) {
        getService().unsyncPlayer(player);
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, PlayerListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }
}
