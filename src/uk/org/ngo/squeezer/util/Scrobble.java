package uk.org.ngo.squeezer.util;

import uk.org.ngo.squeezer.Squeezer;

public class Scrobble {
    public static boolean haveScrobbleDroid() {
        return Intents.isBroadcastReceiverAvailable(
                Squeezer.getContext(),
                "net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
    }

    public static boolean haveSls() {
        return Intents.isBroadcastReceiverAvailable(
                Squeezer.getContext(),
                "com.adam.aslfms.notify.playstatechanged");
    }

    public static boolean canScrobble() {
        return haveScrobbleDroid() || haveSls();
    }

}
