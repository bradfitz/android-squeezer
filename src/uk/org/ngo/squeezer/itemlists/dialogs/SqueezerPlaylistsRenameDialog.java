package uk.org.ngo.squeezer.itemlists.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.SqueezerPlaylistsActivity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

public class SqueezerPlaylistsRenameDialog extends SqueezerBaseEditTextDialog {
    private SqueezerPlaylistsActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (SqueezerPlaylistsActivity) getActivity();
        dialog.setTitle(getString(R.string.rename_title, activity.getCurrentPlaylist().getName()));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(activity.getCurrentPlaylist().getName());

        return dialog;
    }

    @Override
    protected boolean commit(String newname) {
        try {
            activity.setOldname(activity.getCurrentPlaylist().getName());
            activity.getService().playlistsRename(activity.getCurrentPlaylist(), newname);
            activity.getCurrentPlaylist().setName(newname);
            activity.getItemAdapter().notifyDataSetChanged();
        } catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newname + "': " + e);
        }
        return true;
    }

}
