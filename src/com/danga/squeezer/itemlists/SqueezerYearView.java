package com.danga.squeezer.itemlists;

import android.view.ContextMenu;
import android.view.Menu;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerYearView extends SqueezerBaseItemView<SqueezerYear> {

	public SqueezerYearView(SqueezerBaseActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.year, quantity);
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerYear item) {
		menu.setHeaderTitle(item.getId());
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_SONGS, 0, R.string.CONTEXTMENU_BROWSE_SONGS);
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ALBUMS, 1, R.string.CONTEXTMENU_BROWSE_ALBUMS);
		menu.add(Menu.NONE, CONTEXTMENU_PLAY_ITEM, 3, R.string.CONTEXTMENU_PLAY_ITEM);
		menu.add(Menu.NONE, CONTEXTMENU_ADD_ITEM, 4, R.string.CONTEXTMENU_ADD_ITEM);
	};

}
