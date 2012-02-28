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
    private final String TAG = "SqueezerHomeActivity";

    private static final int ARTISTS = 0;
    private static final int ALBUMS = 1;
    private static final int SONGS = 2;
    private static final int GENRES = 3;
    private static final int YEARS = 4;
    private static final int MUSIC_FOLDER = 5;
    private static final int RANDOM_MIX = 6;
    private static final int PLAYLISTS = 7;
    private static final int INTERNET_RADIO = 8;
    private static final int APPS = 9;
    private static final int FAVORITES = 10;

    private boolean mCanMusicfolder = false;
    private boolean mCanRandomplay = false;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);
        MenuFragment.add(this, SqueezerMenuFragment.class);
    }

    @Override
    protected void onServiceConnected() throws RemoteException {
        getService().registerCallback(mCallback);
    }


    private final IServiceCallback mCallback = new IServiceCallback.Stub() {
        public void onConnectionChanged(boolean isConnected, boolean postConnect, boolean loginFailure)
                throws RemoteException {
            // XXX: The UI needs to change at this point, since none of the
            // options are valid if we're not connected.
        }

        /**
         * Sets the menu after handshaking with the SqueezeServer has completed.
         * <p>
         * This is necessary because the service doesn't know whether the server
         * supports music folder browsing and random play ability until the
         * handshake completes, and the menu is adjusted depending on whether or
         * not those abilities exist.
         */
        public void onHandshakeCompleted() throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    createListItems();
                }
            });
        }

        // Nothing to do when these events happen.

        public void onMusicChanged() throws RemoteException {
        }

        public void onPlayerChanged(String playerId, String playerName) throws RemoteException {
        }

        public void onPlayStatusChanged(boolean isPlaying) throws RemoteException {
        }

        public void onTimeInSongChange(int secondsIn, int secondsTotal) throws RemoteException {
        }

        public void onPowerStatusChanged() throws RemoteException {
        }
    };

    /**
     * Creates the list of items to show in the activity.
     * <p>
     * Must be run on the UI thread.
     */
    private void createListItems() {
        int[] icons = new int[] {
                R.drawable.ic_artists,
                R.drawable.ic_albums, R.drawable.ic_songs,
                R.drawable.ic_genres, R.drawable.ic_years,
                R.drawable.ic_music_folder, R.drawable.ic_random,
                R.drawable.ic_playlists, R.drawable.ic_internet_radio,
                R.drawable.ic_my_apps, R.drawable.ic_favorites
        };

        String[] items = getResources().getStringArray(R.array.home_items);;

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
        for (int i = ARTISTS; i <= INTERNET_RADIO; i++) { // APPS & FAVORITES
                                                          // not implemented
            if (i == MUSIC_FOLDER && !mCanMusicfolder)
                continue;

            if (i == RANDOM_MIX && !mCanRandomplay)
                continue;

            rows.add(new IconRowAdapter.IconRow(i, items[i], icons[i]));
        }

        listView.setAdapter(new IconRowAdapter(this, rows));
		listView.setOnItemClickListener(onHomeItemClick);
	}

	private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            switch ((int) id) {
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
                case MUSIC_FOLDER:
                    SqueezerMusicFolderListActivity.show(SqueezerHomeActivity.this);
                    break;
                case RANDOM_MIX:
                    SqueezerRandomplayActivity.show(SqueezerHomeActivity.this);
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

    @Override
    public void onPause() {
        if (getService() != null) {
            try {
                getService().unregisterCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Service exception in onPause(): " + e);
            }
        }

        super.onPause();
    }

	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerHomeActivity.class)
        		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        		.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
