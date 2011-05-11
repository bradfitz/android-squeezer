package com.danga.squeezer.itemlists;

import android.widget.ImageView;

import com.danga.squeezer.framework.SqueezerBaseItemView;
import com.danga.squeezer.framework.SqueezerIconUpdater;
import com.danga.squeezer.framework.SqueezerItem;
import com.danga.squeezer.framework.SqueezerItemListActivity;

public abstract class SqueezerIconicItemView<T extends SqueezerItem> extends SqueezerBaseItemView<T> {
	private final SqueezerIconUpdater<T> iconUpdater;

	public SqueezerIconicItemView(SqueezerItemListActivity activity) {
		super(activity);
		iconUpdater = new SqueezerIconUpdater<T>(activity);
	}

	protected void updateIcon(final ImageView icon, final Object item, final String urlString) {
		iconUpdater.updateIcon(icon, item, urlString);
	}

}