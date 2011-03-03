package com.danga.squeezer.itemlists;

import java.util.List;

import android.os.RemoteException;

import com.danga.squeezer.SqueezerBaseListActivity;
import com.danga.squeezer.model.SqueezerSong;

public abstract class SqueezerAbstractSongListActivity extends SqueezerBaseListActivity<SqueezerSong> {

	public void registerCallback() throws RemoteException {
		getService().registerSongListCallback(songListCallback);
	}

	public void unregisterCallback() throws RemoteException {
		getService().unregisterSongListCallback(songListCallback);
	}

	private IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
		public void onSongsReceived(int count, int max, int start, List<SqueezerSong> items) throws RemoteException {
			onItemsReceived(count, max, start, items);
		}
    };

}