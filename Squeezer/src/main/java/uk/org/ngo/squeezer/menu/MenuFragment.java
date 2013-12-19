package uk.org.ngo.squeezer.menu;

import android.view.MenuItem;

import uk.org.ngo.squeezer.HomeActivity;


public class MenuFragment extends BaseMenuFragment {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Application icon clicked.
            case android.R.id.home:
                HomeActivity.show(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
