package com.danga.squeezer.itemlists;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.model.SqueezerPlugin;

public class SqueezerApplicationListActivity extends SqueezerPluginListActivity {

	@Override
	public SqueezerItemView<SqueezerPlugin> createItemView() {
		return new SqueezerApplicationView(this);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().apps(start);
	}

    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerApplicationListActivity.class);
        context.startActivity(intent);
    }

}
