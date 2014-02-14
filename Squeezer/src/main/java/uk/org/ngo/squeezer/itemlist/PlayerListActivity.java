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

import java.util.List;

import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Player;

public class PlayerListActivity extends BaseListActivity<Player> {

    private Player activePlayer;

    public Player getActivePlayer() {
        return activePlayer;
    }

    @Override
    public ItemView<Player> createItemView() {
        return new PlayerView(this);
    }

    @Override
    protected void registerCallback() {
        getService().registerPlayerListCallback(playerListCallback);
    }

    @Override
    protected void unregisterCallback() {
        getService().unregisterPlayerListCallback(playerListCallback);
    }

    @Override
    protected void orderPage(int start) {
        getService().players(start);
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, PlayerListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    private final IServicePlayerListCallback playerListCallback
            = new IServicePlayerListCallback() {
        public void onPlayersReceived(int count, int start, List<Player> items) {
            activePlayer = getService().getActivePlayer();
            onItemsReceived(count, start, items);
        }
    };

}
