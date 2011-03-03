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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
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
	private static final int DIALOG_SELECT_SORT_ORDER = 0;
	private static final int DIALOG_FILTER = 1;

	private String searchString;
	private SqueezerAlbum album;
	private SqueezerArtist artist;
	private SqueezerYear year;
	private SqueezerGenre genre;
	private Enum<SongsSortOrder> sortOrder = SongsSortOrder.title;

	private GenreSpinner genreSpinner;
	private YearSpinner yearSpinner;

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
		return new SqueezerSongView(this);
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
		SqueezerAlbumListActivity.show(this, item);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_sort:
			showDialog(DIALOG_SELECT_SORT_ORDER);
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
		case DIALOG_SELECT_SORT_ORDER:
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
	        ImageButton filterButton = (ImageButton) filterForm.findViewById(R.id.button_filter);
	        ImageButton cancelButton = (ImageButton) filterForm.findViewById(R.id.button_cancel);

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
	        
	        filterButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                	searchString = editText.getText().toString();
					genre = (SqueezerGenre) genreSpinnerView.getSelectedItem();
					year = (SqueezerYear) yearSpinnerView.getSelectedItem();
					orderItems();
					dismissDialog(DIALOG_FILTER);
				}
			});
	        cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dismissDialog(DIALOG_FILTER);
				}
			});
	        
			return builder.create();
        }
        return null;
    }

    public enum SongsSortOrder {
    	title,
    	tracknum;
    }

}