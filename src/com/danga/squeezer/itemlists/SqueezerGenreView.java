package com.danga.squeezer.itemlists;

import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerBaseItemView;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerGenre;


public class SqueezerGenreView extends SqueezerBaseItemView<SqueezerGenre> {

	public SqueezerGenreView(SqueezerItemListActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.genre, quantity);
	}

	public void onItemSelected(int index, SqueezerGenre item) throws RemoteException {
		SqueezerAlbumListActivity.show(getActivity(), item);
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerGenre item) {
		menu.setHeaderTitle(item.getName());
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_SONGS, 0, R.string.CONTEXTMENU_BROWSE_SONGS);
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ALBUMS, 1, R.string.CONTEXTMENU_BROWSE_ALBUMS);
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ARTISTS, 2, R.string.CONTEXTMENU_BROWSE_ARTISTS);
		menu.add(Menu.NONE, CONTEXTMENU_PLAY_ITEM, 3, R.string.CONTEXTMENU_PLAY_ITEM);
		menu.add(Menu.NONE, CONTEXTMENU_ADD_ITEM, 4, R.string.CONTEXTMENU_ADD_ITEM);
	};

}
