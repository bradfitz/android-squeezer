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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.SqueezerActivity;
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistsDeleteDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistsRenameDialog;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;


public class SqueezerPlaylistView extends SqueezerBaseItemView<SqueezerPlaylist> {
	private static final int PLAYLISTS_CONTEXTMENU_DELETE_ITEM = 0;
	private static final int PLAYLISTS_CONTEXTMENU_RENAME_ITEM = 1;
	private static final int PLAYLISTS_CONTEXTMENU_BROWSE_SONGS = 2;
	private static final int PLAYLISTS_CONTEXTMENU_PLAY_ITEM = 3;
	private static final int PLAYLISTS_CONTEXTMENU_ADD_ITEM = 4;
	private final SqueezerPlaylistsActivity activity;

	public SqueezerPlaylistView(SqueezerPlaylistsActivity activity) {
		super(activity);
		this.activity = activity;
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.playlist, quantity);
	}

	public void onItemSelected(int index, SqueezerPlaylist item) throws RemoteException {
		getActivity().play(item);
		SqueezerActivity.show(getActivity());
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerPlaylist item) {
		menu.setHeaderTitle(item.getName());
		menu.add(Menu.NONE, PLAYLISTS_CONTEXTMENU_DELETE_ITEM, 0, R.string.menu_item_delete);
		menu.add(Menu.NONE, PLAYLISTS_CONTEXTMENU_RENAME_ITEM, 1, R.string.menu_item_rename);
		menu.add(Menu.NONE, PLAYLISTS_CONTEXTMENU_BROWSE_SONGS, 2, R.string.CONTEXTMENU_BROWSE_SONGS);
		menu.add(Menu.NONE, PLAYLISTS_CONTEXTMENU_PLAY_ITEM, 3, R.string.CONTEXTMENU_PLAY_ITEM);
		menu.add(Menu.NONE, PLAYLISTS_CONTEXTMENU_ADD_ITEM, 4, R.string.CONTEXTMENU_ADD_ITEM);
	};

	@Override
	public boolean doItemContext(MenuItem menuItem, int index, SqueezerPlaylist selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
		case PLAYLISTS_CONTEXTMENU_DELETE_ITEM:
			{
				activity.setCurrentPlaylist(selectedItem);
				new SqueezerPlaylistsDeleteDialog().show(activity.getSupportFragmentManager(), SqueezerPlaylistsDeleteDialog.class.getName());
			}
			return true;
		case PLAYLISTS_CONTEXTMENU_RENAME_ITEM:
			{
				activity.setCurrentPlaylist(selectedItem);
				new SqueezerPlaylistsRenameDialog().show(activity.getSupportFragmentManager(), SqueezerPlaylistsRenameDialog.class.getName());
			}
			return true;
		case PLAYLISTS_CONTEXTMENU_BROWSE_SONGS:
			SqueezerPlaylistSongsActivity.show(getActivity(), selectedItem);
			return true;
		case PLAYLISTS_CONTEXTMENU_PLAY_ITEM:
			getActivity().play(selectedItem);
			return true;
		case PLAYLISTS_CONTEXTMENU_ADD_ITEM:
			getActivity().add(selectedItem);
			return true;
		}
		return super.doItemContext(menuItem, index, selectedItem);
	}

}
