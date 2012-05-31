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


import uk.org.ngo.squeezer.framework.SqueezerArtworkItem;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.service.ISqueezeService;

/**
 * Represents the view hierarchy for a single {@link SqueezerItem} subclass.
 * where the item has track artwork associated with it.
 * 
 * @param <T>
 */
public abstract class SqueezerAlbumArtView<T extends SqueezerItem> extends SqueezerIconicItemView<T> {
	public SqueezerAlbumArtView(SqueezerItemListActivity activity) {
		super(activity);
	}

	/**
	 * Fetches artwork associated with a given item and attaches it to
	 * an image.
	 * 
	 * @param icon the image to update
	 * @param item the item that contains artwork
	 */
	protected void setIconToTrackArtwork(final ImageView icon, final SqueezerArtworkItem item) {
		icon.setImageResource(R.drawable.icon_album_noart);
		downloadUrlToImageView(getAlbumArtUrl(item.getArtwork_track_id()), icon);
	}

	private String getAlbumArtUrl(String artwork_track_id) {
		if (artwork_track_id == null)
			return null;

		ISqueezeService service = getActivity().getService();
		if (service == null)
			return null;

		try {
			return service.getAlbumArtUrl(artwork_track_id);
		} catch (RemoteException e) {
			Log.e(getClass().getSimpleName(), "Error requesting album art url: " + e);
			return null;
		}
	}

}