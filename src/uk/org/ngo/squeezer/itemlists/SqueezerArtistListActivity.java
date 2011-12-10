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

import uk.org.ngo.squeezer.framework.SqueezerFilterableListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;

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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.IServiceArtistListCallback;

public class SqueezerArtistListActivity extends SqueezerFilterableListActivity<SqueezerArtist> implements GenreSpinnerCallback{
	private String searchString = null;
	private SqueezerAlbum album;
	private SqueezerGenre genre;

	private GenreSpinner genreSpinner;

	public SqueezerGenre getGenre() {
		return genre;
	}

	@Override
	public SqueezerItemView<SqueezerArtist> createItemView() {
		return new SqueezerArtistView(this);
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerAlbum.class.getName().equals(key)) {
					album = extras.getParcelable(key);
				} else if (SqueezerGenre.class.getName().equals(key)) {
					genre = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerArtistListCallback(artistsListCallback);
		if (genreSpinner != null) genreSpinner.registerCallback();
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterArtistListCallback(artistsListCallback);
		if (genreSpinner != null) genreSpinner.unregisterCallback();
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().artists(start, searchString, album, genre);
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
		case DIALOG_FILTER:

			View filterForm = getLayoutInflater().inflate(R.layout.filter_dialog, null);
			builder.setTitle(R.string.menu_item_filter);
			builder.setView(filterForm);

	        final EditText editText = (EditText) filterForm.findViewById(R.id.search_string);
	        editText.setHint(getString(R.string.filter_text_hint, getItemAdapter().getQuantityString(2)));
			filterForm.findViewById(R.id.year_view).setVisibility(View.GONE);
			final Spinner genreSpinnerView = (Spinner) filterForm.findViewById(R.id.genre_spinner);
	        genreSpinner = new GenreSpinner(this, this, genreSpinnerView);

	        if (album != null) {
	        	((EditText)filterForm.findViewById(R.id.album)).setText(album.getName());
	        	(filterForm.findViewById(R.id.album_view)).setVisibility(View.VISIBLE);
	        }

	        editText.setOnKeyListener(new OnKeyListener() {
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                	searchString = editText.getText().toString();
						genre = (SqueezerGenre) genreSpinnerView.getSelectedItem();
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
					orderItems();
				}
			});
	        builder.setNegativeButton(android.R.string.cancel, null);

			return builder.create();
        }
        return null;
    }

	public static void show(Context context, SqueezerItem... items) {
        final Intent intent = new Intent(context, SqueezerArtistListActivity.class);
        for (SqueezerItem item: items)
        	intent.putExtra(item.getClass().getName(), item);
        context.startActivity(intent);
    }

    private final IServiceArtistListCallback artistsListCallback = new IServiceArtistListCallback.Stub() {
		public void onArtistsReceived(int count, int start, List<SqueezerArtist> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
