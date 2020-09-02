package uk.org.ngo.squeezer.service;

import android.content.Context;

import uk.org.ngo.squeezer.R;

public class PlayerNotFoundException extends Exception {
    public PlayerNotFoundException(Context context) {
        super(context.getString(R.string.NO_PLAYER_FOUND));
    }
}
