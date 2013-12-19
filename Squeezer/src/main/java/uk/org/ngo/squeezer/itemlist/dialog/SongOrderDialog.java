package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.SongListActivity;

public class SongOrderDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SongListActivity activity = (SongListActivity) getActivity();

        String[] sortOrderStrings = new String[SongsSortOrder.values().length];
        for (SongsSortOrder sortOrder : SongsSortOrder.values()) {
            sortOrderStrings[sortOrder.ordinal()] = getString(sortOrder.stringResource);
        }

        int checkedItem = activity.getSortOrder().ordinal();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.choose_sort_order,
                activity.getItemAdapter().getQuantityString(2)));
        builder.setSingleChoiceItems(sortOrderStrings, checkedItem,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int indexSelected) {
                        activity.setSortOrder(SongsSortOrder.values()[indexSelected]);
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    /**
     * Sort order strings supported by the server.
     * <p/>
     * Values must correspond with the string expected by the server. Any '__' in the strings will
     * be removed.
     */
    public enum SongsSortOrder {
        title(R.string.songs_sort_order_title),
        tracknum(R.string.songs_sort_order_tracknum);
        // TODO: At least some versions of the server support "albumtrack",
        // is that useful?

        /**
         * The text to use for this ordering
         */
        private int stringResource;

        private SongsSortOrder(int stringResource) {
            this.stringResource = stringResource;
        }
    }

}
