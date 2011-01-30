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
	public SqueezerItemListAdapter(SqueezerItemView<T> itemView) {
		super(itemView);
	}

	@Override
	protected T[] setUpList(int max) {
		T[] items = super.setUpList(max);
		getActivity().setTitle(getHeader(items.length));
		return items;
	}

}