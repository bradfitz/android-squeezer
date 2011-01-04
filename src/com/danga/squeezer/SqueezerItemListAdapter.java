/**
 * 
 */
package com.danga.squeezer;


/**
 * <p>
 * Specialization of {@link SqueezerItemAdapter} to be used in
 * {@link SqueezerBaseListActivity}.
 * 
 * @param <T>
 *            Denotes the class of the items this class should list
 * @author Kurt Aaholst
 */
public class SqueezerItemListAdapter<T extends SqueezerItem> extends SqueezerItemAdapter<T> {
	
	/**
	 * Calls {@link SqueezerItemAdapter#SqueezerBaseAdapter(SqueezerItemView, int)}
	 */
	public SqueezerItemListAdapter(SqueezerItemView<T> itemView, int count) {
		super(itemView, count);
	}

	@Override
	protected T[] setUpList(int max) {
		T[] items = super.setUpList(max);
		int size = items.length;
		String item_text = getQuantityString(size);
		String header = (getTotalItems() > size
				? getActivity().getString(R.string.browse_max_items_text, size, getTotalItems(), item_text)
				: getActivity().getString(R.string.browse_items_text, size, item_text));
		getActivity().setTitle(header);
		return items;
	}

}