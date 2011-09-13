package com.danga.squeezer.itemlists;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerYearListActivity extends SqueezerBaseListActivity<SqueezerYear>{

	@Override
	public SqueezerItemView<SqueezerYear> createItemView() {
		return new SqueezerYearView(this);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerYearListCallback(yearListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterYearListCallback(yearListCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().years(start);
	}

    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerYearListActivity.class);
        context.startActivity(intent);
    }

    private IServiceYearListCallback yearListCallback = new IServiceYearListCallback.Stub() {
		public void onYearsReceived(int count, int start, List<SqueezerYear> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
