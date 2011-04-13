package com.danga.squeezer.framework;

import android.view.Menu;
import android.view.MenuItem;

import com.danga.squeezer.R;

public abstract class SqueezerFilterableListActivity<T extends SqueezerItem> extends SqueezerBaseListActivity<T> {
	
	@Override
	public boolean onSearchRequested() {
		showDialog(DIALOG_FILTER);
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filtermenuitem, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_filter:
			showDialog(DIALOG_FILTER);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

}
