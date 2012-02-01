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


import uk.org.ngo.squeezer.framework.SqueezerBaseActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerGenreListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerPlaylistsActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerRadioListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerYearListActivity;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerMenuFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class SqueezerHomeActivity extends SqueezerBaseActivity {
    private static final int ARTISTS = 0;
    private static final int ALBUMS = 1;
    private static final int SONGS = 2;
    private static final int GENRES = 3;
    private static final int YEARS = 4;
    private static final int RANDOM_MIX = 5;
    private static final int MUSIC_FOLDER = -1; /* 6; */
    private static final int PLAYLISTS = 6;
    private static final int INTERNET_RADIO = 7;
    private static final int APPS = 8;
    private static final int FAVORITES = 9;

    private boolean canRandomplay = true;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);
        MenuFragment.add(this, SqueezerMenuFragment.class);
        setHomeMenu();
    }

    @Override
    protected void onServiceConnected() throws RemoteException {
    }

	private void setHomeMenu() {
        int[] icons = new int[] { R.drawable.icon_ml_artist,
                R.drawable.icon_ml_albums, R.drawable.icon_ml_songs,
                R.drawable.icon_ml_genres, R.drawable.icon_ml_years,
                R.drawable.icon_ml_random, /* R.drawable.icon_ml_folder, */
                R.drawable.icon_ml_playlist, R.drawable.icon_internet_radio,
                R.drawable.icon_my_apps, R.drawable.icon_favorites
        };

        String[] items = getResources().getStringArray(R.array.home_items);;

        if (getService() != null) {
            try {
                canRandomplay = getService().canRandomplay();
            } catch (RemoteException e) {
                Log.e(getTag(), "Error requesting randomplay ability: " + e);
            }
        }
        if (!canRandomplay) {
            items = new String[items.length - 1];
            icons = new int[icons.length - 1];
            int j = 0;
            for (int i = 0; i < items.length; i++) {
                if (i != RANDOM_MIX) {
                    items[j] = items[i];
                    icons[j] = icons[i];
                    j++;
                }
            }

        }

		listView.setAdapter(new IconRowAdapter(this, getResources().getStringArray(R.array.home_items), icons));
		listView.setOnItemClickListener(onHomeItemClick);
	}

	private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!canRandomplay && position >= RANDOM_MIX)
                position += 1;

            switch (position) {
                case ARTISTS:
                    SqueezerArtistListActivity.show(SqueezerHomeActivity.this);
                    break;
                case ALBUMS:
                    SqueezerAlbumListActivity.show(SqueezerHomeActivity.this);
                    break;
                case SONGS:
                    SqueezerSongListActivity.show(SqueezerHomeActivity.this);
                    break;
                case GENRES:
                    SqueezerGenreListActivity.show(SqueezerHomeActivity.this);
                    break;
                case YEARS:
                    SqueezerYearListActivity.show(SqueezerHomeActivity.this);
                    break;
                case RANDOM_MIX:
                    SqueezerRandomplayActivity.show(SqueezerHomeActivity.this);
                    break;
                case MUSIC_FOLDER:
                    /* TODO: Implement browsing the music folder. */
                    break;
                case PLAYLISTS:
                    SqueezerPlaylistsActivity.show(SqueezerHomeActivity.this);
                    break;
                case INTERNET_RADIO:
                    // Uncomment these next two lines as an easy way to check
                    // crash reporting functionality.

                    // String sCrashString = null;
                    // Log.e("MyApp", sCrashString.toString());
                    SqueezerRadioListActivity.show(SqueezerHomeActivity.this);
                    break;
                case APPS:
                    // TODO (kaa) implement
                    // Currently hidden, by commenting out the entry in
                    // strings.xml.
                    // SqueezerApplicationListActivity.show(SqueezerHomeActivity.this);
                    break;
                case FAVORITES:
                    // Currently hidden, by commenting out the entry in
                    // strings.xml.
                    // TODO: Implement
                    break;
			}
		}
	};

	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerHomeActivity.class)
        		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        		.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
