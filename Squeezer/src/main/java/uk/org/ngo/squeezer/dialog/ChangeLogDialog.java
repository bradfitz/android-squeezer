package uk.org.ngo.squeezer.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.webkit.WebView;

import uk.org.ngo.squeezer.R;

/**
 * Extends ChangeLog to use the v7 support AlertDialog which follows the application theme.
 */
public class ChangeLogDialog extends de.cketti.library.changelog.ChangeLog {
    public ChangeLogDialog(final Context context) {
        super(context);
    }

    public ChangeLogDialog(final Context context, final String css) {
        super(context, css);
    }

    public ChangeLogDialog(final Context context, final SharedPreferences preferences, final String css) {
        super(context, preferences, css);
    }

    /**
     * Get a themed "What's New" dialog.
     *
     * @return An AlertDialog displaying the changes since the previous installed version of your
     *         app (What's New). But when this is the first run of your app including
     *         {@code ChangeLog} then the full log dialog is show.
     */
    public AlertDialog getThemedLogDialog() {
        return getThemedDialog(isFirstRunEver());
    }

    /**
     * Get a themed dialog with the full change log.
     *
     * @return An AlertDialog with a full change log displayed.
     */
    public AlertDialog getThemedFullLogDialog() {
        return getThemedDialog(true);
    }

    private AlertDialog getThemedDialog(boolean full) {
        WebView wv = new WebView(mContext.getApplicationContext());
        //wv.setBackgroundColor(0); // transparent
        wv.loadDataWithBaseURL(null, getLog(full), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(wv)
                .setCancelable(false)
                // OK button
                .setPositiveButton(
                        mContext.getResources().getString(R.string.changelog_ok_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // The user clicked "OK" so save the current version code as
                                // "last version code".
                                updateVersionInPreferences();
                            }
                        });

        if (!full) {
            // Show "More..." button if we're only displaying a partial change log.
            builder.setNegativeButton(R.string.changelog_show_full,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            getThemedFullLogDialog().show();
                        }
                    });
        }

        return builder.create();
    }
}
