/*
 * Copyright (c) 2013 Google Inc.  All Rights Reserved.
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


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import uk.org.ngo.squeezer.dialog.ServerAddressView;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

/**
 * An activity for when the user is not connected to a Squeezeserver.
 * <p/>
 * Provide a UI for connecting to the configured server, launch HomeActivity when the user
 * connects.
 */
public class DisconnectedActivity extends BaseActivity {
    private ServerAddressView serverAddressView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disconnected);
        serverAddressView = (ServerAddressView) findViewById(R.id.server_address_view);
    }

    /**
     * Show this activity.
     * <p/>
     * Flags are set to clear the previous activities, as trying to go back while disconnected makes
     * no sense.
     * <p/>
     * The pending transition is overridden to animate the activity in place, rather than having it
     * appear to move in from off-screen.
     */
    public static void show(Activity activity) {
        final Intent intent = new Intent(activity, DisconnectedActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Act on the user requesting a server connection through the activity's UI.
     *
     * @param view The view the user pressed.
     */
    public void onUserInitiatesConnect(View view) {
        serverAddressView.savePreferences();
        NowPlayingFragment fragment = (NowPlayingFragment) getSupportFragmentManager()
                .findFragmentById(R.id.now_playing_fragment);
        fragment.startVisibleConnection();
    }

    public void onEventMainThread(ConnectionChanged event) {
        if (event.mIsConnected) {
            // The user requested a connection to the server, which succeeded.  There's
            // no prior activity to go to, so launch HomeActivity, with flags to
            // clear other activities so hitting "back" won't show this activity again.
            final Intent intent = new Intent(this, HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
            this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}
