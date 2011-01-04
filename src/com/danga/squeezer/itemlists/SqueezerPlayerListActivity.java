package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.SqueezerBasicListActivity;
import com.danga.squeezer.model.SqueezerPlayer;

public class SqueezerPlayerListActivity extends SqueezerBasicListActivity<SqueezerPlayer> {

	public SqueezerItemView<SqueezerPlayer> createItemView() {
		return new SqueezerPlayerView(SqueezerPlayerListActivity.this);
	}

	public void prepareActivity(Bundle extras) {
	}

	public void registerCallback() throws RemoteException {
		getService().registerPlayerListCallback(playerListCallback);
	}

	public void unregisterCallback() throws RemoteException {
		getService().unregisterPlayerListCallback(playerListCallback);
	}

	public void orderItems(int start) throws RemoteException {
		getService().players(start);
	}

	public void onItemSelected(int index, SqueezerPlayer item) throws RemoteException {
		getService().setActivePlayer(item);
		finish();
	};
	
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlayerListActivity.class);
        context.startActivity(intent);
    }

    private IServicePlayerListCallback playerListCallback = new IServicePlayerListCallback.Stub() {
    	
		public void onPlayersReceived(final int count, final int max, final int pos, final List<SqueezerPlayer> players) throws RemoteException {
			getUIThreadHandler().post(new Runnable() {
				public void run() {
					getItemListAdapter().update(count, max, pos, players);
				}
			});
		}
    	
    };

}
