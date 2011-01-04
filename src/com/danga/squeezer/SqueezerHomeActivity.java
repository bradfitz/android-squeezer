package com.danga.squeezer;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class SqueezerHomeActivity extends ListActivity {
	private static final int NOW_PLAYING = 0;
	private static final int MUSIC = 1;
	private static final int INTERNET_RADIO = 2;
	private static final int FAVORITES = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] values = getResources().getStringArray(R.array.home_items);
		int[] icons = new int[] { R.drawable.icon_nowplaying,
				R.drawable.icon_mymusic, R.drawable.icon_internet_radio,
				R.drawable.icon_favorites };
		setListAdapter(new IconRowAdapter(this, values, icons));

		getListView().setOnItemClickListener(onItemClick);
	}

	private OnItemClickListener onItemClick = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			switch (position) {
			case NOW_PLAYING:
				SqueezerActivity.show(SqueezerHomeActivity.this);
				break;
			case MUSIC:
				SqueezerMusicActivity.show(SqueezerHomeActivity.this);
				break;
			case INTERNET_RADIO:
				break;
			case FAVORITES:
				break;
			}
		}
	};

	static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerHomeActivity.class);
        context.startActivity(intent);
    }

}
