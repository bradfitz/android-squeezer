package com.danga.squeezer;


import java.util.List;

import android.os.Bundle;
import android.os.RemoteException;

/**
 * <p>
 * Generic interface which defines what all squeezer item list activities
 * must implement.
 * </p>
 * @author Kurt Aaholst
 *
 * @param <T> Denotes the type of item the activity can list
 */
public interface SqueezerListActivity<T extends SqueezerItem> {

	/**
	 * @return A new view logic to be used by this activity
	 */
	SqueezerItemView<T> createItemView();
	
	/**
	 * Initial setup of this activity. 
	 * @param extras Optionally use this information to setup the activity. (may be null)
	 */
    public void prepareActivity(Bundle extras);
    
    /**
     * This is called when the service is connected.
     * <p>
     * You must register a callback for {@link SqueezeService} to call when the ordered items
     * from {@link #orderItems(int)} are received from SqueezeServer. This callback must pass
     * these items on to {@link SqueezerItemListAdapter#update(int, int, int, List)}.
     * 
     * @throws RemoteException
     */
	public void registerCallback() throws RemoteException;
	
	/**
	 * This is called when the service is disconnected.
	 * @throws RemoteException
	 */
	public void unregisterCallback() throws RemoteException;
    
    /**
     * Implementations must start an asynchronous fetch of items, when this is called.
     * @throws RemoteException
     * @param start Position in list to start the fetch. Pass this on the {@link SqueezeService}
     */
	public void orderItems(int start) throws RemoteException;
	
	/**
	 * Implement the action to be taken when an item is selected. Be
	 * @param index Position in the list of the selected item.
	 * @param item The selected item. This may be null if 
	 * @throws RemoteException
	 */
	public void onItemSelected(int index, T item) throws RemoteException;

}