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

package uk.org.ngo.squeezer.itemlists;

import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistItemMoveDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistSaveDialog;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Activity that shows the songs in the current playlist.
 */
public class SqueezerCurrentPlaylistActivity extends SqueezerBaseListActivity<SqueezerSong> {

	public static void show(Context context) {
	    final Intent intent = new Intent(context, SqueezerCurrentPlaylistActivity.class);
	    context.startActivity(intent);
	}

    private int currentPlaylistIndex;

    @Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(this) {

		    @Override
		    public android.view.View getAdapterView(android.view.View convertView, int index, SqueezerSong item) {
                View view = super.getView(convertView, item, index ==  currentPlaylistIndex);
                view.setBackgroundResource(index ==  currentPlaylistIndex ? R.drawable.list_item_background_current : R.drawable.list_item_background_normal);
		        return view;
		    };
		    
            /**
             * Jumps to whichever song the user chose.
             */
			@Override
			public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
				getActivity().getService().playlistIndex(index);
				getActivity().finish();
			}

            @Override
            public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
                super.setupContextMenu(menu, index, item);

                menu.setGroupVisible(R.id.group_playlist, true);

                if (index == 0)
                    menu.findItem(R.id.playlist_move_up).setVisible(false);

                if (index == getAdapter().getCount() - 1)
                    menu.findItem(R.id.playlist_move_down).setVisible(false);
            }

            @Override
            public boolean doItemContext(MenuItem menuItem, int index, SqueezerSong selectedItem)
                    throws RemoteException {
                switch (menuItem.getItemId()) {
                    case R.id.play_now:
                        getService().playlistIndex(index);
                        return true;

                    case R.id.remove_from_playlist:
                        getService().playlistRemove(index);
                        orderItems();
                        return true;

                    case R.id.playlist_move_up:
                        getService().playlistMove(index, index - 1);
                        orderItems();
                        return true;

                    case R.id.playlist_move_down:
                        getService().playlistMove(index, index + 1);
                        orderItems();
                        return true;

                    case R.id.playlist_move:
                        SqueezerPlaylistItemMoveDialog.addTo(SqueezerCurrentPlaylistActivity.this,
                                index);
                        return true;
                }

                return super.doItemContext(menuItem, index, selectedItem);
            };
        };
    }

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().currentPlaylist(start);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.currentplaylistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlist_clear:
			if (getService() != null)
				try {
					getService().playlistClear();
					finish();
				} catch (RemoteException e) {
					Log.e(getTag(), "Error trying to clear playlist: " + e);
				}
			return true;
		case R.id.menu_item_playlist_save:
	        new SqueezerPlaylistSaveDialog().show(getSupportFragmentManager(), "SaveDialog");
	        return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
    protected void registerCallback() throws RemoteException {
        getService().registerSongListCallback(songListCallback);
    }

    @Override
    protected void unregisterCallback() throws RemoteException {
        getService().unregisterSongListCallback(songListCallback);
    }

    private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
        public void onSongsReceived(int count, int start, List<SqueezerSong> items) throws RemoteException {
            currentPlaylistIndex = getService().getPlayerState().getCurrentPlaylistIndex();
            onItemsReceived(count, start, items);
            // Initially position the list at the currently playing song.
            // Do it again once it has loaded because the newly displayed items
            // may push the current song outside the displayed area.
            if (start == 0 || (start <= currentPlaylistIndex && currentPlaylistIndex < start + items.size()))
                selectCurrentSong(currentPlaylistIndex, start);
        }
    };

    private void selectCurrentSong(final int currentPlaylistIndex, final int start) {
        Log.i(getTag(), "set selection(" +start + "): "+ currentPlaylistIndex);
	    getListView().post(new Runnable() {
            public void run() {
                getListView().setSelectionFromTop(currentPlaylistIndex, 0);
            }
        });
    }

}
