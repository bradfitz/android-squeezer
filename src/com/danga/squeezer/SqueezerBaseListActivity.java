package com.danga.squeezer;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.danga.squeezer.ISqueezeService;

/**
 * <p>
 * A generic base class for an activity to list items of a particular SqueezeServer data type. The data type
 * is defined by the generic type argument, and must be an extension of {@link SqueezeItem}.
 * The page must contain an extension of {@link SqueezerBaseListAdapter} for your {@link SqueezeItem}.
 * This is set by {@link #setListAdapter(android.widget.ListAdapter)}. It can be an anonymous inner class,
 * if you don't want to use it in other pages.<br/>
 * Other than this, implement the abstract methods, and you are done.
 * </p>
 * @param <T>	Denotes the class of the items this class should list
 * @author Kurt Aaholst
 */
public abstract class SqueezerBaseListActivity<T extends SqueezeItem> extends ListActivity {
    private ISqueezeService serviceStub = null;
	private SqueezerBaseListAdapter<T> itemListAdapter;
	private Handler uiThreadHandler = new Handler() {};
    
    protected String getTag() {
    	return getClass().getSimpleName();
	}
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent().getExtras() != null)
			prepareActivity(getIntent().getExtras());
    	setListAdapter(IconRowAdapter.loadingAdapter(this));
    	getListView().setFastScrollEnabled(true);
    	getListView().setOnItemClickListener(onItemClick);
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceStub = ISqueezeService.Stub.asInterface(service);
   			try {
				prepareService();
            } catch (RemoteException e) {
                Log.e(getTag(), e.toString());
            }
        }
        public void onServiceDisconnected(ComponentName name) {
            serviceStub = null;
        };
    };

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, SqueezeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getServiceStub());
        if (getServiceStub() != null) {
   			try {
				prepareService();
            } catch (RemoteException e) {
                Log.e(getTag(), e.toString());
            }
        }
    }

	@Override
    public void onPause() {
        super.onPause();
        if (getServiceStub() != null) {
        	try {
				releaseService();
			} catch (RemoteException e) {
                Log.e(getTag(), e.toString());
			}
        }
        if (serviceConnection != null) {
        	unbindService(serviceConnection);
        }
    }    

    protected abstract void prepareActivity(Bundle extras);
	protected abstract void prepareService() throws RemoteException;
	protected abstract void releaseService() throws RemoteException;
	protected abstract void onItemSelected(int index, T item) throws RemoteException;
	
	public ISqueezeService getServiceStub() {
		return serviceStub;
	}

	public SqueezerBaseListAdapter<T> getItemListAdapter() {
		return itemListAdapter;
	}

	public Handler getUiThreadHandler() {
		return uiThreadHandler;
	}

	public void setItemListAdapter(SqueezerBaseListAdapter<T> listAdapter) {
		this.itemListAdapter = listAdapter;
		setListAdapter(itemListAdapter);
	}

	private OnItemClickListener onItemClick = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (getItemListAdapter() != null) {
				T item = getItemListAdapter().getItem(position);
				if (item.getId() != null) {
		   			try {
						onItemSelected(position, item);
		            } catch (RemoteException e) {
		                Log.e(getTag(), e.toString());
		            }
				}
			}
		}
	};

}
