package com.danga.squeezer.itemlists;

import android.view.View;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.Util;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerYearView extends SqueezerBaseItemView<SqueezerYear> {

	public SqueezerYearView(SqueezerBaseActivity activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerYear item) {
		return Util.getListItemView(getActivity(), convertView, item.getId());
	}

	public void updateAdapterView(View view, SqueezerYear item) {
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.year, quantity);
	}

}
