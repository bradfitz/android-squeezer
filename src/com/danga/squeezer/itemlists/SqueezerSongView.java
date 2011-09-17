package com.danga.squeezer.itemlists;


import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerSongView extends SqueezerIconicItemView<SqueezerSong> {
	private LayoutInflater layoutInflater;

	private boolean browseByAlbum;
	public void setBrowseByAlbum(boolean browseByAlbum) { this.browseByAlbum = browseByAlbum; }

	private boolean browseByArtist;
	public void setBrowseByArtist(boolean browseByArtist) { this.browseByArtist = browseByArtist; }

	public SqueezerSongView(SqueezerItemListActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	@Override
	public View getAdapterView(View convertView, SqueezerSong item) {
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
		if (item.getId() != null) {
			String text2 =  item.getArtist()  + " - " + item.getAlbum();
			if (item.getYear() != 0) text2 = item.getYear() + " - " + text2;
			viewHolder.label2.setText(text2);
		} else
			viewHolder.label2.setText("");
		updateAlbumArt(viewHolder.icon, item);

		return convertView;
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
		menu.setHeaderTitle(item.getName());
		if (item.getAlbum_id() != null && !browseByAlbum)
			menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ALBUM_SONGS, 1, R.string.CONTEXTMENU_BROWSE_ALBUM_SONGS);
		if (item.getArtist_id() != null)
			menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ARTIST_ALBUMS, 2, R.string.CONTEXTMENU_BROWSE_ARTIST_ALBUMS);
		if (item.getArtist_id() != null && !browseByArtist)
			menu.add(Menu.NONE, CONTEXTMENU_BROWSE_ARTIST_SONGS, 3, R.string.CONTEXTMENU_BROWSE_ARTIST_SONGS);
		menu.add(Menu.NONE, CONTEXTMENU_PLAY_ITEM, 4, R.string.CONTEXTMENU_PLAY_ITEM);
		menu.add(Menu.NONE, CONTEXTMENU_ADD_ITEM, 5, R.string.CONTEXTMENU_ADD_ITEM);
		menu.add(Menu.NONE, CONTEXTMENU_INSERT_ITEM, 6, R.string.CONTEXTMENU_INSERT_ITEM);
	};

	@Override
	public boolean doItemContext(android.view.MenuItem menuItem, int index, SqueezerSong selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
		case CONTEXTMENU_BROWSE_ALBUM_SONGS:
			SqueezerSongListActivity.show(getActivity(), new SqueezerAlbum(selectedItem.getAlbum_id(), selectedItem.getAlbum()));
			return true;
		case CONTEXTMENU_BROWSE_ARTIST_ALBUMS:
			SqueezerAlbumListActivity.show(getActivity(), new SqueezerArtist(selectedItem.getArtist_id(), selectedItem.getArtist()));
			return true;
		case CONTEXTMENU_BROWSE_ARTIST_SONGS:
			SqueezerSongListActivity.show(getActivity(), new SqueezerArtist(selectedItem.getArtist_id(), selectedItem.getArtist()));
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
