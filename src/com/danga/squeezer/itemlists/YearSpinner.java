package com.danga.squeezer.itemlists;

import java.util.List;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;

import com.danga.squeezer.framework.SqueezerItemAdapter;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerYear;
import com.danga.squeezer.service.ISqueezeService;

public class YearSpinner {
	private static final String TAG = YearSpinner.class.getName();
	YearSpinnerCallback callback;
	private SqueezerItemListActivity activity;
	private Spinner spinner;

	public YearSpinner(YearSpinnerCallback callback, SqueezerItemListActivity activity, Spinner spinner) {
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
    	
		public void onYearsReceived(final int count, final int start, final List<SqueezerYear> list) throws RemoteException {
			callback.getUIThreadHandler().post(new Runnable() {
				public void run() {
					if (adapter == null) {
						SqueezerYearView itemView = new SqueezerYearView(activity);
						adapter = new SqueezerItemAdapter<SqueezerYear>(itemView, true);
						spinner.setAdapter(adapter);
					}
					adapter.update(count, start, list);
					spinner.setSelection(adapter.findItem(callback.getYear()));

					if (count > start + list.size())
						if ((start + list.size()) % adapter.getPageSize() == 0 ) {
							orderItems(start + list.size());
						}
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
