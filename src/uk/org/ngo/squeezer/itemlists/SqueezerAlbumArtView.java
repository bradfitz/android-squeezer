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


import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerArtworkItem;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.util.ImageCache;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Represents the view hierarchy for a single {@link SqueezerItem} subclass.
 * where the item has track artwork associated with it.
 * 
 * @param <T>
 */
public abstract class SqueezerAlbumArtView<T extends SqueezerArtworkItem> extends
        SqueezerIconicItemView<T> {
    LayoutInflater mLayoutInflater;

    /** The icon to display when there's no artwork for this item */
    private static final int ICON_NO_ARTWORK = R.drawable.icon_album_noart_143;

    /** The icon to display when artwork exists, but has not been loaded yet */
    private static final int ICON_PENDING_ARTWORK = R.drawable.icon_album_noart_143;

    private static ImageCache sImageCache = ImageCache.getInstance();

    public SqueezerAlbumArtView(SqueezerItemListActivity activity) {
		super(activity);
        mLayoutInflater = activity.getLayoutInflater();
	}

    @Override
    public View getAdapterView(View convertView, T item) {
        ViewHolder viewHolder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mLayoutInflater.inflate(R.layout.icon_two_line_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.text1 = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.text2 = (TextView) convertView.findViewById(R.id.text2);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        setItemViewText(viewHolder, item);

        viewHolder.artworkUrl = getAlbumArtUrl(item.getArtwork_track_id());

        // If there's no artwork then use the "no art" resource, and note
        // that there's no artwork to fetch.
        if (viewHolder.artworkUrl == null) {
            viewHolder.icon.setImageResource(ICON_NO_ARTWORK);
            viewHolder.updateArtwork = false;
        } else {
            // Check the cache to see if the image is there, and set
            // it immediately if it is.
            Bitmap b = sImageCache.getFast(viewHolder.artworkUrl);
            if (b != null) {
                viewHolder.icon.setImageBitmap(b);
                viewHolder.updateArtwork = false;
            } else {
                // If flinging or an update is pending then note the artwork
                // needs to be updated, but do nothing else.
                if (activity.isArtworkUpdatePending()
                        || activity.getScrollState() == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    viewHolder.icon.setImageResource(ICON_PENDING_ARTWORK);
                    viewHolder.updateArtwork = true;
                } else {
                    // Not flinging, no pending updates, go ahead and update the
                    // artwork now.
                    downloadUrlToImageView(viewHolder.artworkUrl, viewHolder.icon);
                    viewHolder.updateArtwork = false;
                }
            }
        }

        return convertView;
    }

    @Override
    public View getAdapterView(View convertView, String text) {
        final ViewHolder viewHolder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mLayoutInflater.inflate(R.layout.icon_two_line_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.text1 = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.text2 = (TextView) convertView.findViewById(R.id.text2);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.text1.setText(text);
        viewHolder.text2.setText(null);
        viewHolder.icon.setImageDrawable(null);

        return convertView;
    }

    /**
     * Sets the text1 and text2 properties of the viewHolder from the item.
     * 
     * @param viewHolder
     * @param item
     */
    abstract void setItemViewText(ViewHolder viewHolder, T item);

    /**
     * Returns the URL to download the specified album artwork, or null if the
     * artwork does not exist, or there was a problem with the service.
     * 
     * @param artwork_track_id
     * @return
     */
    protected String getAlbumArtUrl(String artwork_track_id) {
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

    public static class ViewHolder {
        TextView text1;
        TextView text2;

        // XXX: These are public so code in SqueezerItemListActivity can see
        // them. This should be refactored.
        public ImageView icon;
        public String artworkUrl;
        public boolean updateArtwork;
    }
}