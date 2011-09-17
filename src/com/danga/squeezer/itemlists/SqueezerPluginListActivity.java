package com.danga.squeezer.itemlists;

import java.util.List;

import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.model.SqueezerPlugin;

public abstract class SqueezerPluginListActivity extends SqueezerBaseListActivity<SqueezerPlugin> {

	private IServicePluginListCallback pluginListCallback = new IServicePluginListCallback.Stub() {
			public void onPluginsReceived(int count, int start, List<SqueezerPlugin> items) throws RemoteException {
				onItemsReceived(count, start, items);
			}
	    };

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerPluginListCallback(pluginListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterPluginListCallback(pluginListCallback);
	}

}