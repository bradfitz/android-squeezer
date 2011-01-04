package com.danga.squeezer;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class SqueezerMusicActivity extends ListActivity {
	private static final int ARTISTS = 0;
	private static final int ALBUMS = 1;
	private static final int GENRES = 2;
	private static final int YEARS = 3;
	private static final int NEW_MUSIC = 4;
	private static final int RANDOM_MIX = 5;
	private static final int MUSIC_FOLDER = 6;
	private static final int PLAYLISTS = 7;
	private static final int SEARCH = 8;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] values = getResources().getStringArray(R.array.music_items);
		int[] icons = new int[] { R.drawable.icon_ml_artist,
				R.drawable.icon_ml_albums, R.drawable.icon_ml_genres,
				R.drawable.icon_ml_years, R.drawable.icon_ml_new_music,
				R.drawable.icon_ml_random, R.drawable.icon_ml_folder,
				R.drawable.icon_ml_playlist, R.drawable.icon_ml_search };
		setListAdapter(new IconRowAdapter(this, values, icons));

		getListView().setOnItemClickListener(onItemClick);
	}

	private OnItemClickListener onItemClick = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			switch (position) {
			case ARTISTS:
				SqueezerArtistListActivity.show(SqueezerMusicActivity.this);
				break;
			case ALBUMS:
				SqueezerAlbumListActivity.show(SqueezerMusicActivity.this);
				break;
			case GENRES:
				break;
			case YEARS:
				break;
			case NEW_MUSIC:
				break;
			case RANDOM_MIX:
				SqueezerRandomMixActivity.show(SqueezerMusicActivity.this);
				break;
			case MUSIC_FOLDER:
				break;
			case PLAYLISTS:
				break;
			case SEARCH:
				break;
			}
		}
	};

	static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerMusicActivity.class);
        context.startActivity(intent);
    }

}
