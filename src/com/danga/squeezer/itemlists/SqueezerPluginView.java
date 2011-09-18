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

package com.danga.squeezer.itemlists;

import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerPlugin;
import com.danga.squeezer.service.ISqueezeService;

public abstract class SqueezerPluginView extends SqueezerIconicItemView<SqueezerPlugin> {
	private final LayoutInflater layoutInflater;

	public SqueezerPluginView(SqueezerItemListActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	@Override
	public View getAdapterView(View convertView, SqueezerPlugin item) {
		ViewHolder viewHolder;

		if (convertView == null || convertView.getTag() == null) {
			convertView = layoutInflater.inflate(R.layout.icon_large_row_layout, null);
			viewHolder = new ViewHolder();
			viewHolder.label = (TextView) convertView.findViewById(R.id.label);
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (ViewHolder) convertView.getTag();

		viewHolder.label.setText(item.getName());
		updateIcon(viewHolder.icon, item, getIconUrl(item.getIcon()));

		return convertView;
	}

	private String getIconUrl(String icon) {
		if (icon == null) return null;

		ISqueezeService service = getActivity().getService();
		if (service == null) return null;

		try {
			return service.getIconUrl(icon);
		} catch (RemoteException e) {
			Log.e(getClass().getSimpleName(), "Error requesting icon url: " + e);
			return null;
		}
	}

	private static class ViewHolder {
		TextView label;
		ImageView icon;
	}

}
