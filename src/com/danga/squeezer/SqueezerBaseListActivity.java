package com.danga.squeezer;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * <p>
 * A generic base class for an activity to list items of a particular SqueezeServer data type. The data type
 * is defined by the generic type argument, and must be an extension of {@link SqueezerItem}.
 * You must provide an {@link SqueezerItemView} to provide the view logic used by this activity. This is done
 * by implementing {@link SqueezerListActivity#createItemView()}.
 * <p>
 * When the activity is first created ({@link #onCreate(Bundle)}), an empty {@link SqueezerItemListAdapter} is
 * created using the provided {@link SqueezerItemView}. See {@link SqueezerListActivity}, too see details
 * of ordering and receiving of list items from SqueezeServer, and handling of item selection.
 * 
 * @param <T>	Denotes the class of the items this class should list
 * @author Kurt Aaholst
 */
public abstract class SqueezerBaseListActivity<T extends SqueezerItem> extends Activity implements SqueezerListActivity<T> {
    private ISqueezeService service = null;
	private SqueezerItemListAdapter<T> itemListAdapter;
	private Handler uiThreadHandler = new Handler() {};
	private ListView listView;
	private SqueezerItemView<T> itemView;
    
    protected String getTag() {
    	return getClass().getSimpleName();
	}
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prepareActivity(getIntent().getExtras());
		setContentView(R.layout.item_list);
		listView = (ListView) findViewById(R.id.item_list);
		itemView = createItemView();
    	listView.setOnItemClickListener(onItemClick);
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
   			try {
				registerCallback();
				orderItems();
            } catch (RemoteException e) {
                Log.e(getTag(), "Error registering list callback: " + e);
            }
        }
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, SqueezeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());
    }

	@Override
    public void onPause() {
        super.onPause();
        if (getService() != null) {
        	try {
				unregisterCallback();
			} catch (RemoteException e) {
                Log.e(getTag(), "Error unregistering list callback: " + e);
			}
        }
        if (serviceConnection != null) {
        	unbindService(serviceConnection);
        }
    }    

	/**
	 * @return The squeezeservice, or null if not bound
	 */
	public ISqueezeService getService() {
		return service;
	}

	/**
	 * @return The current listadapter, or null if not set
	 */
	public SqueezerItemAdapter<T> getItemListAdapter() {
		return itemListAdapter;
	}

	/**
	 * Use this to post Runnables to work off thread
	 */
	public Handler getUIThreadHandler() {
		return uiThreadHandler;
	}

	/**
	 * Order items from the start, and prepare an adapter to receive them
	 * @throws RemoteException 
	 */
	public void orderItems() {
		try {
			orderItems(0);
		} catch (RemoteException e) {
			Log.e(getTag(), "Error ordering items: " + e);
		}
		resetItemListAdapter();
	}
	
	/**
	 * Set the adapter to handle the display of the items, see also {@link #setListAdapter(android.widget.ListAdapter)}
	 * @param listAdapter
	 */
	public void resetItemListAdapter() {
		itemListAdapter = new SqueezerItemListAdapter<T>(itemView, 0);
		listView.setAdapter(itemListAdapter);
	}
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.itemlist, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	MenuItem fetchAll = menu.findItem(R.id.menu_item_fetch_all);
    	fetchAll.setVisible(!getItemListAdapter().isFullyLoaded());
    	return true;
    }

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_home:
        	SqueezerHomeActivity.show(this);
			return true;
        case R.id.menu_item_main:
        	SqueezerActivity.show(this);
			return true;
        case R.id.menu_item_fetch_all:
        	try {
				orderItems(getItemListAdapter().getCount());
			} catch (RemoteException e) {
                Log.e(getTag(), "Error ordering remaining items: " + e);
			}
			return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}
	
	private OnItemClickListener onItemClick = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			T item = getItemListAdapter().getItem(position);
			
			if (item == null || item.getId() != null) {
	   			try {
					onItemSelected(position, item);
	            } catch (RemoteException e) {
	                Log.e(getTag(), "Error from default action for '" + item + "': " + e);
	            }
			}
		}
	};

}
