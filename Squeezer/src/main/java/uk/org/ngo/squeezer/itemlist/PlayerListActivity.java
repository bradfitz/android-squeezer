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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;

import java.lang.ref.WeakReference;
import java.util.List;

import uk.org.ngo.squeezer.NowPlayingFragment;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.IServicePlayerStateCallback;
import uk.org.ngo.squeezer.service.IServicePlayersCallback;
import uk.org.ngo.squeezer.service.IServiceVolumeCallback;

public class PlayerListActivity extends ItemListActivity {
    public static final String CURRENT_PLAYER = "currentPlayer";

    private ExpandableListView mResultsExpandableListView;

    private PlayerListAdapter mResultsAdapter;

    private Player currentPlayer;
    private boolean mTrackingTouch;

    /** An update arrived while tracking touches. Player state should be resynced. */
    private boolean mUpdateWhileTracking = false;

    private final Handler uiThreadHandler = new UiThreadHandler(this);

    private final static class UiThreadHandler extends Handler {
        private static final int VOLUME_CHANGE = 1;
        private static final int PLAYER_STATE = 2;

        final WeakReference<PlayerListActivity> activity;

        public UiThreadHandler(PlayerListActivity activity) {
            this.activity = new WeakReference<PlayerListActivity>(activity);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case VOLUME_CHANGE:
                    activity.get().onVolumeChanged(message.arg1, (Player)message.obj);
                    break;
                case PLAYER_STATE:
                    activity.get().onPlayerStateReceived();
                    break;
            }
        }
    }

    private void onVolumeChanged(int newVolume, Player player) {
        PlayerState playerState = getService().getPlayerState(player.getId());
        if (playerState != null) {
            playerState.setCurrentVolume(newVolume);
            Log.d("PlayerListActivity", "Received new volume for + " + player.getName() + " vol: "+ newVolume);
            if (!mTrackingTouch)
                mResultsAdapter.notifyDataSetChanged();
        }
    }

    private void onPlayerStateReceived() {
        if (!mTrackingTouch) {
            mResultsAdapter.updatePlayers(getService().getPlayers(), getService().getActivePlayer());
            for (int i = 0; i < mResultsAdapter.getGroupCount(); i++) {
                mResultsExpandableListView.expandGroup(i);
            }
        } else {
            mUpdateWhileTracking = true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.item_list_players);
        if (savedInstanceState != null)
            currentPlayer = savedInstanceState.getParcelable(CURRENT_PLAYER);
        ((NowPlayingFragment) getSupportFragmentManager().findFragmentById(R.id.now_playing_fragment)).setIgnoreVolumeChange(true);

        mResultsAdapter = new PlayerListAdapter(this, getImageFetcher());
        mResultsExpandableListView = (ExpandableListView) findViewById(R.id.expandable_list);
        mResultsExpandableListView.setAdapter(mResultsAdapter);

        mResultsExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                mResultsAdapter.onChildClick(groupPosition, childPosition);
                return true;
            }
        });

        mResultsExpandableListView.setOnCreateContextMenuListener(mResultsAdapter);
        mResultsExpandableListView.setOnScrollListener(new ItemListActivity.ScrollListener());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CURRENT_PLAYER, currentPlayer);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void orderPage(int start) {
        getService().players();
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        getService().registerVolumeCallback(volumeCallback);
        getService().registerPlayersCallback(playersCallback);
        getService().registerPlayerStateCallback(playerStateCallback);
    }

    private final IServicePlayerStateCallback playerStateCallback
            = new IServicePlayerStateCallback() {
        @Override
        public void onPlayerStateReceived(final Player player, final PlayerState playerState) {
            uiThreadHandler.obtainMessage(UiThreadHandler.PLAYER_STATE, 0, 0).sendToTarget();
        }

        @Override
        public Object getClient() {
            return PlayerListActivity.this;
        }
    };

    private final IServiceVolumeCallback volumeCallback = new IServiceVolumeCallback() {
        @Override
        public void onVolumeChanged(final int newVolume, final Player player) {
            uiThreadHandler.obtainMessage(UiThreadHandler.VOLUME_CHANGE, newVolume, 0, player).sendToTarget();
        }

        @Override
        public Object getClient() {
            return PlayerListActivity.this;
        }

        @Override
        public boolean wantAllPlayers() {
            return true;
        }
    };

    private final IServicePlayersCallback playersCallback = new IServicePlayersCallback() {
        @Override
        public void onPlayersChanged(List<Player> players, Player activePlayer) {
            mResultsAdapter.updatePlayers(players, activePlayer);
            for (int i = 0; i < mResultsAdapter.getGroupCount(); i++) {
                mResultsExpandableListView.expandGroup(i);
            }
        }

        @Override
        public Object getClient() {
            return PlayerListActivity.this;
        }
    };

    public PlayerState getPlayerState(String id) {
        return getService().getPlayerState(id);
    }

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
                // XXX: Revisit later.
                // clearAndReOrderItems();
            }
        }
    }

    public void playerRename(String newName) {
        getService().playerRename(currentPlayer, newName);
        this.currentPlayer.setName(newName);
        mResultsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void clearItemAdapter() {
        mResultsAdapter.clear();
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, PlayerListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }
}
