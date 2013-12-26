package uk.org.ngo.squeezer;


import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;

// Trick to make the app context useful available everywhere.
// See http://stackoverflow.com/questions/987072/using-application-context-everywhere

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=804fa58d",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast,
        sharedPreferencesName = Preferences.NAME,
        formKey = "")
public class Squeezer extends Application {

    private static Squeezer instance;

    public Squeezer() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }
}

