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
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A view that shows a single song with its artwork, and a context menu.
 */
public class SqueezerSongView extends SqueezerAlbumArtView<SqueezerSong> {
    private static final String TAG = "SqueezerSongView";

	private final LayoutInflater layoutInflater;

	private boolean browseByAlbum;
	public void setBrowseByAlbum(boolean browseByAlbum) { this.browseByAlbum = browseByAlbum; }

	private boolean browseByArtist;
	public void setBrowseByArtist(boolean browseByArtist) { this.browseByArtist = browseByArtist; }

	public SqueezerSongView(SqueezerItemListActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	@Override
    public View getAdapterView(View convertView, final SqueezerSong item) {
        final ViewHolder viewHolder;

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
			if (item.getArtist() != null) text2 += item.getArtist();
			if (item.getAlbum() != null) text2 += " - " + item.getAlbum();
			if (item.getYear() != 0) text2 = item.getYear() + " - " + text2;
		}
		viewHolder.label2.setText(text2);

		setIconToTrackArtwork(viewHolder.icon, item);

		return convertView;
	}

	public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
		getActivity().insert(item);
	}

    /**
     * Creates the context menu for a song by inflating R.menu.songcontextmenu.
     * <p>
     * Subclasses that show songs in playlists should call through to this
     * first, then adjust the visibility of R.id.group_playlist.
     */
	public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.songcontextmenu, menu);

		menu.setHeaderTitle(item.getName());

        if (item.getAlbum_id() != null && !browseByAlbum)
            menu.findItem(R.id.view_this_album).setVisible(true);

        if (item.getArtist_id() != null)
            menu.findItem(R.id.view_albums_by_song).setVisible(true);

        if (item.getArtist_id() != null && !browseByArtist)
            menu.findItem(R.id.view_songs_by_artist).setVisible(true);
    }

	@Override
	public boolean doItemContext(android.view.MenuItem menuItem, int index, SqueezerSong selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
            case R.id.view_this_album:
                SqueezerSongListActivity.show(getActivity(),
                        new SqueezerAlbum(selectedItem.getAlbum_id(), selectedItem.getAlbum()));
                return true;

            case R.id.view_albums_by_song:
                SqueezerAlbumListActivity.show(getActivity(),
                        new SqueezerArtist(selectedItem.getArtist_id(), selectedItem.getArtist()));
                return true;

            case R.id.view_songs_by_artist:
                SqueezerSongListActivity.show(getActivity(),
                        new SqueezerArtist(selectedItem.getArtist_id(), selectedItem.getArtist()));
                return true;

            case R.id.download:
                ((SqueezerAbstractSongListActivity) getActivity()).downloadSong(selectedItem
                        .getId());
                return true;
		}

		return super.doItemContext(menuItem, index, selectedItem);
	};

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.song, quantity);
	}

	private static class ViewHolder {
		TextView label1;
		TextView label2;
		ImageView icon;
	}

}
