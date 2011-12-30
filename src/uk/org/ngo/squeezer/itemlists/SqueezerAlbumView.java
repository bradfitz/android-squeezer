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
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import uk.org.ngo.squeezer.R;

public class SqueezerAlbumView extends SqueezerAlbumArtView<SqueezerAlbum> {
	private final LayoutInflater layoutInflater;

	public SqueezerAlbumView(SqueezerItemListActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	@Override
	public View getAdapterView(View convertView, SqueezerAlbum item) {
		ViewHolder viewHolder;

		if (convertView == null || convertView.getTag() == null) {
			convertView = layoutInflater.inflate(R.layout.icon_two_line_layout, null);
			viewHolder = new ViewHolder();
			viewHolder.label1 = (TextView) convertView.findViewById(R.id.text1);
			viewHolder.label2 = (TextView) convertView.findViewById(R.id.text2);
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (ViewHolder) convertView.getTag();

		viewHolder.label1.setText(item.getName());
		String text2 = "";
		if (item.getId() != null) {
			text2 = item.getArtist();
			if (item.getYear() != 0) text2 += " - " + item.getYear();
		}
		viewHolder.label2.setText(text2);
		updateAlbumArt(viewHolder.icon, item);

		return convertView;
	}

	public void onItemSelected(int index, SqueezerAlbum item) throws RemoteException {
		getActivity().play(item);
		SqueezerActivity.show(getActivity());
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerAlbum item) {
		menu.setHeaderTitle(item.getName());
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_SONGS, 0, R.string.CONTEXTMENU_BROWSE_SONGS);
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ARTISTS, 2, R.string.CONTEXTMENU_BROWSE_ARTISTS);
		menu.add(Menu.NONE, CONTEXTMENU_PLAY_ITEM, 3, R.string.CONTEXTMENU_PLAY_ITEM);
		menu.add(Menu.NONE, CONTEXTMENU_ADD_ITEM, 4, R.string.CONTEXTMENU_ADD_ITEM);
	};

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.album, quantity);
	}

    private static class ViewHolder {
		TextView label1;
		TextView label2;
		ImageView icon;
	}

}
