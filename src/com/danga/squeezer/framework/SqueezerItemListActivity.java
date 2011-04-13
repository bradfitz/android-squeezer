package com.danga.squeezer.framework;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.RemoteException;
import android.util.Log;

import com.danga.squeezer.service.SqueezeService;

/**
 * <p>
 * This class defines the common minimum, which any activity browsing the SqueezeServer's database
 * must implement.
 * </p>
 * @author Kurt Aaholst
 */
public abstract class SqueezerItemListActivity extends SqueezerBaseActivity {
    
    /**
     * This is called when the service is connected.
     * <p>
     * You must register a callback for {@link SqueezeService} to call when the ordered items
     * from {@link #orderPage(int)} are received from SqueezeServer. This callback must pass
     * these items on to {@link SqueezerItemListAdapter#update(int, int, int, List)}.
     * 
     * @throws RemoteException
     */
	protected abstract void registerCallback() throws RemoteException;
	
	/**
	 * This is called when the service is disconnected.
	 * @throws RemoteException
	 */
	protected abstract void unregisterCallback() throws RemoteException;
    
    /**
     * Implementations must start an asynchronous fetch of items, when this is called.
     * @throws RemoteException
     * @param start Position in list to start the fetch. Pass this on to {@link SqueezeService}
     */
	protected abstract void orderPage(int start) throws RemoteException;


	private Set<Integer> orderedPages = new HashSet<Integer>();

	/**
	 * Order page at specified position, if it has not already been ordered.
	 * @param pagePosition
	 */
	public void maybeOrderPage(int pagePosition) {
		if (!orderedPages.contains(pagePosition)) {
			orderedPages.add(pagePosition);
			try {
				orderPage(pagePosition);
			} catch (RemoteException e) {
				Log.e(getTag(), "Error ordering items (" + pagePosition + "): " + e);
			}
		}
	}

	/**
	 * Clear all information about which pages has been ordered, and reorder the first page
	 */
	public void reorderItems() {
		orderedPages.clear();
		maybeOrderPage(0);
	}
}