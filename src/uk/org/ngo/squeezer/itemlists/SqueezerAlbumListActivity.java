/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.itemlists;

import java.util.EnumSet;
import java.util.List;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.dialogs.AlbumViewDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerAlbumFilterDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.AlbumViewDialog.AlbumsSortOrder;
import uk.org.ngo.squeezer.itemlists.dialogs.AlbumViewDialog.AlbumListLayout;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerFilterMenuItemFragment;
import uk.org.ngo.squeezer.menu.SqueezerFilterMenuItemFragment.SqueezerFilterableListActivity;
import uk.org.ngo.squeezer.menu.ViewMenuItemFragment;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.model.SqueezerYear;
import uk.org.ngo.squeezer.util.ImageFetcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Lists albums, optionally filtered to match specific criteria.
 */
public class SqueezerAlbumListActivity extends SqueezerBaseListActivity<SqueezerAlbum>
        implements GenreSpinnerCallback, YearSpinnerCallback,
        SqueezerFilterableListActivity, ViewMenuItemFragment.ListActivityWithViewMenu {

    private AlbumsSortOrder sortOrder = null;
    private AlbumListLayout listLayout = null;

	private String searchString = null;
    public String getSearchString() { return searchString; }
    public void setSearchString(String searchString) { this.searchString = searchString; }

	private SqueezerSong song;
    public SqueezerSong getSong() { return song; }
    public void setSong(SqueezerSong song) { this.song = song; }

    private SqueezerArtist artist;
    public SqueezerArtist getArtist() { return artist; }
    public void setArtist(SqueezerArtist artist) { this.artist = artist; }

	private SqueezerYear year;
    @Override
    public SqueezerYear getYear() { return year; }
    @Override
    public void setYear(SqueezerYear year) { this.year = year; }

	private SqueezerGenre genre;
    @Override
    public SqueezerGenre getGenre() { return genre; }
    @Override
    public void setGenre(SqueezerGenre genre) { this.genre = genre; }

    private GenreSpinner genreSpinner;
    public void setGenreSpinner(Spinner spinner) {
        genreSpinner = new GenreSpinner(this, this, spinner);
    }

    private YearSpinner yearSpinner;
    public void setYearSpinner(Spinner spinner) {
        yearSpinner = new YearSpinner(this, this, spinner);
    }

    @Override
	public SqueezerItemView<SqueezerAlbum> createItemView() {
		return (listLayout == AlbumListLayout.grid) ? new AlbumGridView(this) : new SqueezerAlbumView(this);
	}

    @Override
    protected ImageFetcher createImageFetcher() {
        // Get an ImageFetcher to scale artwork to the size of the icon view.
        Resources resources = getResources();
        int height, width;
        if (listLayout == AlbumListLayout.grid) {
            height = resources.getDimensionPixelSize(R.dimen.album_art_icon_grid_height);
            width = resources.getDimensionPixelSize(R.dimen.album_art_icon_grid_width);
        } else {
            height = resources.getDimensionPixelSize(R.dimen.album_art_icon_height);
            width = resources.getDimensionPixelSize(R.dimen.album_art_icon_width);
        }
        ImageFetcher imageFetcher = new ImageFetcher(this, Math.max(height, width));
        imageFetcher.setLoadingImage(R.drawable.icon_pending_artwork);
        return imageFetcher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setListLayout();
        super.onCreate(savedInstanceState);

        MenuFragment.add(this, SqueezerFilterMenuItemFragment.class);
        MenuFragment.add(this, ViewMenuItemFragment.class);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                if (SqueezerArtist.class.getName().equals(key)) {
                    artist = extras.getParcelable(key);
                } else if (SqueezerYear.class.getName().equals(key)) {
                    year = extras.getParcelable(key);
                } else if (SqueezerGenre.class.getName().equals(key)) {
                    genre = extras.getParcelable(key);
                } else if (SqueezerSong.class.getName().equals(key)) {
                    song = extras.getParcelable(key);
                } else if (AlbumsSortOrder.class.getName().equals(key)) {
                    sortOrder = AlbumsSortOrder.valueOf(extras.getString(key));
                } else
                    Log.e(getTag(), "Unexpected extra value: " + key + "("
                            + extras.get(key).getClass().getName() + ")");
            }
        }

        TextView header = (TextView) findViewById(R.id.header);
        EnumSet<SqueezerAlbumView.Details> details = EnumSet.allOf(SqueezerAlbumView.Details.class);
        if (artist != null) {
            details.remove(SqueezerAlbumView.Details.ARTIST);
            header.setText(getString(R.string.albums_by_artist_header, artist.getName()));
            header.setVisibility(View.VISIBLE);
        }
        if (genre != null) {
            details.remove(SqueezerAlbumView.Details.GENRE);
            header.setText(getString(R.string.albums_by_genre_header, genre.getName()));
            header.setVisibility(View.VISIBLE);
        }
        if (year != null) {
            details.remove(SqueezerAlbumView.Details.YEAR);
            header.setText(getString(R.string.albums_by_year_header, year.getName()));
            header.setVisibility(View.VISIBLE);
        }
        ((SqueezerAlbumView) getItemView()).setDetails(details);
    }

    @Override
    protected int getContentView() {
        return (listLayout == AlbumListLayout.grid) ? R.layout.item_grid : R.layout.item_list_albums;
    }

    @Override
	protected void registerCallback() throws RemoteException {
		getService().registerAlbumListCallback(albumListCallback);
		if (genreSpinner != null) genreSpinner.registerCallback();
		if (yearSpinner != null) yearSpinner.registerCallback();
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterAlbumListCallback(albumListCallback);
		if (genreSpinner != null) genreSpinner.unregisterCallback();
		if (yearSpinner != null) yearSpinner.unregisterCallback();
	}

    @Override
    protected void orderPage(int start) throws RemoteException {
        if (sortOrder == null) {
            try {
                sortOrder = AlbumsSortOrder.valueOf(getService().preferredAlbumSort());
            } catch (IllegalArgumentException e) {
                Log.w(getTag(), "Unknown preferred album sort: " + e);
                sortOrder = AlbumsSortOrder.album;
            }
        }

        getService().albums(start, sortOrder.name().replace("__", ""), getSearchString(), artist,
                getYear(), getGenre(), song);
    }

    public AlbumsSortOrder getSortOrder() {
        return sortOrder;
    }

	public void setSortOrder(AlbumsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		getIntent().putExtra(AlbumsSortOrder.class.getName(), sortOrder.name());
		clearAndReOrderItems();
	}

    public AlbumListLayout getListLayout() {
        return listLayout;
    }

    /**
     * Set the preferred album list layout.
     * <p>
     * If the list layout is not selected, a default one is chosen, based on the
     * current screen size, on the assumption that the artwork grid is preferred
     * on larger screens.
     */
    private void setListLayout() {
        SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);
        String listLayoutString = preferences.getString(Preferences.KEY_ALBUM_LIST_LAYOUT, null);
        if (listLayoutString == null) {
            int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            listLayout = (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) ? AlbumListLayout.grid : AlbumListLayout.list;
        } else
            listLayout = AlbumListLayout.valueOf(listLayoutString);
    }

    public void setListLayout(AlbumListLayout listLayout) {
        SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Preferences.KEY_ALBUM_LIST_LAYOUT, listLayout.name());
        editor.commit();

        startActivity(getIntent());
        finish();
    }

    @Override
    public boolean onSearchRequested() {
        showFilterDialog();
        return false;
    }

    @Override
    public void showFilterDialog() {
        new SqueezerAlbumFilterDialog().show(getSupportFragmentManager(), "AlbumFilterDialog");
    }

    @Override
    public void showViewDialog() {
        new AlbumViewDialog().show(getSupportFragmentManager(), "AlbumOrderDialog");
    }

    public static void show(Context context, SqueezerItem... items) {
        show(context, null, items);
    }

    public static void show(Context context, AlbumsSortOrder sortOrder, SqueezerItem... items) {
        final Intent intent = new Intent(context, SqueezerAlbumListActivity.class);
        if (sortOrder != null)
            intent.putExtra(AlbumsSortOrder.class.getName(), sortOrder.name());
        for (SqueezerItem item: items)
            intent.putExtra(item.getClass().getName(), item);
        context.startActivity(intent);
    }

    private final IServiceAlbumListCallback albumListCallback = new IServiceAlbumListCallback.Stub() {
		@Override
        public void onAlbumsReceived(int count, int start, List<SqueezerAlbum> items) throws RemoteException {
		    onItemsReceived(count, start, items);
		}
    };

}
