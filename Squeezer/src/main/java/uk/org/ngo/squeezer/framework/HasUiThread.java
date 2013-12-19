package uk.org.ngo.squeezer.framework;

import android.os.Handler;

public interface HasUiThread {

    Handler getUIThreadHandler();
}
