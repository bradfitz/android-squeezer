package uk.org.ngo.squeezer;


import android.content.Context;

import androidx.multidex.MultiDexApplication;

// Trick to make the app context useful available everywhere.
// See http://stackoverflow.com/questions/987072/using-application-context-everywhere

public class Squeezer extends MultiDexApplication {

    private static Squeezer instance;

    public Squeezer() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

