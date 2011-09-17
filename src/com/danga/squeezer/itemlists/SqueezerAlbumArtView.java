package com.danga.squeezer.itemlists;


import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerArtworkItem;
import com.danga.squeezer.framework.SqueezerItem;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.service.ISqueezeService;

public abstract class SqueezerAlbumArtView<T extends SqueezerItem> extends SqueezerIconicItemView<T> {
	public SqueezerAlbumArtView(SqueezerItemListActivity activity) {
		super(activity);
	}
	
	protected void updateAlbumArt(final ImageView icon, final SqueezerArtworkItem item) {
		icon.setImageResource(R.drawable.icon_album_noart);
		updateIcon(icon, item, getAlbumArtUrl(item.getArtwork_track_id()));
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