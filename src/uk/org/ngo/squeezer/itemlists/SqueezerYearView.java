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
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerYear;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;

public class SqueezerYearView extends SqueezerBaseItemView<SqueezerYear> {

	public SqueezerYearView(SqueezerItemListActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.year, quantity);
	}

	public void onItemSelected(int index, SqueezerYear item) throws RemoteException {
		SqueezerAlbumListActivity.show(getActivity(), item);
	}

    // XXX: Make this a menu resource.
	public void setupContextMenu(ContextMenu menu, int index, SqueezerYear item) {
		menu.setHeaderTitle(item.getId());
        menu.add(Menu.NONE, R.id.browse_songs, 0, R.string.CONTEXTMENU_BROWSE_SONGS);
		menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ALBUMS, 1, R.string.CONTEXTMENU_BROWSE_ALBUMS);
        menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.CONTEXTMENU_PLAY_ITEM);
        menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.CONTEXTMENU_ADD_ITEM);
	};

}
