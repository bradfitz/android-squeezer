package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.danga.squeezer.SqueezerActivity;
import com.danga.squeezer.SqueezerBaseListActivity;
import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerYear;

public abstract class SqueezerSongListActivity extends SqueezerBaseListActivity<SqueezerSong> {
	private String searchString;
	private SqueezerAlbum album;
	private SqueezerArtist artist;
	private SqueezerYear year;
	private SqueezerGenre genre;
	private Enum<SongsSortOrder> sortOrder = SongsSortOrder.title;

	public static void show(Context context) {
	    final Intent intent = new Intent(context, SqueezerSongListActivity.class);
	    context.startActivity(intent);
	}


	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(SqueezerSongListActivity.this);
	}

	public void prepareActivity(Bundle extras) {
	}

	public void registerCallback() throws RemoteException {
		getService().registerSongListCallback(songListCallback);
	}

	public void unregisterCallback() throws RemoteException {
		getService().unregisterSongListCallback(songListCallback);
	}

	public void orderItems(int start) throws RemoteException {
		getService().songs(start, sortOrder.name(), searchString, album, artist, year, genre);
	}

	public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
		getService().playSong(item);
		SqueezerActivity.show(this);
	}

	private IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
		public void onSongsReceived(int count, int max, int start, List<SqueezerSong> items) throws RemoteException {
			onItemsReceived(count, max, start, items);
		}
    };

    public enum SongsSortOrder {
    	title,
    	tracknum;
    }

}