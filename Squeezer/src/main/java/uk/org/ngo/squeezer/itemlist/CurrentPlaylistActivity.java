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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;
import android.os.Handler;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.itemlist.dialog.PlaylistSaveDialog;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.MusicChanged;
import uk.org.ngo.squeezer.service.event.PlaylistTracksAdded;
import uk.org.ngo.squeezer.service.event.PlaylistTracksDeleted;

import static uk.org.ngo.squeezer.framework.BaseItemView.ViewHolder;

/**
 * Activity that shows the songs in the current playlist.
 */
public class CurrentPlaylistActivity extends PluginListActivity {

    public static void show(Context context) {
        final Intent intent = new Intent(context, CurrentPlaylistActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("cmd", "status");
        context.startActivity(intent);
    }

    /**
     * Handler for updating the index of the currently selected item in the playlist.
     * In theory you can post a Runnable to the ListView, but that appears to fail on
     * some OEM Android versions, whereas a dedicated Handler always works. See
     * https://github.com/nikclayton/android-squeezer/pull/164 for more.
     */
    private Handler playlistIndexUpdateHandler = new Handler();

    private int currentPlaylistIndex;

    /**
     * A list adapter that highlights the view that's currently playing.
     */
    private class HighlightingListAdapter extends ItemAdapter<Plugin> {

        public HighlightingListAdapter(ItemView<Plugin> itemView) {
            super(itemView);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            Object viewTag = view.getTag();

            // This test because the view tag wont be set until the album is received from the server
            if (viewTag instanceof ViewHolder) {
                ViewHolder viewHolder = (ViewHolder) viewTag;
                if (position == currentPlaylistIndex) {
                    viewHolder.text1
                            .setTextAppearance(getActivity(), R.style.SqueezerTextAppearance_ListItem_Primary);

                    // Changing the background resource to a 9-patch drawable causes the padding
                    // to be reset. See http://www.mail-archive.com/android-developers@googlegroups.com/msg09595.html
                    // for details. Save the current padding before setting the drawable, and
                    // restore afterwards.
                    int paddingLeft = view.getPaddingLeft();
                    int paddingTop = view.getPaddingTop();
                    int paddingRight = view.getPaddingRight();
                    int paddingBottom = view.getPaddingBottom();

                    view.setBackgroundResource(getAttributeValue(R.attr.playing_item));

                    view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                } else {
                    viewHolder.text1.setTextAppearance(getActivity(), R.style.SqueezerTextAppearance_ListItem_Primary);
                    view.setBackgroundColor(getAttributeValue(R.attr.background));
                }
            }
            return view;
        }
    }

    @Override
    protected ItemAdapter<Plugin> createItemListAdapter(
            ItemView<Plugin> itemView) {
        return new HighlightingListAdapter(itemView);
    }

    @Override
    public ItemView<Plugin> createItemView() {
        return new PluginView(CurrentPlaylistActivity.this) {

            @Override
            public boolean isSelectable(Plugin item) {
                return true;
            }

            /**
             * Jumps to whichever song the user chose.
             */
            @Override
            public void onItemSelected(int index, Plugin item) {

                // check first for a hierarchical menu or a input to perform
                if (item.hasSubItems() || item.hasInput()) {
                    super.onItemSelected(index, item);
                } else {
                    ISqueezeService service = getActivity().getService();
                    if (service != null) {
                        getActivity().getService().playlistIndex(index);
                    }
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.currentplaylistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Sets the enabled state of the R.menu.currentplaylistmenu items.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final int[] ids = {R.id.menu_item_playlist_clear, R.id.menu_item_playlist_save,
                R.id.menu_item_playlist_show_current_song};

        final boolean knowCurrentPlaylist = getCurrentPlaylist() != null;

        for (int id : ids) {
            MenuItem item = menu.findItem(id);
            item.setVisible(knowCurrentPlaylist);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_playlist_clear:
                if (getService() != null) {
                    getService().playlistClear();
                    finish();
                }
                return true;
            case R.id.menu_item_playlist_save:
                PlaylistSaveDialog.addTo(this, getCurrentPlaylist());
                return true;
            case R.id.menu_item_playlist_show_current_song:
                selectCurrentSong(currentPlaylistIndex, 0);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private String getCurrentPlaylist() {
        if (getService() == null) {
            return null;
        }
        return getService().getCurrentPlaylist();
    }

    public void onEventMainThread(MusicChanged event) {
        if (getService() == null) {
            return;
        }
        if (event.player.equals(getService().getActivePlayer())) {
            Log.d(getTag(), "onMusicChanged " + event.playerState.getCurrentSong());
            currentPlaylistIndex = event.playerState.getCurrentPlaylistIndex();
            getItemAdapter().notifyDataSetChanged();
        }
    }

    public void onEventMainThread(PlaylistTracksAdded event) {
        clearAndReOrderItems();
        getItemAdapter().notifyDataSetChanged();
    }

    public void onEventMainThread(PlaylistTracksDeleted event) {
        // TODO: Investigate feasibility of deleting single items from the adapter.
        clearAndReOrderItems();
        getItemAdapter().notifyDataSetChanged();
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, Object> parameters, List<Plugin> items, Class<Plugin> dataType) {
        for (Plugin item : items) {
            // check for a hierarchical menu or a input to perform
            if (!(item.hasSubItems() || item.hasInput())) {
                if (item.moreAction == null) {
                    item.moreAction = item.goAction;
                    item.goAction = null;
                }
            }
        }
        super.onItemsReceived(count, start, parameters, items, dataType);

        ISqueezeService service = getService();
        if (service == null) {
            return;
        }

        currentPlaylistIndex = service.getPlayerState().getCurrentPlaylistIndex();
        // Initially position the list at the currently playing song.
        // Do it again once it has loaded because the newly displayed items
        // may push the current song outside the displayed area.
        if (start == 0 || (start <= currentPlaylistIndex && currentPlaylistIndex < start + items
                .size())) {
            selectCurrentSong(currentPlaylistIndex, start);
        }
    }

    private void selectCurrentSong(final int currentPlaylistIndex, final int start) {
        Log.i(getTag(), "set selection(" + start + "): " + currentPlaylistIndex);
        playlistIndexUpdateHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: this doesn't work if the current playlist is displayed in a grid
                ((ListView) getListView()).setSelectionFromTop(currentPlaylistIndex, 0);
            }
        });
    }

}
