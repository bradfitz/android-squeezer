package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.danga.squeezer.SqueezerBasicListActivity;
import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerYearListActivity extends SqueezerBasicListActivity<SqueezerYear>{

	public SqueezerItemView<SqueezerYear> createItemView() {
		return new SqueezerYearView(this);
	}

	public void registerCallback() throws RemoteException {
		getService().registerYearListCallback(yearListCallback);
	}

	public void unregisterCallback() throws RemoteException {
		getService().unregisterYearListCallback(yearListCallback);
	}

	public void orderItems(int start) throws RemoteException {
		getService().years(start);
	}

	public void onItemSelected(int index, SqueezerYear item) throws RemoteException {
		SqueezerAlbumListActivity.show(this, item);
	}

    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerYearListActivity.class);
        context.startActivity(intent);
    }

    private IServiceYearListCallback yearListCallback = new IServiceYearListCallback.Stub() {
		public void onYearsReceived(int count, int max, int start, List<SqueezerYear> items) throws RemoteException {
			onItemsReceived(count, max, start, items);
		}
    };

}
