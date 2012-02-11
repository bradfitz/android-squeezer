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

import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.framework.SqueezerBaseActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerGenreListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerMusicFolderListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerPlaylistsActivity;
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

public class SqueezerMusicActivity extends SqueezerBaseActivity {
	private static final int ARTISTS = 0;
	private static final int ALBUMS = 1;
	private static final int SONGS = 2;
	private static final int GENRES = 3;
	private static final int YEARS = 4;
    private static final int MUSIC_FOLDER = 5;
    private static final int RANDOM_MIX = 6;
    private static final int PLAYLISTS = 7;
    private static final int SEARCH = 8;

    private boolean mCanMusicfolder = false;
    private boolean mCanRandomplay = false;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);
        MenuFragment.add(this, SqueezerMenuFragment.class);
        setMusicMenu();
    }

    @Override
    protected void onServiceConnected() throws RemoteException {
        setMusicMenu();
    }

	private void setMusicMenu() {
        final String[] musicItems = getResources().getStringArray(R.array.music_items);
        final int[] musicIcons = new int[] {
                R.drawable.ic_artists, R.drawable.ic_albums, R.drawable.ic_songs,
                R.drawable.ic_genres, R.drawable.ic_years, R.drawable.ic_music_folder,
                R.drawable.ic_random, R.drawable.ic_playlists, R.drawable.ic_search
        };

        if (getService() != null) {
            try {
                mCanMusicfolder = getService().canMusicfolder();
            } catch (RemoteException e) {
                Log.e(getTag(), "Error requesting musicfolder ability: " + e);
            }
        }

		if (getService() != null) {
        	try {
				mCanRandomplay = getService().canRandomplay();
			} catch (RemoteException e) {
				Log.e(getTag(), "Error requesting randomplay ability: " + e);
			}
		}

        List<IconRowAdapter.IconRow> rows = new ArrayList<IconRowAdapter.IconRow>();
        for (int i = ARTISTS; i <= SEARCH; i++) {
            if (i == MUSIC_FOLDER && !mCanMusicfolder)
                continue;

            if (i == RANDOM_MIX && !mCanRandomplay)
                continue;

            rows.add(new IconRowAdapter.IconRow(i, musicItems[i], musicIcons[i]));
        }

        listView.setAdapter(new IconRowAdapter(this, rows));
		listView.setOnItemClickListener(onMusicItemClick);
	}

	private final OnItemClickListener onMusicItemClick = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch ((int) id) {
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
                case MUSIC_FOLDER:
                    SqueezerMusicFolderListActivity.show(SqueezerMusicActivity.this);
                    break;
                case RANDOM_MIX:
				SqueezerRandomplayActivity.show(SqueezerMusicActivity.this);
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
