package uk.org.ngo.squeezer.itemlists.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.service.SqueezerServerString;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SqueezerAlbumOrderDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SqueezerAlbumListActivity activity = (SqueezerAlbumListActivity) getActivity();

        String[] sortOrderStrings = new String[AlbumsSortOrder.values().length];
        for (AlbumsSortOrder sortOrder : AlbumsSortOrder.values()) {
            sortOrderStrings[sortOrder.ordinal()] = activity.getServerString(sortOrder.serverString);
        }

        int checkedItem = activity.getSortOrder().ordinal();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.choose_sort_order, activity.getItemAdapter().getQuantityString(2)));
        builder.setSingleChoiceItems(sortOrderStrings, checkedItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int indexSelected) {
                    activity.setSortOrder(AlbumsSortOrder.values()[indexSelected]);
                    dialog.dismiss();
                }
            });

        return builder.create();
    }

    /**
     * Sort order strings supported by the server.
     * <p>
     * Values must correspond with the string expected by the server. Any '__'
     * in the strings will be removed.
     */
    public enum AlbumsSortOrder {
        __new(SqueezerServerString.BROWSE_NEW_MUSIC),
        album(SqueezerServerString.ALBUM),
        artflow(SqueezerServerString.SORT_ARTISTYEARALBUM),
        artistalbum(SqueezerServerString.SORT_ARTISTALBUM),
        yearalbum(SqueezerServerString.SORT_YEARALBUM),
        yearartistalbum(SqueezerServerString.SORT_YEARARTISTALBUM);
        
        private SqueezerServerString serverString;
        
        private AlbumsSortOrder(SqueezerServerString serverString) {
            this.serverString = serverString;
        }
    }

}