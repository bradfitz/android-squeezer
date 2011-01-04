package com.danga.squeezer.itemlists;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.model.SqueezerArtist;


public class SqueezerArtistView extends SqueezerBaseItemView<SqueezerArtist> {

	public SqueezerArtistView(Activity activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerArtist item) {
		TextView view;
		view = (TextView)(convertView != null && TextView.class.isAssignableFrom(convertView.getClass())
				? convertView
				: getActivity().getLayoutInflater().inflate(R.layout.list_item, null));
		view.setText((CharSequence) item.getName());
		return view;
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.artist, quantity);
	}

}
