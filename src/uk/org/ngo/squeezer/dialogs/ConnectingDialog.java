package uk.org.ngo.squeezer.dialogs;

import uk.org.ngo.squeezer.NowPlayingFragment;
import uk.org.ngo.squeezer.R;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public class ConnectingDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String connectingTo = args.getString("connectingTo");
        ProgressDialog connectingDialog = new ProgressDialog(getActivity());
        connectingDialog.setTitle(getText(R.string.connecting_text));
        connectingDialog.setIndeterminate(true);
        connectingDialog.setMessage(getString(R.string.connecting_to_text, connectingTo));
        return connectingDialog;
    }

    @Override
    public void onDismiss(android.content.DialogInterface dialog) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            Fragment nowPlayingFragment = fm.findFragmentById(R.id.now_playing_fragment);
            if (nowPlayingFragment != null)
                ((NowPlayingFragment) nowPlayingFragment).clearConnectingDialog();
        }
    };

    public static ConnectingDialog addTo(FragmentActivity activity, String connectingTo) {
        ConnectingDialog dialog = new ConnectingDialog();
        Bundle args = new Bundle();
        args.putString("connectingTo", connectingTo);
        dialog.setArguments(args);
        try {
            dialog.show(activity.getSupportFragmentManager(), "ConnectingDialog");
        } catch (IllegalStateException e) {
            // Apparently we are not allowed to show the dialog at this point.
            Log.i("ConnectingDialog", "show() was not allowed: " + e);
            return null;
        }
        return dialog;
    }

}