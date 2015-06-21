package uk.org.ngo.squeezer.menu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * A base class to be extended by fragments which would like to participate in populating the action
 * bar, and which are used in an {@link ActionBarActivity} .
 * <p>
 * This class takes care of removing action bar items from the options menu. It also contains a few
 * convenience methods to ease using a menu fragment.
 *
 * @author Kurt Aaholst
 */
public class BaseMenuFragment extends Fragment {

    /**
     * Just a little helper, which calls {@link #setHasOptionsMenu(boolean)} with a true argument.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Conditionally add a fragment of the given type to the supplied activity. If a fragment of the
     * given type has not previously been added to the activity (by this method), then a new
     * instance of the fragment is created, and added to the activity by calling {@link
     * FragmentTransaction#add(Fragment, String)}
     *
     * @param activity The activity to add the new fragment to
     * @param clazz Type of the fragment to add to the activity
     */
    public static void add(FragmentActivity activity, Class<? extends Fragment> clazz) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(clazz.getName()) == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            try {
                fragmentTransaction.add(clazz.newInstance(), clazz.getName());
            } catch (Exception e) {
                throw new InstantiationException(
                        "Unable to instantiate fragment " + clazz.getName(), e);
            }
            fragmentTransaction.commit();
        }
    }

}
