package uk.org.ngo.squeezer.menu;

import android.view.MenuItem;

import uk.org.ngo.squeezer.SqueezerHomeActivity;


public class SqueezerMenuFragment extends MenuFragment {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Application icon clicked.
            case android.R.id.home:
                SqueezerHomeActivity.show(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
