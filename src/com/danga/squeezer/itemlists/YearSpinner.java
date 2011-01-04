package com.danga.squeezer.itemlists;

import java.util.List;

import android.app.Activity;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;

import com.danga.squeezer.ISqueezeService;
import com.danga.squeezer.SqueezerItemAdapter;
import com.danga.squeezer.model.SqueezerYear;

public class YearSpinner {
	private static final String TAG = YearSpinner.class.getName();
	YearSpinnerCallback callback;
	private Activity activity;
	private Spinner spinner;

	public YearSpinner(YearSpinnerCallback callback, Activity activity, Spinner spinner) {
		this.callback = callback;
		this.activity = activity;
		this.spinner = spinner;
		registerCallback();
		orderItems(0);
	}

	private void orderItems(int start) {
		if (callback.getService() != null) {
			try {
				callback.getService().years(start);
			} catch (RemoteException e) {
                Log.e(TAG, "Error ordering items: " + e);
			}
		}
	}

	public void registerCallback() {
		if (callback.getService() != null) {
			try {
				callback.getService().registerYearListCallback(yearListCallback);
			} catch (RemoteException e) {
                Log.e(TAG, "Error registering callback: " + e);
			}
		}
	}

	public void unregisterCallback() {
		if (callback.getService() != null) {
			try {
				callback.getService().unregisterYearListCallback(yearListCallback);
			} catch (RemoteException e) {
                Log.e(TAG, "Error unregistering callback: " + e);
			}
		}
	}

    private IServiceYearListCallback yearListCallback = new IServiceYearListCallback.Stub() {
		private SqueezerItemAdapter<SqueezerYear> adapter;
    	
		public void onYearsReceived(final int count, final int max, final int start, final List<SqueezerYear> list) throws RemoteException {
			callback.getUIThreadHandler().post(new Runnable() {
				public void run() {
					if (adapter == null) {
						SqueezerYearView itemView = new SqueezerYearView(activity);
						adapter = new SqueezerItemAdapter<SqueezerYear>(itemView, count, true);
						spinner.setAdapter(adapter);
						if (count > max) orderItems(max);
					}
					adapter.update(count, max, start, list);
					spinner.setSelection(adapter.findItem(callback.getYear()));
				}
			});
		}
    	
    };
    
    public interface YearSpinnerCallback {
    	ISqueezeService getService();
    	Handler getUIThreadHandler();
    	SqueezerYear getYear();
    }

}
