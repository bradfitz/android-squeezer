package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlaylistSongsActivity;

public class PlaylistRenameDialog extends BaseEditTextDialog {

    private PlaylistSongsActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (PlaylistSongsActivity) getActivity();
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
