package uk.org.ngo.squeezer.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.SqueezerActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final TextView message = (TextView) getActivity().getLayoutInflater().inflate(R.layout.about_textview, null);

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo info;
        String aboutTitle;
        try {
            info = pm.getPackageInfo("uk.org.ngo.squeezer", 0);
            aboutTitle = getString(R.string.about_title, info.versionName);
        } catch (NameNotFoundException e) {
            aboutTitle = "Package not found.";
        }

        message.setText(Html.fromHtml((String) getText(R.string.about_text)));
        message.setAutoLinkMask(SqueezerActivity.RESULT_OK);
        message.setMovementMethod(ScrollingMovementMethod.getInstance());

        return new AlertDialog.Builder(getActivity())
                .setTitle(aboutTitle)
                .setView(message)
                .create();
    }
}