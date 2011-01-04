package com.danga.squeezer.itemlists;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseListActivity;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerSongView extends SqueezerIconicItemView<SqueezerSong> {
	private int rowLayout = R.layout.icon_two_line_layout;
	private int iconId = R.id.icon;
	private int text1Id = R.id.text1;
	private int text2Id = R.id.text2;
	
	public SqueezerSongView(SqueezerBaseListActivity<SqueezerSong> activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerSong item) {
		View row = getActivity().getLayoutInflater().inflate(rowLayout, null);

		TextView label1 = (TextView) row.findViewById(text1Id);
		label1.setText((CharSequence) item.getName());

		if (item.getId() != null) {
			TextView label2 = (TextView) row.findViewById(text2Id);
			label2.setText((CharSequence) (item.getYear() + " - " + item.getArtist()  + " - " + item.getAlbum()));
		}

		ImageView icon = (ImageView) row.findViewById(iconId);
		updateAlbumArt(icon, item.getArtwork_track_id());

		return (row);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.song, quantity);
	}

}
