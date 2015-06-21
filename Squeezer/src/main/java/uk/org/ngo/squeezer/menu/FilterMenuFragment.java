package uk.org.ngo.squeezer.menu;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import uk.org.ngo.squeezer.R;

/**
 * A fragment that implements a "Filter" menu.
 * <p>
 * Activities that host this fragment must implement {@link #FilterableListActivity}.
 *
 * <pre>
 * public void onCreate(Bundle savedInstanceState) {
 *     ...
 *     BaseMenuFragment.add(this, FilterMenuFragment.class);
 * }
 *
 * public void showFilterDialog() {
 * }
 * </pre>
 */
public class FilterMenuFragment extends BaseMenuFragment {

    FilterableListActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filtermenuitem, menu);
        super.onCreateOptionsMenu(menu, inflater);
        activity = (FilterableListActivity) getActivity();
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
    public interface FilterableListActivity {

        /**
         * Show a dialog allowing the user to specify how to filter the results.
         */
        void showFilterDialog();

        /**
         * Ensure that the activity that hosts this fragment derives from FragmentActivity.
         */
        FragmentManager getSupportFragmentManager();
    }
}
