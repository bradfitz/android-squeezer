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

import java.util.List;

import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.framework.SqueezerOrderableListActivity;
import uk.org.ngo.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerAlbumFilterDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerAlbumOrderDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerAlbumOrderDialog.AlbumsSortOrder;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.model.SqueezerYear;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;


public class SqueezerAlbumListActivity extends SqueezerOrderableListActivity<SqueezerAlbum>
		implements GenreSpinnerCallback, YearSpinnerCallback {

	private AlbumsSortOrder sortOrder = AlbumsSortOrder.album;

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
    public SqueezerYear getYear() { return year; }
    public void setYear(SqueezerYear year) { this.year = year; }

	private SqueezerGenre genre;
    public SqueezerGenre getGenre() { return genre; }
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
		return new SqueezerAlbumView(this);
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerArtist.class.getName().equals(key)) {
					artist = extras.getParcelable(key);
				} else if (SqueezerYear.class.getName().equals(key)) {
					year = extras.getParcelable(key);
                } else if (SqueezerGenre.class.getName().equals(key)) {
                    genre = extras.getParcelable(key);
                } else if (SqueezerSong.class.getName().equals(key)) {
                    song = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
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
		getService().albums(start, sortOrder.name().replace("__", ""), getSearchString(), artist, getYear(), getGenre(), song);
	}

    public AlbumsSortOrder getSortOrder() {
        return sortOrder;
    }

	public void setSortOrder(AlbumsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		orderItems();
	}
	
	public void showFilterDialog() {
        new SqueezerAlbumFilterDialog().show(getSupportFragmentManager(), "AlbumFilterDialog");
	}

	public void showOrderDialog() {
	    new SqueezerAlbumOrderDialog().show(getSupportFragmentManager(), "AlbumOrderDialog");
	}

    public static void show(Context context, SqueezerItem... items) {
        final Intent intent = new Intent(context, SqueezerAlbumListActivity.class);
        for (SqueezerItem item: items)
        	intent.putExtra(item.getClass().getName(), item);
        context.startActivity(intent);
    }

    private final IServiceAlbumListCallback albumListCallback = new IServiceAlbumListCallback.Stub() {
		public void onAlbumsReceived(int count, int start, List<SqueezerAlbum> items) throws RemoteException {
		    onItemsReceived(count, start, items);
		}
    };

}

