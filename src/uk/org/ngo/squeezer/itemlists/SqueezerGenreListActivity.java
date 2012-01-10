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

package uk.org.ngo.squeezer.itemlists;

import java.util.List;

import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.model.SqueezerGenre;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import uk.org.ngo.squeezer.itemlists.IServiceGenreListCallback;

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

    private final IServiceGenreListCallback genreListCallback = new IServiceGenreListCallback.Stub() {
		public void onGenresReceived(int count, int start, List<SqueezerGenre> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
