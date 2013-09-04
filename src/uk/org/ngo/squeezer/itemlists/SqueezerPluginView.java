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
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.model.SqueezerPlugin;
import uk.org.ngo.squeezer.util.ImageFetcher;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class SqueezerPluginView extends SqueezerBaseItemView<SqueezerPlugin> {
    public SqueezerPluginView(SqueezerBaseListActivity<SqueezerPlugin> activity) {
		super(activity);
	}

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, SqueezerPlugin item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (convertView != null && convertView.getTag().getClass() == ViewHolder.class)
                ? (ViewHolder) convertView.getTag()
                : null;

        if (viewHolder == null) {
            convertView = getLayoutInflater().inflate(R.layout.icon_large_row_layout, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.label = (TextView) convertView.findViewById(R.id.label);
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(viewHolder);
        }

		viewHolder.label.setText(item.getName());
        imageFetcher.loadImage(getActivity().getIconUrl(item.getIcon()), viewHolder.icon);
		return convertView;
	}

	private static class ViewHolder {
		TextView label;
		ImageView icon;
	}

}
