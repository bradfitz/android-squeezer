/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.danga.squeezer.itemlists;

import java.util.List;

import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.model.SqueezerPlugin;

public abstract class SqueezerPluginListActivity extends SqueezerBaseListActivity<SqueezerPlugin> {

	private final IServicePluginListCallback pluginListCallback = new IServicePluginListCallback.Stub() {
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