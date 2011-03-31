package com.danga.squeezer;

import java.util.List;

import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


/**
 * A generic class for an adapter to list items of a particular
 * SqueezeServer data type. The data type is defined by the generic type
 * argument, and must be an extension of {@link SqueezerItem}.
 * <p>
 * If you need an adapter for a {@link SqueezerBaseListActivity}, then use
 * {@link SqueezerItemListAdapter} instead.
 * <p>
 * 
 * @param <T>
 *            Denotes the class of the items this class should list
 * @see SqueezerItemView
 * @author Kurt Aaholst
 */
public class SqueezerItemAdapter<T extends SqueezerItem> extends BaseAdapter {

	/**
	 * View logic for this adapter
	 */
	private SqueezerItemView<T> itemView;
	
	/**
	 * List of items, and possibly headed with an empty item.
	 * <p>
	 * As the items are received from SqueezeServer they will be inserted in the
	 * list.
	 */
	private T[] items;
	
	/**
	 *  This is set if the list shall start with an empty item.
	 */
	private boolean emptyItem;

	/**
	 * Text to display before the items are received from SqueezeServer
	 */
	private String loadingText;
	
	/**
	 * Total numbers of items as reported by SqueezeServer, including an optional empty item, at the
	 * beginning of the list.
	 */
	private int totalItems;

	/**
	 * Creates a new adapter. Initially the item list is populated with items displaying the
	 * localized "loading" text. Call {@link #update(int, int, int, List)} as items arrives
	 * from SqueezeServer.
	 *   
	 * @param itemView The {@link SqueezerItemView} to use with this adapter
	 * @param count The number of items this adapter initially contains. 
	 * @param emptyItem If set the list of items shall start with an empty item
	 */
	public SqueezerItemAdapter(SqueezerItemView<T> itemView, int count, boolean emptyItem) {
		this.itemView = itemView;
		itemView.setAdapter(this);
		this.emptyItem = emptyItem;
		loadingText = itemView.getActivity().getString(R.string.loading_text);
		count += (emptyItem ? 1 : 0);
		this.totalItems = count;
		setItems(setUpList(count));
	}

	/**
	 * Removes all items from this adapter leaving it empty.
	 */
	public void clear() {
		totalItems = (emptyItem ? 1 : 0);
		setItems(setUpList(0));
	}
	
	/**
	 * Calls {@link #SqueezerBaseAdapter(SqueezerItemView, int, boolean)}, with emptyItem = false
	 */
	public SqueezerItemAdapter(SqueezerItemView<T> itemView, int count) {
		this(itemView, count, false);
	}

	/**
	 * Calls {@link #SqueezerBaseAdapter(SqueezerItemView, int, boolean)}, with emptyItem = false
	 */
	public SqueezerItemAdapter(SqueezerItemView<T> itemView) {
		this(itemView, 0);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		T item = items[position];
		if (item != null)
			return itemView.getAdapterView(convertView, item);
		return Util.getListItemView(getActivity(), convertView, (position == 0 && emptyItem ? "" : loadingText));
	}
	
	public String getQuantityString(int size) {
		return itemView.getQuantityString(size);
	}
	
	public SqueezerBaseActivity getActivity() {
		return itemView.getActivity();
	}
	
	public void setupContextMenu(ContextMenu menu, int position) {
		final T selectedItem = getItem(position);
		if (selectedItem != null && selectedItem.getId() != null) {
			itemView.setupContextMenu(menu, position, selectedItem);
		}
	}
	
	public boolean doItemContext(MenuItem menuItem, int position) throws RemoteException {
		return itemView.doItemContext(menuItem, position, getItem(position));
	}
	
	public void setItemView(SqueezerItemView<T> itemView) {
		this.itemView = itemView;
	}

	public SqueezerItemView<T> getItemView() {
		return itemView;
	}

	private void setItems(T[] items) {
		this.items = items;
	}

	public int getCount() {
		return items.length;
	}

	public T getItem(int position) {
		return items[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public int getTotalItems() {
		return totalItems;
	}

	public boolean isFullyLoaded() {
		return (getTotalItems() <= getCount());
	}

	public String getHeader(int count) {
		String item_text = getQuantityString(count);
		String header = (getTotalItems() > count
				? getActivity().getString(R.string.browse_max_items_text, item_text, count, getTotalItems())
				: getActivity().getString(R.string.browse_items_text, item_text, count));
		return header;
	}


	/**
	 * Allocate a list of {@link T}, and possible an empty item at position 0.
	 * <p>
	 * The size of the list will be {@link #totalItems} or max.
	 * 
	 * @param max
	 *            Maximum size of list.
	 * @return The new list
	 */
	protected T[] setUpList(int max) {
		int size = (getTotalItems() > max ? max : getTotalItems());
		T[] items = arrayInstance(size);
		return items;
	}

	/**
	 * Update the contents of the items in this list.
	 * <p>
	 * The size of the list of items is automatically adjusted if necessary, to obey the given
	 * parameters.
	 * 
	 * @param count
	 *            Number of items as reported by squeezeserver.
	 * @param start
	 *            The start position of items in this update.
	 * @param items
	 *            New items to insert in the main list
	 */
	public void update(int count, int max, int start, List<T> items) {
		int offset = (emptyItem ? 1 : 0);
		count += offset;
		start += offset;
		max += offset;
		if (count != getTotalItems() || start + items.size() > getCount()) {
			totalItems = count;
			T[] newItems = setUpList(start + items.size() > max ? count : max);
			for (int i = 0; i < getCount(); i++) {
				if (i >= newItems.length) break;
				newItems[i] = getItem(i);
			}
			setItems(newItems);
		}
		for (T t: items) {
			this.items[start++] = t;
		}

		notifyDataSetChanged();
	}

	/**
	 * @param item
	 * @return The position of the given item in this adapter or 0 if not found
	 */
	public int findItem(T item) {
		for (int pos = 0; pos < items.length; pos++)
			if (items[pos] == null) {
				if (item == null) return pos;
			} else
				if (items[pos].equals(item)) return pos;
		return 0;
	}

	protected T[] arrayInstance(int size) {
		return itemView.getCreator().newArray(size);
	}

}