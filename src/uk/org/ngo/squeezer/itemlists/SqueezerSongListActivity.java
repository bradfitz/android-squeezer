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

import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.model.SqueezerYear;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Spinner;

import uk.org.ngo.squeezer.R;

public class SqueezerSongListActivity extends SqueezerAbstractSongListActivity implements GenreSpinnerCallback, YearSpinnerCallback {
	private String searchString;
	private SqueezerAlbum album;
	private SqueezerArtist artist;
	private SqueezerYear year;
	private SqueezerGenre genre;
	private Enum<SongsSortOrder> sortOrder = SongsSortOrder.title;

	private GenreSpinner genreSpinner;
	private YearSpinner yearSpinner;
	private SqueezerSongView songViewLogic;

	public SqueezerGenre getGenre() {
		return genre;
	}

	public SqueezerYear getYear() {
		return year;
	}

	public static void show(Context context, SqueezerItem... items) {
	    final Intent intent = new Intent(context, SqueezerSongListActivity.class);
        for (SqueezerItem item: items)
        	intent.putExtra(item.getClass().getName(), item);
	    context.startActivity(intent);
	}


	@Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		songViewLogic = new SqueezerSongView(this);
		return songViewLogic;
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerAlbum.class.getName().equals(key)) {
					album = extras.getParcelable(key);
					sortOrder = SongsSortOrder.tracknum;
				} else if (SqueezerArtist.class.getName().equals(key)) {
					artist = extras.getParcelable(key);
				} else if (SqueezerYear.class.getName().equals(key)) {
					year = extras.getParcelable(key);
				} else if (SqueezerGenre.class.getName().equals(key)) {
					genre = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
		songViewLogic.setBrowseByAlbum(album != null);
		songViewLogic.setBrowseByArtist(artist != null);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		super.registerCallback();
		if (genreSpinner != null) genreSpinner.registerCallback();
		if (yearSpinner != null) yearSpinner.registerCallback();
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		super.registerCallback();
		if (genreSpinner != null) genreSpinner.unregisterCallback();
		if (yearSpinner != null) yearSpinner.unregisterCallback();
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().songs(start, sortOrder.name(), searchString, album, artist, year, genre);
	}

	public void setSortOrder(SongsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		orderItems();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filtermenuitem, menu);
        getMenuInflater().inflate(R.menu.ordermenuitem, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_sort:
			showDialog(DIALOG_ORDER);
			return true;
		case R.id.menu_item_filter:
			showDialog(DIALOG_FILTER);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onSearchRequested() {
		showDialog(DIALOG_FILTER);
		return false;
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
		case DIALOG_ORDER:
		    String[] sortOrderStrings = new String[SongsSortOrder.values().length];
		    sortOrderStrings[SongsSortOrder.title.ordinal()] = getString(R.string.songs_sort_order_title);
		    sortOrderStrings[SongsSortOrder.tracknum.ordinal()] = getString(R.string.songs_sort_order_tracknum);
		    int checkedItem = sortOrder.ordinal();
		    builder.setTitle(getString(R.string.choose_sort_order, getItemAdapter().getQuantityString(2)));
		    builder.setSingleChoiceItems(sortOrderStrings, checkedItem, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int indexSelected) {
		               	setSortOrder(SongsSortOrder.values()[indexSelected]);
		                dialog.dismiss();
		            }
		        });
		    return builder.create();
		case DIALOG_FILTER:
			View filterForm = getLayoutInflater().inflate(R.layout.filter_dialog, null);
			builder.setTitle(R.string.menu_item_filter);
			builder.setView(filterForm);
	        final EditText editText = (EditText) filterForm.findViewById(R.id.search_string);
	        editText.setHint(getString(R.string.filter_text_hint, getItemAdapter().getQuantityString(2)));
			final Spinner genreSpinnerView = (Spinner) filterForm.findViewById(R.id.genre_spinner);
			final Spinner yearSpinnerView = (Spinner) filterForm.findViewById(R.id.year_spinner);

	        genreSpinner = new GenreSpinner(this, this, genreSpinnerView);
	        yearSpinner = new YearSpinner(this, this, yearSpinnerView);

	        if (artist != null) {
	        	((EditText)filterForm.findViewById(R.id.artist)).setText(artist.getName());
	        	(filterForm.findViewById(R.id.artist_view)).setVisibility(View.VISIBLE);
	        }
	        if (album != null) {
	        	((EditText) filterForm.findViewById(R.id.album)).setText(album.getName());
	        	(filterForm.findViewById(R.id.album_view)).setVisibility(View.VISIBLE);
	        }

	        editText.setOnKeyListener(new OnKeyListener() {
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                	searchString = editText.getText().toString();
						genre = (SqueezerGenre) genreSpinnerView.getSelectedItem();
						year = (SqueezerYear) yearSpinnerView.getSelectedItem();
						orderItems();
						dismissDialog(DIALOG_FILTER);
						return true;
	                }
	                return false;
	            }
	        });

	        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
                	searchString = editText.getText().toString();
					genre = (SqueezerGenre) genreSpinnerView.getSelectedItem();
					year = (SqueezerYear) yearSpinnerView.getSelectedItem();
					orderItems();
				}
			});
	        builder.setNegativeButton(android.R.string.cancel, null);

			return builder.create();
        }
        return null;
    }

    public enum SongsSortOrder {
    	title,
    	tracknum;
    }

}