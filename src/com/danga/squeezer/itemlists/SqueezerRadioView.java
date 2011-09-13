package com.danga.squeezer.itemlists;

import android.os.RemoteException;
import android.view.ContextMenu;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerPlugin;

public class SqueezerRadioView extends SqueezerPluginView {

	public SqueezerRadioView(SqueezerItemListActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.radio, quantity);
	}

	public void onItemSelected(int index, SqueezerPlugin item) throws RemoteException {
		SqueezerPluginItemListActivity.show(getActivity(), item);
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerPlugin item) {
	}

}
