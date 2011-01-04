package com.danga.squeezer.itemlists;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

import com.danga.squeezer.ISqueezeService;
import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.SqueezerBaseListActivity;
import com.danga.squeezer.SqueezerItem;

public abstract class SqueezerIconicItemView<T extends SqueezerItem> extends SqueezerBaseItemView<T> {
	private SqueezerBaseListActivity<T> activity;

	public SqueezerIconicItemView(SqueezerBaseListActivity<T> activity) {
		super(activity);
		this.activity = activity;
	}

	protected void updateAlbumArt(ImageView icon, String artwork_track_id) {
		icon.setImageResource(R.drawable.icon_album_noart);
		final String albumArtUrl = getAlbumArtUrl(artwork_track_id);
		if (albumArtUrl != null && albumArtUrl.length() > 0) {
			URL url;
			InputStream inputStream = null;
			boolean gotContent = false;
			try {
				url = new URL(albumArtUrl);
				inputStream = (InputStream) url.getContent();
				gotContent = true;
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
			if (!gotContent) {
				return;
			}
			final Drawable drawable = Drawable.createFromStream(inputStream,
					"src");
			icon.setImageDrawable(drawable);
		}
	}

	private String getAlbumArtUrl(String artwork_track_id) {
		if (artwork_track_id == null) return null;
		
		ISqueezeService service = activity.getService();
		if (service == null) return null;
		
		try {
			return service.getAlbumArtUrl(artwork_track_id);
		} catch (RemoteException e) {
			Log.e(getClass().getSimpleName(), "Error requesting album art url: " + e);
			return null;
		}
	}

}