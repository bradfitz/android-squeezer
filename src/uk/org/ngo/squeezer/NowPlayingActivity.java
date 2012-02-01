/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer;

import uk.org.ngo.squeezer.framework.SqueezerBaseActivity;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerMenuFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class NowPlayingActivity extends SqueezerBaseActivity {
    protected static final int HOME_REQUESTCODE = 0;
    private final String TAG = "NowPlayingActivity";

    private GoogleAnalyticsTracker tracker;

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.now_playing);

        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);
        if (preferences.getBoolean(Preferences.KEY_ANALYTICS_ENABLED, true)) {
            if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                Log.v("NowPlayingActivity", "Tracking page view 'NowPlayingActivity");
                // Start the tracker in manual dispatch mode...
                tracker = GoogleAnalyticsTracker.getInstance();
                tracker.startNewSession("UA-26457780-1", this);
                tracker.trackPageView("NowPlayingActivity");
            }
        }

        MenuFragment.add(this, SqueezerMenuFragment.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume...");
    }


    @Override
    public void onPause() {
        Log.d(TAG, "onPause...");
        super.onPause();
    }


    /*
     * (non-Javadoc)
     * @see
     * uk.org.ngo.squeezer.framework.SqueezerBaseActivity#onServiceConnected()
     */
    @Override
    protected void onServiceConnected() throws RemoteException {
        // Does nothing
    }

    public static void show(Context context) {
		final Intent intent = new Intent(context, NowPlayingActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Send analytics stats (if enabled).
        if (tracker != null) {
            tracker.dispatch();
            tracker.stopSession();
        }
    }
}
