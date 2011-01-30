package com.danga.squeezer.itemlists;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.view.Menu;

import com.danga.squeezer.model.SqueezerSong;




public class SqueezerCurrentPlaylistActivity extends SqueezerSongListActivity {

	public static void show(Context context) {
	    final Intent intent = new Intent(context, SqueezerCurrentPlaylistActivity.class);
	    context.startActivity(intent);
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	return false;
    }

	@Override
	public void orderItems(int start) throws RemoteException {
		getService().currentPlaylist(start);
	}

	@Override
	public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
		getService().playlistIndex(index);
		finish();
	}

}
