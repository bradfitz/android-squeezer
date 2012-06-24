package uk.org.ngo.squeezer.itemlists.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.SqueezerPlaylistSongsActivity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;

public class SqueezerPlaylistRenameDialog extends SqueezerBaseEditTextDialog {
    private SqueezerPlaylistSongsActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (SqueezerPlaylistSongsActivity) getActivity();
        dialog.setTitle(getString(R.string.rename_title, activity.getPlaylist().getName()));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(activity.getPlaylist().getName());

        return dialog;
    }

    @Override
    protected boolean commit(String newname) {
        activity.playlistRename(newname);
        return true;
    }

}
