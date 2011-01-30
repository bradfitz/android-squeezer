package com.danga.squeezer.itemlists;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

import com.danga.squeezer.ISqueezeService;
import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerArtworkItem;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.SqueezerItem;

public abstract class SqueezerIconicItemView<T extends SqueezerItem> extends SqueezerBaseItemView<T> {
	private final ScheduledThreadPoolExecutor backgroundExecutor = new ScheduledThreadPoolExecutor(1);

	public SqueezerIconicItemView(SqueezerBaseActivity activity) {
		super(activity);
	}
	
	protected void updateAlbumArt(final ImageView icon, final SqueezerArtworkItem item) {
		icon.setImageResource(R.drawable.icon_album_noart);
		final String albumArtUrl = getAlbumArtUrl(item.getArtwork_track_id());

		if (albumArtUrl == null || albumArtUrl.length() == 0) {
			icon.setTag(null);
			return;
		}

		icon.setTag(item);
		backgroundExecutor.execute(new Runnable() {
			public void run() {
				if (icon.getTag() != item) {
                    // Bail out before fetch the resource if the item for
                    // album art has changed since this Runnable got scheduled.
                    return;
				}
				try {
					URL url = new URL(albumArtUrl);
					InputStream inputStream = (InputStream) url.getContent();
					final Drawable drawable = Drawable.createFromStream(inputStream, "src");
					getActivity().getUIThreadHandler().post(new Runnable() {
						public void run() {
							if (icon.getTag() == item) {
                                // Only set the image if the item art hasn't changed since we
                                // started and finally fetched the image over the network
                                // and decoded it.
								icon.setImageDrawable(drawable);
							}
						}
						
					});
				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
			}
		});
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