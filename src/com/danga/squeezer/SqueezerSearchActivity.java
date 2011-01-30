package com.danga.squeezer;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.itemlists.IServiceGenreListCallback;
import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.itemlists.SqueezerAlbumListActivity;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerSearchActivity extends SqueezerBaseActivity {
	private TextView loadingLabel;
	private ExpandableListView resultsExpandableListView;
	private SqueezerSearchAdapter searchResultsAdapter;
	private String searchString;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_layout);

		ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
		loadingLabel = (TextView) findViewById(R.id.loading_label);
		final EditText searchCriteriaText = (EditText) findViewById(R.id.search_input);

		searchResultsAdapter = new SqueezerSearchAdapter(this);
		resultsExpandableListView = (ExpandableListView) findViewById(R.id.search_expandable_list);
		resultsExpandableListView.setAdapter( searchResultsAdapter );

		searchCriteriaText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					doSearch(searchCriteriaText.getText().toString());
					return true;
				}
				return false;
			}
		});

        searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
            	if (getService() != null) {
            		doSearch(searchCriteriaText.getText().toString());
            	}
			}
		});

        resultsExpandableListView.setOnChildClickListener( new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
	    		SqueezerItem item = searchResultsAdapter.getChild(groupPosition, childPosition);
				if (item != null && item.getId() != null) {
					try {
					if (item instanceof SqueezerSong) {
						getService().playSong((SqueezerSong)item);
						SqueezerActivity.show(SqueezerSearchActivity.this);
					} else if (item instanceof SqueezerAlbum) {
						getService().playAlbum((SqueezerAlbum) item);
						SqueezerActivity.show(SqueezerSearchActivity.this);
					} else if (item instanceof SqueezerArtist) {
						SqueezerAlbumListActivity.show(SqueezerSearchActivity.this, item);
					} else if (item instanceof SqueezerGenre) {
						SqueezerAlbumListActivity.show(SqueezerSearchActivity.this, item);
					}
					} catch (RemoteException e) {
		                Log.e(getTag(), "Error from default action for search result '" + item + "': " + e);
					}
				}
				return true;
			}
		});

	};

	@Override
	protected void onServiceConnected() throws RemoteException {
		getService().registerArtistListCallback(artistsCallback);
		getService().registerAlbumListCallback(albumsCallback);
		getService().registerGenreListCallback(genresCallback);
		getService().registerSongListCallback(songsCallback);
		doSearch();
	}

	@Override
    public void onPause() {
        if (getService() != null) {
        	try {
        		getService().unregisterArtistListCallback(artistsCallback);
        		getService().unregisterAlbumListCallback(albumsCallback);
        		getService().unregisterGenreListCallback(genresCallback);
        		getService().unregisterSongListCallback(songsCallback);
			} catch (RemoteException e) {
                Log.e(getTag(), "Error unregistering list callback: " + e);
			}
        }
        super.onPause();
    }    
	
	static void show(Context context) {
		final Intent intent = new Intent(context, SqueezerSearchActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

	
	private void doSearch(int start) {
		try {
			getService().search(start, searchString);
		} catch (RemoteException e) {
			Log.e(getTag(), "Error performing search: " + e);
		}
	}

	private void doSearch(String searchString) {
		if (searchString != null && searchString.length() > 0 && getService() != null) {
			this.searchString = searchString;
			doSearch(0);
			resultsExpandableListView.setVisibility(View.GONE);
			loadingLabel.setVisibility(View.VISIBLE);
			searchResultsAdapter.clear();
		}
	}
	
	private void doSearch() {
		doSearch(searchString);
	}
	
	private <T extends SqueezerItem> void onItemsReceived(final Class<T> clazz, final int count, final int max, final int start, final List<T> items) {
		getUIThreadHandler().post(new Runnable() {
			public void run() {
				searchResultsAdapter.updateItems(clazz, count, max, start, items);
				loadingLabel.setVisibility(View.GONE);
				resultsExpandableListView.setVisibility(View.VISIBLE);
			}
		});
	}

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.searchmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	MenuItem fetchAll = menu.findItem(R.id.menu_item_fetch_all);
    	fetchAll.setVisible(!searchResultsAdapter.isFullyLoaded());
    	return true;
    }

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_home:
        	SqueezerHomeActivity.show(this);
			return true;
        case R.id.menu_item_main:
        	SqueezerActivity.show(this);
			return true;
        case R.id.menu_item_fetch_all:
			doSearch(searchResultsAdapter.getMaxCount());
			return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}

	
	private IServiceArtistListCallback artistsCallback = new IServiceArtistListCallback.Stub() {
		public void onArtistsReceived(int count, int max, int start, List<SqueezerArtist> items) throws RemoteException {
			onItemsReceived(SqueezerArtist.class, count, max, start, items);
		}
	};

	private IServiceAlbumListCallback albumsCallback = new IServiceAlbumListCallback.Stub() {
		public void onAlbumsReceived(int count, int max, int start, List<SqueezerAlbum> items) throws RemoteException {
			onItemsReceived(SqueezerAlbum.class, count, max, start, items);
		}
	};

	private IServiceGenreListCallback genresCallback = new IServiceGenreListCallback.Stub() {
		public void onGenresReceived(int count, int max, int start, List<SqueezerGenre> items) throws RemoteException {
			onItemsReceived(SqueezerGenre.class, count, max, start, items);
		}
	};

	private IServiceSongListCallback songsCallback = new IServiceSongListCallback.Stub() {
		public void onSongsReceived(int count, int max, int start, List<SqueezerSong> items) throws RemoteException {
			onItemsReceived(SqueezerSong.class, count, max, start, items);
		}
	};

}
