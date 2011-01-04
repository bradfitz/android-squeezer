package com.danga.squeezer.itemlists;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.model.SqueezerYear;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public class SqueezerYearView extends SqueezerBaseItemView<SqueezerYear> {

	public SqueezerYearView(Activity activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerYear item) {
		TextView view;
		view = (TextView)(convertView != null && TextView.class.isAssignableFrom(convertView.getClass())
				? convertView
				: getActivity().getLayoutInflater().inflate(R.layout.list_item, null));
		view.setText((CharSequence) item.getId());
		return view;
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.year, quantity);
	}

}
