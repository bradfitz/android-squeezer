package com.danga.squeezer.itemlists;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;

import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerCurrentPlaylistActivity extends SqueezerAbstractSongListActivity {

	public static void show(Context context) {
	    final Intent intent = new Intent(context, SqueezerCurrentPlaylistActivity.class);
	    context.startActivity(intent);
	}

	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(this) {
			@Override
			public void setupContextMenu(ContextMenu menu, SqueezerSong item) {
			}
		};
	}

	public void orderItems(int start) throws RemoteException {
		getService().currentPlaylist(start);
	}

	public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
		getService().playlistIndex(index);
		finish();
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	return false;
    }

}
