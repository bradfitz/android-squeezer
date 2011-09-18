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

package com.danga.squeezer.itemlists;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Spinner;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItem;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.framework.SqueezerOrderableListActivity;
import com.danga.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import com.danga.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerAlbumListActivity extends SqueezerOrderableListActivity<SqueezerAlbum>
		implements GenreSpinnerCallback, YearSpinnerCallback {

	private AlbumsSortOrder sortOrder = AlbumsSortOrder.album;
	private String searchString = null;
	private SqueezerArtist artist;
	private SqueezerYear year;
	private SqueezerGenre genre;

	private GenreSpinner genreSpinner;
	private YearSpinner yearSpinner;

	public SqueezerGenre getGenre() {
		return genre;
	}

	public SqueezerYear getYear() {
		return year;
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
		getService().albums(start, sortOrder.name().replace("__", ""), searchString, artist, year, genre);
	}

	public void setSortOrder(AlbumsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		orderItems();
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
		case DIALOG_ORDER:
		    String[] sortOrderStrings = new String[AlbumsSortOrder.values().length];
		    sortOrderStrings[AlbumsSortOrder.album.ordinal()] = getString(R.string.albums_sort_order_album);
		    sortOrderStrings[AlbumsSortOrder.artflow.ordinal()] = getString(R.string.albums_sort_order_artflow);
		    sortOrderStrings[AlbumsSortOrder.__new.ordinal()] = getString(R.string.albums_sort_order_new);
		    int checkedItem = sortOrder.ordinal();
		    builder.setTitle(getString(R.string.choose_sort_order, getItemAdapter().getQuantityString(2)));
		    builder.setSingleChoiceItems(sortOrderStrings, checkedItem, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int indexSelected) {
		               	setSortOrder(AlbumsSortOrder.values()[indexSelected]);
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

	        if (artist != null) {
	        	((EditText)filterForm.findViewById(R.id.artist)).setText(artist.getName());
	        	filterForm.findViewById(R.id.artist_view).setVisibility(View.VISIBLE);
	        }

	        genreSpinner = new GenreSpinner(this, this, genreSpinnerView);
	        yearSpinner = new YearSpinner(this, this, yearSpinnerView);

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

    private enum AlbumsSortOrder {
    	album,
    	artflow,
    	__new;
    }

}
