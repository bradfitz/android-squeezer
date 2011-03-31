package com.danga.squeezer.itemlists;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.model.SqueezerPlaylist;


public class SqueezerPlaylistView extends SqueezerBaseItemView<SqueezerPlaylist> {
	private static final int PLAYLISTS_CONTEXTMENU_DELETE_ITEM = 0;
	private static final int PLAYLISTS_CONTEXTMENU_RENAME_ITEM = 1;
	private static final int PLAYLISTS_CONTEXTMENU_BROWSE_SONGS = 2;
	private static final int PLAYLISTS_CONTEXTMENU_PLAY_ITEM = 3;
	private static final int PLAYLISTS_CONTEXTMENU_ADD_ITEM = 4;

	public SqueezerPlaylistView(SqueezerBaseActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.playlist, quantity);
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
				Bundle args = new Bundle();
				args.putParcelable("playlist", selectedItem);
				getActivity().showDialog(SqueezerPlaylistsActivity.DIALOG_DELETE, args);
			}
			return true;
		case PLAYLISTS_CONTEXTMENU_RENAME_ITEM:
			{
				Bundle args = new Bundle();
				args.putParcelable("playlist", selectedItem);
				getActivity().showDialog(SqueezerPlaylistsActivity.DIALOG_RENAME, args);
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
