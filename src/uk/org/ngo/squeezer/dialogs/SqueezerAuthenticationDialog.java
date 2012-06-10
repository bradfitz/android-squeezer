package uk.org.ngo.squeezer.dialogs;

import uk.org.ngo.squeezer.NowPlayingFragment;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;

public class SqueezerAuthenticationDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final SharedPreferences preferences = getActivity().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);

        View form = getActivity().getLayoutInflater().inflate(R.layout.authentication_dialog, null);
        builder.setView(form);

        final EditText userNameEditText = (EditText) form.findViewById(R.id.username);
        userNameEditText.setText(preferences.getString(Preferences.KEY_USERNAME, null));

        final EditText passwordEditText = (EditText) form.findViewById(R.id.password);
        passwordEditText.setText(preferences.getString(Preferences.KEY_PASSWORD, null));

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Preferences.KEY_USERNAME, userNameEditText.getText().toString());
                editor.putString(Preferences.KEY_PASSWORD, passwordEditText.getText().toString());
                editor.commit();

                ((NowPlayingFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.now_playing_fragment)).startVisibleConnection();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
