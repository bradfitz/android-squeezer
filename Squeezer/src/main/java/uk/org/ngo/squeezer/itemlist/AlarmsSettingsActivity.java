/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.itemlist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.model.PlayerPref;
import uk.org.ngo.squeezer.service.IServicePlayerPrefCallback;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;

public class AlarmsSettingsActivity extends BaseActivity {

    private SeekBar volumeSlider;
    private SeekBar snoozeSlider;
    private SeekBar timeoutSlider;
    private CompoundButtonWrapper alarmsFadeButton;

    public static void show(Activity context) {
        final Intent intent = new Intent(context, AlarmsSettingsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarms_settings);
        setTitle(ServerString.ALARM.getLocalizedString());

        View alarms_volume = findViewById(R.id.alarms_volume);
        TextView volumeValue = (TextView) alarms_volume.findViewById(R.id.value);
        volumeSlider = (SeekBar) alarms_volume.findViewById(R.id.slider);
        volumeSlider.setMax(100);
        volumeSlider.setOnSeekBarChangeListener(
                new PlayerPrefSeekBarTracker(PlayerPref.alarmDefaultVolume,
                        volumeValue,
                        ServerString.ALARM_VOLUME.getLocalizedString(), 1)
        );
        alarms_volume.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AlarmsSettingsActivity.this, ServerString.ALARM_VOLUME_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });

        View alarms_snooze = findViewById(R.id.alarms_snooze);
        TextView snoozeValue = (TextView) alarms_snooze.findViewById(R.id.value);
        snoozeSlider = (SeekBar) alarms_snooze.findViewById(R.id.slider);
        snoozeSlider.setMax(30);
        snoozeSlider.setOnSeekBarChangeListener(
                new PlayerPrefSeekBarTracker(PlayerPref.alarmSnoozeSeconds,
                        snoozeValue,
                        ServerString.SETUP_SNOOZE_MINUTES.getLocalizedString(), 60)
        );
        alarms_snooze.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AlarmsSettingsActivity.this, ServerString.SETUP_SNOOZE_MINUTES_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });

        View alarms_timeout = findViewById(R.id.alarms_timeout);
        TextView timeoutValue = (TextView) alarms_timeout.findViewById(R.id.value);
        timeoutSlider = (SeekBar) alarms_timeout.findViewById(R.id.slider);
        timeoutSlider.setMax(90);
        timeoutSlider.setOnSeekBarChangeListener(
                new PlayerPrefSeekBarTracker(PlayerPref.alarmTimeoutSeconds,
                        timeoutValue,
                        ServerString.SETUP_ALARM_TIMEOUT.getLocalizedString(), 60)
        );
        alarms_timeout.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AlarmsSettingsActivity.this, ServerString.SETUP_ALARM_TIMEOUT_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });

        TextView alarms_fade_label = (TextView) findViewById(R.id.alarms_fade_label);
        alarms_fade_label.setText(ServerString.ALARM_FADE.getLocalizedString());
        alarmsFadeButton = new CompoundButtonWrapper((CompoundButton) findViewById(R.id.alarms_fade), new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                savePlayerPref(PlayerPref.alarmfadeseconds, isChecked ? 1 : 0);
            }
        });
    }

    private void updateViews() {
        volumeSlider.setProgress(getPlayerPref(PlayerPref.alarmDefaultVolume));
        snoozeSlider.setProgress(getPlayerPref(PlayerPref.alarmSnoozeSeconds) / 60);
        timeoutSlider.setProgress(getPlayerPref(PlayerPref.alarmTimeoutSeconds) / 60);
        alarmsFadeButton.setChecked(getPlayerPref(PlayerPref.alarmfadeseconds) > 0);
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        getService().registerPlayerPrefCallback(playerPrefCallback);
        getService().playerPref(PlayerPref.alarmfadeseconds);
        getService().playerPref(PlayerPref.alarmDefaultVolume);
        getService().playerPref(PlayerPref.alarmSnoozeSeconds);
        getService().playerPref(PlayerPref.alarmTimeoutSeconds);
    }

    public int getPlayerPref(PlayerPref playerPref) {
        Integer prefValue = playerPrefs.get(playerPref);
        return prefValue == null ? 0 : prefValue;
    }

    public void savePlayerPref(PlayerPref playerPref, int newValue) {
        playerPrefs.put(playerPref, newValue);
        getService().playerPref(playerPref, String.valueOf(newValue));
    }


    private Map<PlayerPref, Integer> playerPrefs = new HashMap<PlayerPref, Integer>();
    private final IServicePlayerPrefCallback playerPrefCallback = new IServicePlayerPrefCallback() {
        @Override
        public void onPlayerPrefReceived(final PlayerPref playerPref, final String value) {
            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    playerPrefs.put(playerPref, Integer.valueOf(value));
                    updateViews();
                }
            });
        }

        @Override
        public Object getClient() {
            return AlarmsSettingsActivity.this;
        }
    };

    private class PlayerPrefSeekBarTracker implements SeekBar.OnSeekBarChangeListener {
        private final TextView valueView;
        private final PlayerPref playerPref;
        private final String text;
        private final int factor;

        public PlayerPrefSeekBarTracker(PlayerPref playerPref, TextView valueView, String text, int factor) {
            this.valueView = valueView;
            this.playerPref = playerPref;
            this.text = text;
            this.factor = factor;
        }



        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            valueView.setText(text + " " + progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            getService().playerPref(playerPref, String.valueOf(seekBar.getProgress()*factor));
        }
    }
}
