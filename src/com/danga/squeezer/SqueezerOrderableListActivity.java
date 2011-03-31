package com.danga.squeezer;

import android.view.Menu;
import android.view.MenuItem;

public abstract class SqueezerOrderableListActivity<T extends SqueezerItem> extends SqueezerFilterableListActivity<T> {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ordermenuitem, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_sort:
			showDialog(DIALOG_ORDER);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

}
