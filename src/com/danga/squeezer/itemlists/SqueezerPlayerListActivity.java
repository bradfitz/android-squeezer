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

	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlayerListActivity.class);
        context.startActivity(intent);
    }

    private final IServicePlayerListCallback playerListCallback = new IServicePlayerListCallback.Stub() {
		public void onPlayersReceived(int count, int start, List<SqueezerPlayer> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
