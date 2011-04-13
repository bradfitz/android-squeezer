package com.danga.squeezer.itemlists;

import android.view.ContextMenu;
import android.view.Menu;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerBaseItemView;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerArtist;


public class SqueezerArtistView extends SqueezerBaseItemView<SqueezerArtist> {

	public SqueezerArtistView(SqueezerItemListActivity activity) {
		super(activity);
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerArtist item) {
		menu.setHeaderTitle(item.getName());
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_SONGS, 0, R.string.CONTEXTMENU_BROWSE_SONGS);
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ALBUMS, 1, R.string.CONTEXTMENU_BROWSE_ALBUMS);
		menu.add(Menu.NONE, CONTEXTMENU_PLAY_ITEM, 3, R.string.CONTEXTMENU_PLAY_ITEM);
		menu.add(Menu.NONE, CONTEXTMENU_ADD_ITEM, 4, R.string.CONTEXTMENU_ADD_ITEM);
	};

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.artist, quantity);
	}

}
