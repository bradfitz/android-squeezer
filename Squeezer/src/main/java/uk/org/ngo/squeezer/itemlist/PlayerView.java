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

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.itemlist.dialog.DefeatDestructiveTouchToPlayDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlayTrackAlbumDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerRenameDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerSyncDialog;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class PlayerView extends PlayerBaseView<PlayerListActivity> {

    public PlayerView(PlayerListActivity activity) {
        super(activity, R.layout.list_item_player);

        setViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE | VIEW_PARAM_CONTEXT_BUTTON);
        setLoadingViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE);
    }

    @Override
    public ViewHolder createViewHolder(View view) {
        return new PlayerViewHolder(view);
    }

    @Override
    public void bindView(View view, Player item) {
        PlayerState playerState = item.getPlayerState();
        PlayerViewHolder viewHolder = (PlayerViewHolder) view.getTag();

        super.bindView(view, item);
        viewHolder.icon.setImageResource(getModelIcon(item.getModel()));

        if (viewHolder.volumeBar == null) {
            viewHolder.volumeBar = view.findViewById(R.id.volume_slider);
            viewHolder.volumeBar.setOnSeekBarChangeListener(new VolumeSeekBarChangeListener(item, viewHolder.volumeValue));
        }

        viewHolder.volumeBar.setVisibility(View.VISIBLE);

        if (playerState.isPoweredOn()) {
            viewHolder.text1.setAlpha(1.0f);
        } else {
            viewHolder.text1.setAlpha(0.25f);
        }

        viewHolder.volumeBar.setProgress(playerState.getCurrentVolume());

        viewHolder.text2.setVisibility(playerState.getSleepDuration() > 0 ? View.VISIBLE : View.INVISIBLE);
        if (playerState.getSleepDuration() > 0) {
            viewHolder.text2.setText(activity.getString(R.string.SLEEPING_IN)
                    + " " + Util.formatElapsedTime(item.getSleepingIn()));
        }
    }

    @Override
    public void showContextMenu(ViewHolder viewHolder, final Player item) {
        PopupMenu popup = new PopupMenu(getActivity(), viewHolder.contextMenuButtonHolder);
        popup.inflate(R.menu.playercontextmenu);

        Menu menu = popup.getMenu();
        PlayerViewLogic.inflatePlayerActions(activity, popup.getMenuInflater(), menu);

        PlayerState playerState = item.getPlayerState();
        menu.findItem(R.id.cancel_sleep).setVisible(playerState.getSleepDuration() != 0);

        menu.findItem(R.id.end_of_song).setVisible(playerState.isPlaying());

        menu.findItem(R.id.toggle_power).setTitle(playerState.isPoweredOn() ? R.string.menu_item_power_off : R.string.menu_item_power_on);

        // Enable player sync menu options if there's more than one player.
        menu.findItem(R.id.player_sync).setVisible(activity.mResultsAdapter.mPlayerCount > 1);

        menu.findItem(R.id.play_track_album).setVisible(playerState.prefs.containsKey(Player.Pref.PLAY_TRACK_ALBUM));

        menu.findItem(R.id.defeat_destructive_ttp).setVisible(playerState.prefs.containsKey(Player.Pref.DEFEAT_DESTRUCTIVE_TTP));

        popup.setOnMenuItemClickListener(menuItem -> doItemContext(menuItem, item));

        activity.mResultsAdapter.mPlayersChanged = false;
        popup.show();
    }

    private boolean doItemContext(MenuItem menuItem, Player selectedItem) {
        if (activity.mResultsAdapter.mPlayersChanged) {
            Toast.makeText(activity, activity.getText(R.string.player_list_changed),
                    Toast.LENGTH_LONG).show();
            return true;
        }

        activity.setCurrentPlayer(selectedItem);
        ISqueezeService service = activity.getService();
        if (service == null) {
            return true;
        }

        if (PlayerViewLogic.doPlayerAction(service, menuItem, selectedItem)) {
            return  true;
        }

        switch (menuItem.getItemId()) {
            case R.id.rename:
                new PlayerRenameDialog().show(activity.getSupportFragmentManager(),
                        PlayerRenameDialog.class.getName());
                return true;
            case R.id.player_sync:
                new PlayerSyncDialog().show(activity.getSupportFragmentManager(),
                        PlayerSyncDialog.class.getName());
                return true;
            case R.id.play_track_album:
                PlayTrackAlbumDialog.show(activity);
                return true;
            case R.id.defeat_destructive_ttp:
                DefeatDestructiveTouchToPlayDialog.show(activity);
                return true;
        }

        return false;
    }

    private class VolumeSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final Player player;
        private final TextView valueView;

        public VolumeSeekBarChangeListener(Player player, TextView valueView) {
            this.player = player;
            this.valueView = valueView;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                ISqueezeService service = activity.getService();
                if (service == null) {
                    return;
                }
                service.adjustVolumeTo(player, progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            activity.setTrackingTouch(true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            activity.setTrackingTouch(false);
        }
    }

    private class PowerButtonClickListener implements View.OnClickListener {
        private final Player player;

        private PowerButtonClickListener(Player player) {
            this.player = player;
        }

        @Override
        public void onClick(View v) {
            ISqueezeService service = activity.getService();
            if (service == null) {
                return;
            }
            service.togglePower(player);
        }
    }

    private static class PlayerViewHolder extends ViewHolder {
        SeekBar volumeBar;
        TextView volumeValue;

        public PlayerViewHolder(@NonNull View view) {
            super(view);
        }
    }
}
