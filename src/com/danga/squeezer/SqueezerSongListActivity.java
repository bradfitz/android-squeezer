package com.danga.squeezer;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.model.SqueezeSong;

public class SqueezerSongListActivity extends SqueezerBaseListActivity<SqueezeSong> {

	@Override
	protected void prepareActivity(Bundle extras) {
		//TODO
	}

	@Override
	protected void prepareService() throws RemoteException {
		getServiceStub().registerSongListCallback(songListCallback);
		getServiceStub().songs();
	}

	@Override
	protected void releaseService() throws RemoteException {
		getServiceStub().registerSongListCallback(songListCallback);
	}

	@Override
	protected void onItemSelected(int index, SqueezeSong item) throws RemoteException {
		getServiceStub().playlistIndex(index);
		finish();
	}
    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerSongListActivity.class);
        context.startActivity(intent);
    }

    private IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
    	
		public void onSongsReceived(final int count, final int pos, final List<SqueezeSong> albums) throws RemoteException {
			getUiThreadHandler().post(new Runnable() {
				public void run() {
					if (getItemListAdapter() == null) {
						SqueezeSong song = new SqueezeSong().setName(getString(R.string.loading_text));
						SqueezeSong[] songs = new SqueezeSong[count];
						Arrays.fill(songs, song);
				    	setItemListAdapter(new SqueezerBaseListAdapter<SqueezeSong>(SqueezerSongListActivity.this, songs) {
				    		private int rowLayout = R.layout.icon_two_line_layout;
				    		private int iconId = R.id.icon;
				    		private int text1Id = R.id.text1;
				    		private int text2Id = R.id.text2;

				    		public View getView(int position, View convertView, ViewGroup parent) {
				    			View row = getActivity().getLayoutInflater().inflate(rowLayout, null);
				    			SqueezeSong item = getItem(position);

				    			TextView label1 = (TextView) row.findViewById(text1Id);
				    			label1.setText((CharSequence) item.getName());

				    			if (item.getId() != null) {
					    			TextView label2 = (TextView) row.findViewById(text2Id);
					    			label2.setText((CharSequence) (item.getYear() + " - " + item.getArtist()  + " - " + item.getAlbum()));
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
