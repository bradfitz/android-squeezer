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

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.IServicePlayerStateCallback;

public class PlayerListActivity extends BaseListActivity<Player> {
    public static final String CURRENT_PLAYER = "currentPlayer";

    Map<String, PlayerState> playerStates = new HashMap<String, PlayerState>();
    Player currentPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            currentPlayer = savedInstanceState.getParcelable(CURRENT_PLAYER);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CURRENT_PLAYER, currentPlayer);
        super.onSaveInstanceState(outState);
    }

    @Override
    public ItemView<Player> createItemView() {
        return new PlayerView(this);
    }

    @Override
    protected void orderPage(int start) {
        getService().players(start, this);
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        getService().registerPlayerStateCallback(playerStateCallback);
    }

    private final IServicePlayerStateCallback playerStateCallback
            = new IServicePlayerStateCallback() {
        @Override
        public void onPlayerStateReceived(final PlayerState playerState) {
            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    playerStates.put(playerState.getPlayerId(), playerState);
                    getItemAdapter().notifyDataSetChanged();
                }
            });
        }

        @Override
        public Object getClient() {
            return PlayerListActivity.this;
        }
    };

    public PlayerState getPlayerState(String id) {
        return playerStates.get(id);
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void playerRename(String newName) {
        getService().playerRename(currentPlayer, newName);
        this.currentPlayer.setName(newName);
        getItemAdapter().notifyDataSetChanged();
    }

    @Override
    protected ItemAdapter<Player> createItemListAdapter(ItemView<Player> itemView) {
        return new PlayerListAdapter(itemView, getImageFetcher());
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, PlayerListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }
}
