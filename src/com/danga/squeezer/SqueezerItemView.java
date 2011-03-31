package com.danga.squeezer;

import android.app.Activity;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.Parcelable.Creator;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Adapter;


/**
 * Defines view logic for a {@link SqueezerItem}
 * 
 * @author Kurt Aaholst
 * 
 * @param <T>
 *            Denotes the class of the item this class implements view logic for
 */
public interface SqueezerItemView<T extends SqueezerItem> {

	/**
	 * @return The activity associated with this view logic
	 */
	SqueezerBaseActivity getActivity();

	/**
	 * @return {@link Resources#getQuantityString(int, int)}
	 */
	String getQuantityString(int quantity);

	/**
	 * <p>Called by {@link Adapter#getView(int, View, ViewGroup)}
	 * 
	 * @param convertView
	 * @param item
	 * @return
	 */
	View getAdapterView(View convertView, T item);

	/**
	 * @return The generic argument of the implementation
	 */
    Class<T> getItemClass();
    
    /**
     * @return the creator for the current {@link SqueezerItem} implementation
     */
	Creator<T> getCreator();

	/**
	 * Set the adapter which uses this view logic
	 * @param adapter
	 */
	void setAdapter(SqueezerItemAdapter<T> adapter);
	
	/**
	 * <p>Setup the context menu for the current {@link SqueezerItem} implementation
	 * <p>Leave this empty if there shall be no context menu.
	 * @see OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View, android.view.ContextMenu.ContextMenuInfo)
	 */
	void setupContextMenu(ContextMenu menu, int index, T item);

	/**
	 * Perform the selected action from the context menu for the selected item.
	 * 
	 * @param selectedItem The item the context menu was generated for
	 * @param menuItem The selected menu action
	 * @return True if the action was consumed
	 * @throws RemoteException
	 * @see {@link Activity#onContextItemSelected(MenuItem)}
	 */
	public boolean doItemContext(MenuItem menuItem, int index, T selectedItems) throws RemoteException;

}
