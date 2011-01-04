package com.danga.squeezer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.model.SqueezePlayer;;

public class SqueezerPlayerListActivity extends SqueezerBaseListActivity<SqueezePlayer> {
	private static Map<String, Integer> modelIcons = initializeModelIcons();

	private static Map<String, Integer> initializeModelIcons() {
		Map<String, Integer> modelIcons = new HashMap<String, Integer>();
		modelIcons.put("baby", R.drawable.icon_baby);
		modelIcons.put("boom", R.drawable.icon_boom);
		modelIcons.put("fab4", R.drawable.icon_fab4);
		modelIcons.put("receiver", R.drawable.icon_receiver);
		modelIcons.put("controller", R.drawable.icon_controller);
		modelIcons.put("sb1n2", R.drawable.icon_sb1n2);
		modelIcons.put("sb3", R.drawable.icon_sb3);
		modelIcons.put("slimp3", R.drawable.icon_slimp3);
		modelIcons.put("softsqueeze", R.drawable.icon_softsqueeze);
		modelIcons.put("squeezeplay", R.drawable.icon_squeezeplay);
		modelIcons.put("transporter", R.drawable.icon_transporter);
		return modelIcons;
	}

	private int getModelIcon(String model) {
		Integer icon = modelIcons.get(model);
		return (icon != null ? icon : R.drawable.icon_blank);
	}

	@Override
	protected void prepareActivity(Bundle extras) {
	}

	@Override
	protected void prepareService() throws RemoteException {
		getServiceStub().registerPlayerListCallback(playerListCallback);
		getServiceStub().players();
	}

	@Override
	protected void releaseService() throws RemoteException {
		getServiceStub().unregisterPlayerListCallback(playerListCallback);
	}

	@Override
	protected void onItemSelected(int index, SqueezePlayer item) throws RemoteException {
		getServiceStub().setActivePlayer(item);
		finish();
	};
	
	static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlayerListActivity.class);
        context.startActivity(intent);
    }

    private IServicePlayerListCallback playerListCallback = new IServicePlayerListCallback.Stub() {
    	
		public void onPlayersReceived(final int count, final int pos, final List<SqueezePlayer> players) throws RemoteException {
			getUiThreadHandler().post(new Runnable() {
				public void run() {
					if (getItemListAdapter() == null) {
						SqueezePlayer player = new SqueezePlayer().setName(getString(R.string.loading_text));
						SqueezePlayer[] players = new SqueezePlayer[count];
						Arrays.fill(players, player);
				    	setItemListAdapter(new SqueezerBaseListAdapter<SqueezePlayer>(SqueezerPlayerListActivity.this, players) {
				    		private int rowLayout = R.layout.icon_large_row_layout;
				    		private int iconId = R.id.icon;
				    		private int text1Id = R.id.label;

				    		public View getView(int position, View convertView, ViewGroup parent) {
				    			View row = getActivity().getLayoutInflater().inflate(rowLayout, null);
				    			SqueezePlayer item = getItem(position);

				    			TextView label = (TextView) row.findViewById(text1Id);
				    			label.setText((CharSequence) item.getName());

								ImageView icon = (ImageView) row.findViewById(iconId);
								icon.setImageResource(getModelIcon(item.getModel()));

				    			return (row);
				    		}
						});
					}
					getItemListAdapter().update(pos, players);
				}
			});
		}
    	
    };

}
