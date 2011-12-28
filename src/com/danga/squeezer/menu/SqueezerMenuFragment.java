package com.danga.squeezer.menu;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.danga.squeezer.R;
import com.danga.squeezer.SettingsActivity;
import com.danga.squeezer.SqueezerActivity;
import com.danga.squeezer.SqueezerHomeActivity;


public class SqueezerMenuFragment extends MenuFragment {

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.itemlistmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            SqueezerActivity.show(getActivity());
            return true;
        case R.id.menu_item_home:
            SqueezerHomeActivity.show(getActivity());
            return true;
        case R.id.menu_item_settings:
            SettingsActivity.show(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
