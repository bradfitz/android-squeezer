package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.model.SqueezerPlayer;

public class SqueezerPlayerListActivity extends SqueezerBaseListActivity<SqueezerPlayer> {

	@Override
	public SqueezerItemView<SqueezerPlayer> createItemView() {
		return new SqueezerPlayerView(this);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerPlayerListCallback(playerListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterPlayerListCallback(playerListCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().players(start);
	}

	@Override
	protected void onItemSelected(int index, SqueezerPlayer item) throws RemoteException {
		getService().setActivePlayer(item);
		finish();
	};
	
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlayerListActivity.class);
        context.startActivity(intent);
    }

    private IServicePlayerListCallback playerListCallback = new IServicePlayerListCallback.Stub() {
		public void onPlayersReceived(int count, int start, List<SqueezerPlayer> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
