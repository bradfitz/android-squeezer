/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer;


import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerGenreListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerPlaylistsActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerYearListActivity;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class SqueezerMusicActivity extends ListActivity {
	private static final String TAG = SqueezerMusicActivity.class.getName();
	private static final int ARTISTS = 0;
	private static final int ALBUMS = 1;
	private static final int SONGS = 2;
	private static final int GENRES = 3;
	private static final int YEARS = 4;
	private static final int RANDOM_MIX = 5;
    private static final int MUSIC_FOLDER = -1; /* 6; */
    private static final int PLAYLISTS = 6;
    private static final int SEARCH = 7;

	private ISqueezeService service;
	private boolean canRandomplay = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setMusicMenu();
	}

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, SqueezeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + service);
    }

	private final ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
            setMusicMenu();
        }
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

	@Override
    public void onPause() {
        super.onPause();
        if (serviceConnection != null) {
        	unbindService(serviceConnection);
        }
    }

	private void setMusicMenu() {
		final String[] musicItems = getResources().getStringArray(R.array.music_items);
		final int[] musicIcons = new int[] { R.drawable.icon_ml_artist,
				R.drawable.icon_ml_albums, R.drawable.icon_ml_songs,
				R.drawable.icon_ml_genres, R.drawable.icon_ml_years,
                R.drawable.icon_ml_random, /* R.drawable.icon_ml_folder, */
				R.drawable.icon_ml_playlist, R.drawable.icon_ml_search };
		String[] items = musicItems;
		int[] icons = musicIcons;

		if (service != null) {
        	try {
				canRandomplay = service.canRandomplay();
			} catch (RemoteException e) {
				Log.e(TAG, "Error requesting randomplay ability: " + e);
			}
		}
		if (!canRandomplay) {
			items = new String[musicItems.length - 1];
			icons = new int[musicIcons.length -1];
			int j = 0;
			for (int i = 0; i < musicItems.length; i++) {
				if (i != RANDOM_MIX) {
					items[j] = musicItems[i];
					icons[j] = musicIcons[i];
					j++;
				}
			}

		}
		setListAdapter(new IconRowAdapter(this, items, icons));
		getListView().setOnItemClickListener(onMusicItemClick);
	}

	private final OnItemClickListener onMusicItemClick = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (!canRandomplay && position >= RANDOM_MIX) position += 1;
			switch (position) {
			case ARTISTS:
				SqueezerArtistListActivity.show(SqueezerMusicActivity.this);
				break;
			case ALBUMS:
				SqueezerAlbumListActivity.show(SqueezerMusicActivity.this);
				break;
			case SONGS:
				SqueezerSongListActivity.show(SqueezerMusicActivity.this);
				break;
			case GENRES:
				SqueezerGenreListActivity.show(SqueezerMusicActivity.this);
				break;
			case YEARS:
				SqueezerYearListActivity.show(SqueezerMusicActivity.this);
				break;
			case RANDOM_MIX:
				SqueezerRandomplayActivity.show(SqueezerMusicActivity.this);
				break;
			case MUSIC_FOLDER:
                /* TODO: Implement browsing the music folder. */
				break;
			case PLAYLISTS:
				SqueezerPlaylistsActivity.show(SqueezerMusicActivity.this);
				break;
			case SEARCH:
				SqueezerSearchActivity.show(SqueezerMusicActivity.this);
				break;
			}
		}
	};

	static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerMusicActivity.class);
        context.startActivity(intent);
    }

}
