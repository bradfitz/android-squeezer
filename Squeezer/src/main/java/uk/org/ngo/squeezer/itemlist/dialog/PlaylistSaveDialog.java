package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;

public class PlaylistSaveDialog extends BaseEditTextDialog {

    private BaseActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        Bundle args = getArguments();
        String name = args.getString("name");

        activity = (BaseActivity) getActivity();
        dialog.setTitle(R.string.save_playlist_title);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHint(R.string.save_playlist_hint);
        if (name != null && name.length() > 0) {
            editText.setText(name);
        }

        return dialog;
    }

    @Override
    protected boolean commit(String name) {
        try {
            activity.getService().playlistSave(name);
        } catch (RemoteException e) {
            Log.e(getTag(), "Error saving playlist as '" + name + "': " + e);
        }
        return true;
    }

    public static void addTo(BaseActivity activity, String name) {
        PlaylistSaveDialog dialog = new PlaylistSaveDialog();
        Bundle args = new Bundle();
        args.putString("name", name);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), "SaveDialog");
    }
}
