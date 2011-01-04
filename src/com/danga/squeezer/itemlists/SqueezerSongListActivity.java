package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.SqueezerBaseListActivity;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerSongListActivity extends SqueezerBaseListActivity<SqueezerSong> {

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
		getService().currentPlaylist(start);
	}

	public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
		getService().playlistIndex(index);
		finish();
	}
    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerSongListActivity.class);
        context.startActivity(intent);
    }

    private IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
    	
		public void onSongsReceived(final int count, final int max, final int pos, final List<SqueezerSong> albums) throws RemoteException {
			getUIThreadHandler().post(new Runnable() {
				public void run() {
					getItemListAdapter().update(count, max, pos, albums);
				}
			});
		}
    	
    };

}
