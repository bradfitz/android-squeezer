package com.danga.squeezer.itemlists;

import android.view.ContextMenu;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerPlugin;

public class SqueezerApplicationView extends SqueezerPluginView {
	public SqueezerApplicationView(SqueezerItemListActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.application, quantity);
	}

	public void onItemSelected(int index, SqueezerPlugin item) {
		//TODO what to do?
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerPlugin item) {
		//TODO what to do?
	}


}
