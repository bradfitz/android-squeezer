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

import org.acra.ErrorReporter;

import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;

public abstract class SqueezerAbstractSongListActivity extends SqueezerBaseListActivity<SqueezerSong> {

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerSongListCallback(songListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterSongListCallback(songListCallback);
	}

	private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
		public void onSongsReceived(int count, int start, List<SqueezerSong> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

    /**
     * Attempts to download the song given by songId.
     * 
     * @param songId ID of the song to download
     */
    public void downloadSong(String songId) {
        /*
         * Quick-and-dirty version. Use ACTION_VIEW to have something try and
         * download the song (probably the browser).
         * 
         * TODO: If running on Gingerbread or greater use the Download Manager
         * APIs to have more control over the download.
         */
        try {
            String url = getService().getSongDownloadUrl(songId);

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (RemoteException e) {
            ErrorReporter.getInstance().handleException(e);
            e.printStackTrace();
        }
    }
}