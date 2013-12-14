package uk.org.ngo.squeezer.menu;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import uk.org.ngo.squeezer.R;

/**
 * A fragment that implements a "Sort" menu.
 * <p/>
 * Activities that host this fragment must implement {@link #OrderableListActivity}.
 *
 * <pre>
 * public void onCreate(Bundle savedInstanceState) {
 *     ...
 *     BaseMenuFragment.add(this, OrderMenuItemFragment.class);
 * }
 *
 * public void showOrderDialog() {
 * }
 * </pre>
 */
public class OrderMenuItemFragment extends BaseMenuFragment {

    OrderableListActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        activity = (OrderableListActivity) getActivity();
    }

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

    /**
     * Interface that activities that host this fragment must implement.
     */
    public interface OrderableListActivity {

        /**
         * Show a dialog allowing the user to choose the sort order.
         */
        public void showOrderDialog();

        /**
         * Ensure that the activity that hosts this fragment derives from FragmentActivity.
         */
        FragmentManager getSupportFragmentManager();
    }

}
