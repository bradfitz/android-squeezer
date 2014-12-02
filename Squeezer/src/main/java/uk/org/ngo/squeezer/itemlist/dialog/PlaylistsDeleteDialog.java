package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlaylistsActivity;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class PlaylistsDeleteDialog extends DialogFragment {

    private PlaylistsActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        activity = (PlaylistsActivity) getActivity();
        builder.setTitle(getString(R.string.delete_title, activity.getCurrentPlaylist().getName()));
        builder.setMessage(R.string.delete__message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ISqueezeService service = activity.getService();
                if (service == null) {
                    return;
                }

                service.playlistsDelete(activity.getCurrentPlaylist());
                activity.clearAndReOrderItems();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

}
