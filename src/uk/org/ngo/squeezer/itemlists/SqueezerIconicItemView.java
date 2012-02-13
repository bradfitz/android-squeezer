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

import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.framework.SqueezerIconUpdater;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import android.widget.ImageView;

/**
 * An item view with associated icon where the image is remote.
 * 
 * @param <T>
 */
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