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

import uk.org.ngo.squeezer.SqueezerActivity;
import uk.org.ngo.squeezer.model.SqueezerPluginItem;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import uk.org.ngo.squeezer.R;

public class SqueezerPluginItemView extends SqueezerIconicItemView<SqueezerPluginItem> {
	private final SqueezerPluginItemListActivity activity;
	private final LayoutInflater layoutInflater;

	public SqueezerPluginItemView(SqueezerPluginItemListActivity activity) {
		super(activity);
		this.activity = activity;
		layoutInflater = activity.getLayoutInflater();
	}

	@Override
	public View getAdapterView(View convertView, SqueezerPluginItem item) {
		ViewHolder viewHolder;

		if (convertView == null || convertView.getTag() == null) {
			convertView = layoutInflater.inflate(R.layout.plugin_item_row_layout, null);
			viewHolder = new ViewHolder();
			viewHolder.label = (TextView) convertView.findViewById(R.id.label);
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			viewHolder.groupIcon =  (ImageView) convertView.findViewById(R.id.groupicon);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (ViewHolder) convertView.getTag();

		viewHolder.label.setText(item.getName());
		viewHolder.groupIcon.setVisibility(item.isHasitems() ? View.VISIBLE : View.INVISIBLE);
		updateIcon(viewHolder.icon, item, item.getImage());

		return convertView;
	}

	private static class ViewHolder {
		TextView label;
		ImageView icon;
		ImageView groupIcon;
	}

	public String getQuantityString(int quantity) {
		return null;
	}

	public void onItemSelected(int index, SqueezerPluginItem item) throws RemoteException {
		if (item.isHasitems())
			activity.show(item);
		else {
			activity.play(item);
			SqueezerActivity.show(getActivity());
		}
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerPluginItem item) {
		if (!item.isHasitems()) {
			menu.setHeaderTitle(item.getName());
			menu.add(Menu.NONE, CONTEXTMENU_PLAY_ITEM, 1, R.string.CONTEXTMENU_PLAY_ITEM);
			menu.add(Menu.NONE, CONTEXTMENU_ADD_ITEM, 2, R.string.CONTEXTMENU_ADD_ITEM);
			menu.add(Menu.NONE, CONTEXTMENU_INSERT_ITEM, 3, R.string.CONTEXTMENU_INSERT_ITEM);
		}
	}

	@Override
	public boolean doItemContext(MenuItem menuItem, int index, SqueezerPluginItem selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
		case CONTEXTMENU_PLAY_ITEM:
			if (activity.play(selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_PLAYING, selectedItem.getName()), Toast.LENGTH_SHORT).show();
			return true;
		case CONTEXTMENU_ADD_ITEM:
			if (activity.add(selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_ADDED, selectedItem.getName()), Toast.LENGTH_SHORT).show();
			return true;
		case CONTEXTMENU_INSERT_ITEM:
			if (activity.insert(selectedItem))
				Toast.makeText(activity, activity.getString(R.string.ITEM_INSERTED, selectedItem.getName()), Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

}
