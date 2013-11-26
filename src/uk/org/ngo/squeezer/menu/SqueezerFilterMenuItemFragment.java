package uk.org.ngo.squeezer.menu;

import uk.org.ngo.squeezer.R;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * A fragment that implements a "Filter" menu.
 * <p>
 * Activities that host this fragment must implement
 * {@link #SqueezerFilterableListActivity}.
 * 
 * <pre>
 * {@code
 * public void onCreate(Bundle savedInstanceState) {
 *     ...
 *     MenuFragment.add(this, SqueezerFilterMenuItemFragment.class);
 * }
 * 
 * public void showFilterDialog() {
 * }
 * </pre>
 */
public class SqueezerFilterMenuItemFragment extends MenuFragment {
    SqueezerFilterableListActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filtermenuitem, menu);
        super.onCreateOptionsMenu(menu, inflater);
        activity = (SqueezerFilterableListActivity) getActivity();
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

    /**
     * Interface that activities that host this fragment must implement.
     */
    public interface SqueezerFilterableListActivity {
        /**
         * Show a dialog allowing the user to specify how to filter the results.
         */
        public void showFilterDialog();

        /**
         * Ensure that the activity that hosts this fragment derives from
         * FragmentActivity.
         */
        FragmentManager getSupportFragmentManager();
    }
}
