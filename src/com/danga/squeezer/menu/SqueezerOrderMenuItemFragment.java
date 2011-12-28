package com.danga.squeezer.menu;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.danga.squeezer.R;


public class SqueezerOrderMenuItemFragment extends MenuFragment {
    private SqueezerOrderableListActivity activity;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (SqueezerOrderableListActivity) getActivity();
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ordermenuitem, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_sort:
            activity.showOrderDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public interface SqueezerOrderableListActivity {
        public void showOrderDialog();
    }

}
