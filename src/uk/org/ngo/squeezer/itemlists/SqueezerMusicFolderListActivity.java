/*
 * Copyright (c) 2012 Google Inc.
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
import uk.org.ngo.squeezer.model.SqueezerMusicFolder;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

public class SqueezerMusicFolderListActivity extends SqueezerBaseListActivity<SqueezerMusicFolder> {

    @Override
    public SqueezerItemView<SqueezerMusicFolder> createItemView() {
        return new SqueezerMusicFolderView(this);
    }

    @Override
    protected void registerCallback() throws RemoteException {
        getService().registerMusicFolderListCallback(musicFolderListCallback);
    }

    @Override
    protected void unregisterCallback() throws RemoteException {
        getService().unregisterMusicFolderListCallback(musicFolderListCallback);
    }

    @Override
    protected void orderPage(int start) throws RemoteException {
        getService().musicFolders(start);
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerMusicFolderListActivity.class);
        context.startActivity(intent);
    }

    public static void show(Context context, SqueezerMusicFolder item) {
        /** TODO: Start the activity showing a particular folder. */
    }

    private final IServiceMusicFolderListCallback musicFolderListCallback = new IServiceMusicFolderListCallback.Stub() {
        public void onMusicFoldersReceived(int count, int start, List<SqueezerMusicFolder> items)
                throws RemoteException {
            onItemsReceived(count, start, items);
        }
    };

}
