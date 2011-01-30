package com.danga.squeezer.itemlists;

import android.view.View;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.Util;
import com.danga.squeezer.model.SqueezerArtist;


public class SqueezerArtistView extends SqueezerBaseItemView<SqueezerArtist> {

	public SqueezerArtistView(SqueezerBaseActivity activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerArtist item) {
		return Util.getListItemView(getActivity(), convertView, item.getName());
	}

	public void updateAdapterView(View view, SqueezerArtist item) {
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.artist, quantity);
	}

}
