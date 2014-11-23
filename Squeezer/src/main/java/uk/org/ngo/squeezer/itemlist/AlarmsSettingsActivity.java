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
import android.support.annotation.NonNull;
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
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;

public class AlarmsSettingsActivity extends BaseActivity {

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
        SeekBar volumeSlider = (SeekBar) alarms_volume.findViewById(R.id.slider);
        volumeSlider.setMax(100);
        SliderPlayerPref volumePref = new SliderPlayerPref(PlayerPref.alarmDefaultVolume, 1, volumeSlider);
        volumeSlider.setOnSeekBarChangeListener(
                new PlayerPrefSeekBarTracker(volumePref, volumeValue, ServerString.ALARM_VOLUME.getLocalizedString())
        );
        playerPrefs.put(PlayerPref.alarmDefaultVolume, volumePref);
        alarms_volume.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AlarmsSettingsActivity.this, ServerString.ALARM_VOLUME_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });

        View alarms_snooze = findViewById(R.id.alarms_snooze);
        TextView snoozeValue = (TextView) alarms_snooze.findViewById(R.id.value);
        SeekBar snoozeSlider = (SeekBar) alarms_snooze.findViewById(R.id.slider);
        snoozeSlider.setMax(30);
        SliderPlayerPref snoozePref = new SliderPlayerPref(PlayerPref.alarmSnoozeSeconds, 60, snoozeSlider);
        snoozeSlider.setOnSeekBarChangeListener(
                new PlayerPrefSeekBarTracker(snoozePref, snoozeValue, ServerString.SETUP_SNOOZE_MINUTES.getLocalizedString())
        );
        playerPrefs.put(PlayerPref.alarmSnoozeSeconds, snoozePref);
        alarms_snooze.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AlarmsSettingsActivity.this, ServerString.SETUP_SNOOZE_MINUTES_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });

        View alarms_timeout = findViewById(R.id.alarms_timeout);
        TextView timeoutValue = (TextView) alarms_timeout.findViewById(R.id.value);
        SeekBar timeoutSlider = (SeekBar) alarms_timeout.findViewById(R.id.slider);
        timeoutSlider.setMax(90);
        SliderPlayerPref timeoutPref = new SliderPlayerPref(PlayerPref.alarmTimeoutSeconds, 60, timeoutSlider);
        timeoutSlider.setOnSeekBarChangeListener(
                new PlayerPrefSeekBarTracker(timeoutPref, timeoutValue, ServerString.SETUP_ALARM_TIMEOUT.getLocalizedString())
        );
        playerPrefs.put(PlayerPref.alarmTimeoutSeconds, timeoutPref);
        alarms_timeout.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AlarmsSettingsActivity.this, ServerString.SETUP_ALARM_TIMEOUT_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });

        TextView alarms_fade_label = (TextView) findViewById(R.id.alarms_fade_label);
        alarms_fade_label.setText(ServerString.ALARM_FADE.getLocalizedString());
        alarmsFadeButton = new CompoundButtonWrapper((CompoundButton) findViewById(R.id.alarms_fade));
        playerPrefs.put(PlayerPref.alarmfadeseconds, new CompoundButtonPlayerPref(alarmsFadeButton));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        alarmsFadeButton.setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (getService() != null) {
                    getService().playerPref(PlayerPref.alarmfadeseconds, isChecked ? "1" : "0");
                }
            }
        });
    }

    @Override
    protected void registerCallback(@NonNull ISqueezeService service) {
        super.registerCallback(service);
        service.registerPlayerPrefCallback(playerPrefCallback);
        service.playerPref(PlayerPref.alarmfadeseconds);
        service.playerPref(PlayerPref.alarmDefaultVolume);
        service.playerPref(PlayerPref.alarmSnoozeSeconds);
        service.playerPref(PlayerPref.alarmTimeoutSeconds);

        for (PlayerPrefCallbacks callbacks : playerPrefs.values())
            callbacks.setEnabled(false);
    }

    private final IServicePlayerPrefCallback playerPrefCallback = new IServicePlayerPrefCallback() {
        @Override
        public void onPlayerPrefReceived(final PlayerPref playerPref, final String value) {
            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    PlayerPrefCallbacks callbacks = playerPrefs.get(playerPref);
                    callbacks.setEnabled(true);
                    callbacks.updateView(Integer.valueOf(value));
                }
            });
        }

        @Override
        public Object getClient() {
            return AlarmsSettingsActivity.this;
        }
    };

    private class PlayerPrefSeekBarTracker implements SeekBar.OnSeekBarChangeListener {
        private final SliderPlayerPref sliderPref;
        private final TextView valueView;
        private final String text;

        public PlayerPrefSeekBarTracker(SliderPlayerPref sliderPref, TextView valueView, String text) {
            this.valueView = valueView;
            this.sliderPref = sliderPref;
            this.text = text;
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
            if (getService() != null) {
                getService().playerPref(sliderPref.playerPref, String.valueOf(seekBar.getProgress() * sliderPref.factor));
            }
        }
    }

    private Map<PlayerPref, PlayerPrefCallbacks> playerPrefs = new HashMap<PlayerPref, PlayerPrefCallbacks>();
    private interface PlayerPrefCallbacks {
        void setEnabled(boolean enabled);
        void updateView(int value);
    }

    private class SliderPlayerPref implements PlayerPrefCallbacks {
        private PlayerPref playerPref;
        private final int factor;
        private final SeekBar slider;

        private SliderPlayerPref(PlayerPref playerPref, int factor, SeekBar slider) {
            this.playerPref = playerPref;
            this.factor = factor;
            this.slider = slider;
        }

        @Override
        public void setEnabled(boolean enabled) {
            slider.setEnabled(enabled);
        }

        @Override
        public void updateView(int value) {
            slider.setProgress(value / factor);
        }
    }

    private class CompoundButtonPlayerPref implements PlayerPrefCallbacks {
        private final CompoundButtonWrapper view;

        private CompoundButtonPlayerPref(CompoundButtonWrapper view) {
            this.view = view;
        }

        @Override
        public void setEnabled(boolean enabled) {
            view.setEnabled(enabled);
        }

        @Override
        public void updateView(int value) {
            view.setChecked(value > 0);
        }
    }

}
