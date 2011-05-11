package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.model.SqueezerGenre;

public class SqueezerGenreListActivity extends SqueezerBaseListActivity<SqueezerGenre>{

	@Override
	public SqueezerItemView<SqueezerGenre> createItemView() {
		return new SqueezerGenreView(this);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerGenreListCallback(genreListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterGenreListCallback(genreListCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().genres(start);
	}

    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerGenreListActivity.class);
        context.startActivity(intent);
    }

    private IServiceGenreListCallback genreListCallback = new IServiceGenreListCallback.Stub() {
		public void onGenresReceived(int count, int start, List<SqueezerGenre> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
