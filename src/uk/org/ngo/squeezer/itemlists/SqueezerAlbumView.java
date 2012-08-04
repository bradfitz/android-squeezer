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

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.View;

/**
 * Shows a single album with its artwork, and a context menu.
 */
public class SqueezerAlbumView extends SqueezerAlbumArtView<SqueezerAlbum> {
    public SqueezerAlbumView(SqueezerItemListActivity activity) {
        super(activity);
    }

    public void bindView(ViewHolder viewHolder, SqueezerAlbum item) {
        viewHolder.text1.setText(item.getName());

        // Might be null if we're using the one-line layout
        if (viewHolder.text2 != null) {
            String text2 = "";
            if (item.getId() != null) {
                text2 = item.getArtist();
                if (item.getYear() != 0)
                    text2 += " - " + item.getYear();
            }
            viewHolder.text2.setText(text2);
        }
    }

    public void bindView(ViewHolder viewHolder, String text) {
        viewHolder.text1.setText(text);
    }

	public void onItemSelected(int index, SqueezerAlbum item) throws RemoteException {
		getActivity().play(item);
		NowPlayingActivity.show(getActivity());
	}

    /**
     * Creates the context menu for an album by inflating
     * R.menu.albumcontextmenu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menuInfo.menuInflater.inflate(R.menu.albumcontextmenu, menu);
    }

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.album, quantity);
	}
}
