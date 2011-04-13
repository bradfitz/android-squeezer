package com.danga.squeezer.framework;

import android.view.Menu;
import android.view.MenuItem;

import com.danga.squeezer.R;

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
