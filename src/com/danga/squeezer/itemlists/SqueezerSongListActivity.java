package com.danga.squeezer.itemlists;

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

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerItem;
import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import com.danga.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerYear;

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
	public void registerCallback() throws RemoteException {
		super.registerCallback();
		if (genreSpinner != null) genreSpinner.registerCallback();
		if (yearSpinner != null) yearSpinner.registerCallback();
	}

	@Override
	public void unregisterCallback() throws RemoteException {
		super.registerCallback();
		if (genreSpinner != null) genreSpinner.unregisterCallback();
		if (yearSpinner != null) yearSpinner.unregisterCallback();
	}

	public void orderItems(int start) throws RemoteException {
		getService().songs(start, sortOrder.name(), searchString, album, artist, year, genre);
	}
	
	public void setSortOrder(SongsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		orderItems();
	}

	public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
		insert(item);
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
		    builder.setTitle(getString(R.string.choose_sort_order, getItemListAdapter().getQuantityString(2)));
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
	        editText.setHint(getString(R.string.filter_text_hint, getItemListAdapter().getQuantityString(2)));
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