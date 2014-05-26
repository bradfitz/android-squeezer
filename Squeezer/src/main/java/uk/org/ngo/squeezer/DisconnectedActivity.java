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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.List;

import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.IServiceCallback;

/**
 * An activity for when the user is not connected to a Squeezeserver.
 * <p/>
 * Provide a UI for connecting to the configured server, launch HomeActivity when the user
 * connects.
 */
public class DisconnectedActivity extends BaseActivity {

    private final String TAG = "DisconnectedActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disconnected);

        Button btnConnect = (Button) findViewById(R.id.btn_connect);
        String ipPort = getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE).getString(
                Preferences.KEY_SERVERADDR, null);
        btnConnect.setText(getString(R.string.connect_to_text, ipPort));
    }

    /**
     * Show this activity.
     * <p/>
     * Flags are set to clear the previous activities, as trying to go back while disconnected makes
     * no sense.
     * <p/>
     * The pending transition is overridden to animate the activity in place, rather than having it
     * appear to move in from off-screen.
     *
     * @param activity
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
        NowPlayingFragment fragment = (NowPlayingFragment) getSupportFragmentManager()
                .findFragmentById(
                        R.id.now_playing_fragment);
        fragment.startVisibleConnection();
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        getService().registerCallback(serviceCallback);
    }

    private final IServiceCallback serviceCallback = new IServiceCallback() {
        // TODO: Maybe move onConnectionChanged to its own callback.

        @Override
        public void onConnectionChanged(final boolean isConnected, final boolean postConnect,
                final boolean loginFailed) {
            if (isConnected) {
                // The user requested a connection to the server, which succeeded.  There's
                // no prior activity to go to, so launch HomeActivity, with flags to
                // clear other activities so hitting "back" won't show this activity again.
                getUIThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        final Intent intent = new Intent(DisconnectedActivity.this,
                                HomeActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        DisconnectedActivity.this.startActivity(intent);
                        DisconnectedActivity.this.overridePendingTransition(android.R.anim.fade_in,
                                android.R.anim.fade_out);
                    }
                });
            }
        }

        @Override
        public void onPlayStatusChanged(final String playStatus) {
        }

        @Override
        public void onShuffleStatusChanged(final boolean initial, final int shuffleStatus) {
        }

        @Override
        public void onRepeatStatusChanged(final boolean initial, final int repeatStatus) {
        }

        @Override
        public void onTimeInSongChange(final int secondsIn, final int secondsTotal) {
        }

        @Override
        public void onPowerStatusChanged(final boolean canPowerOn, final boolean canPowerOff) {
        }

        @Override
        public Object getClient() {
            return DisconnectedActivity.this;
        }
    };
}
