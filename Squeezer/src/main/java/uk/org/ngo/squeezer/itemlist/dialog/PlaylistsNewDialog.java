package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlaylistsActivity;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class PlaylistsNewDialog extends BaseEditTextDialog {

    private PlaylistsActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (PlaylistsActivity) getActivity();
        dialog.setTitle(R.string.new_playlist_title);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHint(R.string.new_playlist_hint);

        return dialog;
    }

    @Override
    protected boolean commit(String name) {
        ISqueezeService service = activity.getService();
        if (service == null) {
            return false;
        }

        service.playlistsNew(name);
        activity.clearAndReOrderItems();
        return true;
    }

}
