package com.danga.squeezer.itemlists;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.model.SqueezerAlbum;

public class SqueezerAlbumView extends SqueezerIconicItemView<SqueezerAlbum> {
	private LayoutInflater layoutInflater;

	public SqueezerAlbumView(SqueezerBaseActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	public View getAdapterView(View convertView, SqueezerAlbum item) {
		ViewHolder viewHolder;
		
		if (convertView == null || convertView.getTag() == null) {
			convertView = layoutInflater.inflate(R.layout.icon_two_line_layout, null);
			viewHolder = new ViewHolder();
			viewHolder.label1 = (TextView) convertView.findViewById(R.id.text1);
			viewHolder.label2 = (TextView) convertView.findViewById(R.id.text2);
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (ViewHolder) convertView.getTag();

		viewHolder.label1.setText(item.getName());
		String text2 = "";
		if (item.getId() != null) {
			text2 = item.getArtist();
			if (item.getYear() != 0) text2 += " - " + item.getYear();
		}
		viewHolder.label2.setText(text2);
		updateAlbumArt(viewHolder.icon, item);

		return convertView;
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.album, quantity);
	}

    private static class ViewHolder {
		TextView label1;
		TextView label2;
		ImageView icon;
	}

}
