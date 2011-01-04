package com.danga.squeezer;

import android.view.Menu;

/**
 * <p>
 * Specialization of {@link SqueezerBaseListActivity}, which doesn't support
 * filtering and ordering.
 * </p>
 * 
 * @param <T>
 *            Denotes the class of the items this class should list
 * @author Kurt Aaholst
 */
public abstract class SqueezerBasicListActivity<T extends SqueezerItem> extends SqueezerBaseListActivity<T> {
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	menu.findItem(R.id.menu_item_filter).setVisible(false);
    	menu.findItem(R.id.menu_item_sort).setVisible(false);
    	return true;
    }

}
