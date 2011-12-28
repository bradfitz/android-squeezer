package com.danga.squeezer.menu;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.danga.squeezer.R;


public class SqueezerFilterMenuItemFragment extends MenuFragment {
    private SqueezerFilterableListActivity activity;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (SqueezerFilterableListActivity)getActivity();
    };
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filtermenuitem, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_filter:
            activity.showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public interface SqueezerFilterableListActivity {
        public void showFilterDialog();
    }

}
