package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.SqueezerBasicListActivity;
import com.danga.squeezer.model.SqueezerGenre;

public class SqueezerGenreListActivity extends SqueezerBasicListActivity<SqueezerGenre>{

	public SqueezerItemView<SqueezerGenre> createItemView() {
		return new SqueezerGenreView(SqueezerGenreListActivity.this);
	}

	public void prepareActivity(Bundle extras) {
	}

	public void registerCallback() throws RemoteException {
		getService().registerGenreListCallback(genreListCallback);
	}

	public void unregisterCallback() throws RemoteException {
		getService().unregisterGenreListCallback(genreListCallback);
	}

	public void orderItems(int start) throws RemoteException {
		getService().genres(start);
	}

	public void onItemSelected(int index, SqueezerGenre item) throws RemoteException {
		SqueezerAlbumListActivity.show(this, item);
	}

    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerGenreListActivity.class);
        context.startActivity(intent);
    }

    private IServiceGenreListCallback genreListCallback = new IServiceGenreListCallback.Stub() {

		public void onGenresReceived(final int count, final int max, final int pos, final List<SqueezerGenre> genres) throws RemoteException {
			getUIThreadHandler().post(new Runnable() {
				public void run() {
					getItemListAdapter().update(count, max, pos, genres);
				}
			});
		}
    	
    };

}
