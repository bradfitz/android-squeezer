package com.danga.squeezer;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.model.SqueezeAlbum;
import com.danga.squeezer.model.SqueezeArtist;

public class SqueezerAlbumListActivity extends SqueezerBaseListActivity<SqueezeAlbum> {
	SqueezeArtist artist;
	
	@Override
	protected void prepareActivity(Bundle extras) {
		for (String key : extras.keySet()) {
			if (SqueezeArtist.class.getName().equals(key)) {
				artist = extras.getParcelable(key);
			} else
				Log.e(getTag(), "Unexpected extra value: " + key + "("
						+ extras.get(key).getClass().getName() + ")");
		}
	}

	@Override
	protected void prepareService() throws RemoteException {
		getServiceStub().registerAlbumListCallback(albumListCallback);
		getServiceStub().albums(artist);
	}

	@Override
	protected void releaseService() throws RemoteException {
		getServiceStub().unregisterAlbumListCallback(albumListCallback);
	}

	@Override
	protected void onItemSelected(int index, SqueezeAlbum item) throws RemoteException {
		getServiceStub().playAlbum(item);
		SqueezerActivity.show(this);
	}

	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerAlbumListActivity.class);
        context.startActivity(intent);
    }

	public static void show(Context context, SqueezeArtist artist) {
        final Intent intent = new Intent(context, SqueezerAlbumListActivity.class);
        intent.putExtra(artist.getClass().getName(), artist);
        context.startActivity(intent);
    }

    private IServiceAlbumListCallback albumListCallback = new IServiceAlbumListCallback.Stub() {
    	
		public void onAlbumsReceived(final int count, final int pos, final List<SqueezeAlbum> albums) throws RemoteException {
			getUiThreadHandler().post(new Runnable() {
				public void run() {
					if (getItemListAdapter() == null) {
						SqueezeAlbum album = new SqueezeAlbum().setName(getString(R.string.loading_text));
						SqueezeAlbum[] albums = new SqueezeAlbum[count];
						Arrays.fill(albums, album);
				    	setItemListAdapter(new SqueezerBaseListAdapter<SqueezeAlbum>(SqueezerAlbumListActivity.this, albums) {
				    		private int rowLayout = R.layout.icon_two_line_layout;
				    		private int iconId = R.id.icon;
				    		private int text1Id = R.id.text1;
				    		private int text2Id = R.id.text2;

				    		public View getView(int position, View convertView, ViewGroup parent) {
				    			View row = getActivity().getLayoutInflater().inflate(rowLayout, null);
				    			SqueezeAlbum item = getItem(position);

				    			TextView label1 = (TextView) row.findViewById(text1Id);
				    			label1.setText((CharSequence) item.getName());

				    			if (item.getId() != null) {
					    			TextView label2 = (TextView) row.findViewById(text2Id);
					    			String text2 = item.getArtist();
					    			if (item.getYear() != 0) text2 += " - " + item.getYear();
					    			label2.setText((CharSequence) text2);
				    			}

								ImageView icon = (ImageView) row.findViewById(iconId);
								icon.setImageResource(R.drawable.icon_album_noart);

				    			return (row);
				    		}
						});
					}
					getItemListAdapter().update(pos, albums);
				}
			});
		}
    	
    };

}
