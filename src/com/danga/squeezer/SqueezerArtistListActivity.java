package com.danga.squeezer;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.danga.squeezer.model.SqueezeArtist;

public class SqueezerArtistListActivity extends SqueezerBaseListActivity<SqueezeArtist>{

	@Override
	protected void prepareActivity(Bundle extras) {
		//TODO
	}

	@Override
	protected void prepareService() throws RemoteException {
		getServiceStub().registerArtistListCallback(artistsListCallback);
		getServiceStub().artists();
	}

	@Override
	protected void releaseService() throws RemoteException {
		getServiceStub().unregisterArtistListCallback(artistsListCallback);
	}

	@Override
	protected void onItemSelected(int index, SqueezeArtist item) throws RemoteException {
		SqueezerAlbumListActivity.show(this, item);
	}

    
	static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerArtistListActivity.class);
        context.startActivity(intent);
    }

    private IServiceArtistListCallback artistsListCallback = new IServiceArtistListCallback.Stub() {

		public void onArtistsReceived(final int count, final int pos, final List<SqueezeArtist> artists) throws RemoteException {
			getUiThreadHandler().post(new Runnable() {
				public void run() {
					if (getItemListAdapter() == null) {
						SqueezeArtist artist = new SqueezeArtist().setName(getString(R.string.loading_text));
						SqueezeArtist[] artists = new SqueezeArtist[count];
						Arrays.fill(artists, artist);
				    	setItemListAdapter(new SqueezerBaseListAdapter<SqueezeArtist>(SqueezerArtistListActivity.this, artists) {
				    		private int rowLayout = R.layout.list_item;

				    		public View getView(int position, View convertView, ViewGroup parent) {
				    			View row = getActivity().getLayoutInflater().inflate(rowLayout, null);

				    			TextView label = (TextView) row;
				    			label.setText((CharSequence) getItem(position).getName());

				    			return (row);
				    		}
				    		
						});
					}
					getItemListAdapter().update(pos, artists);
				}
			});
		}
    	
    };

}
