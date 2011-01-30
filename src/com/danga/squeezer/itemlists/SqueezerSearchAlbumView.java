package com.danga.squeezer.itemlists;

import android.view.View;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.Util;
import com.danga.squeezer.model.SqueezerAlbum;

public class SqueezerSearchAlbumView extends SqueezerBaseItemView<SqueezerAlbum> {

	public SqueezerSearchAlbumView(SqueezerBaseActivity activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerAlbum item) {
		return Util.getListItemView(getActivity(), convertView, item.getName());
	}

	public void updateAdapterView(View view, SqueezerAlbum item) {
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.album, quantity);
	}

}
