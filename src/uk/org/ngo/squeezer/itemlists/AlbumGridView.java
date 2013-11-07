/*
 * Copyright (c) 2013 Kurt Aaholst <kaaholst@gmail.com>
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

import android.view.View;
import android.view.ViewGroup;

import java.util.EnumSet;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.util.ImageFetcher;

/**
 * Shows a single album with its artwork, and a context menu.
 */
public class AlbumGridView extends SqueezerAlbumView {
    public AlbumGridView(SqueezerItemListActivity activity) {
        super(activity);
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, EnumSet<ViewParams> viewParams) {
        return getAdapterView(convertView, parent, viewParams, R.layout.grid_item);
    }

    @Override
    public void bindView(View view, SqueezerAlbum item, ImageFetcher imageFetcher) {
        super.bindView(view, item, imageFetcher);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Marquee effect on TextViews only works if they're selected.
        viewHolder.text1.setSelected(true);
        viewHolder.text2.setSelected(true);
    }
}
