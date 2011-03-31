package com.danga.squeezer.itemlists;

import java.util.List;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;

import com.danga.squeezer.service.ISqueezeService;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerItemAdapter;
import com.danga.squeezer.model.SqueezerGenre;

public class GenreSpinner {
	private static final String TAG = GenreSpinner.class.getName();
	GenreSpinnerCallback callback;
	private SqueezerBaseActivity activity;
	private Spinner spinner;

	public GenreSpinner(GenreSpinnerCallback callback, SqueezerBaseActivity activity, Spinner spinner) {
		this.callback = callback;
		this.activity = activity;
		this.spinner = spinner;
		registerCallback();
		orderItems(0);
	}

	private void orderItems(int start) {
		if (callback.getService() != null) {
			try {
				callback.getService().genres(start);
			} catch (RemoteException e) {
                Log.e(TAG, "Error ordering items: " + e);
			}
		}
	}

	public void registerCallback() {
		if (callback.getService() != null) {
			try {
				callback.getService().registerGenreListCallback(genreListCallback);
			} catch (RemoteException e) {
                Log.e(TAG, "Error registering callback: " + e);
			}
		}
	}

	public void unregisterCallback() {
		if (callback.getService() != null) {
			try {
				callback.getService().unregisterGenreListCallback(genreListCallback);
			} catch (RemoteException e) {
                Log.e(TAG, "Error unregistering callback: " + e);
			}
		}
	}

    private IServiceGenreListCallback genreListCallback = new IServiceGenreListCallback.Stub() {
		private SqueezerItemAdapter<SqueezerGenre> adapter;
    	
		public void onGenresReceived(final int count, final int max, final int start, final List<SqueezerGenre> list) throws RemoteException {
			callback.getUIThreadHandler().post(new Runnable() {
				public void run() {
					if (adapter == null) {
						SqueezerGenreView itemView = new SqueezerGenreView(activity);
						adapter = new SqueezerItemAdapter<SqueezerGenre>(itemView, count, true);
						spinner.setAdapter(adapter);
						if (count > max) orderItems(max);
					}
					adapter.update(count, max, start, list);
					spinner.setSelection(adapter.findItem(callback.getGenre()));
				}
			});
		}
    	
    };
    
    public interface GenreSpinnerCallback {
    	ISqueezeService getService();
    	Handler getUIThreadHandler();
    	SqueezerGenre getGenre();
    }

}
