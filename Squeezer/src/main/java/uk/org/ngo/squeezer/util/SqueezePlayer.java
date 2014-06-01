package uk.org.ngo.squeezer.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import uk.org.ngo.squeezer.Preferences;

public class SqueezePlayer {
    private static final String SQUEEZEPLAYER_PACKAGE = "de.bluegaspode.squeezeplayer";
    private static final String SQUEEZEPLAYER_SERVICE = "de.bluegaspode.squeezeplayer.playback.service.PlaybackService";
    private static final String TAG = "SqueezePlayer";

    public static boolean hasSqueezePlayer(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(SQUEEZEPLAYER_PACKAGE);
        return (intent != null);
    }

    public static void maybeStartSqueezePlayer(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
        boolean squeezePlayerEnabled = preferences.getBoolean(Preferences.KEY_SQUEEZEPLAYER_ENABLED, true);
        if (squeezePlayerEnabled && hasSqueezePlayer(context)) {
            final ComponentName component = new ComponentName(SQUEEZEPLAYER_PACKAGE, SQUEEZEPLAYER_SERVICE);
            context.startService(new Intent().setComponent(component));
        }
    }

}
