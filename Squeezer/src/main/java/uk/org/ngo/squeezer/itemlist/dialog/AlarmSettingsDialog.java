/*
 * Copyright (c) 2015 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.itemlist.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.model.Player;

/**
 * A dialog with controls to manage a player's default alarm preferences (volume, snooze duration,
 * etc).
 * <p>
 * Activities that host this dialog must implement {@link AlarmSettingsDialog.HostActivity}
 * to provide information about the preferences, and to save the new values when the user
 * selects the dialog's positive action button.
 */
public class AlarmSettingsDialog extends DialogFragment {
    private HostActivity mHostActivity;

    /** Activities that host this dialog must implement this interface. */
    public interface HostActivity {
        /**
         * @return The current player.
         */
        @NonNull
        Player getPlayer();

        /**
         * @param playerPref the name of the preference to get
         * @param def the default value to return if the preference does not exist
         * @return The value of the PlayerPref identified by <code>playerPref</code>
         */
        @NonNull
        String getPlayerPref(@NonNull @Player.Pref.Name String playerPref, @NonNull String def);

        /**
         * Called when the user selects the dialog's positive button.
         *
         * @param volume The user's chosen volume
         * @param snooze The user's chosen snooze duration, in seconds
         * @param timeout The user's chosen timeout duration, in seconds
         * @param fade Whether alarms should fade up
         */
        void onPositiveClick(int volume, int snooze, int timeout, boolean fade);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mHostActivity = (HostActivity) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement HostActivity");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint({"InflateParams"})
        final View view = getActivity().getLayoutInflater().inflate(R.layout.alarm_settings_dialog, null);

        final TextView alarmVolumeHint = view.findViewById(R.id.alarm_volume_hint);
        final TextView alarmSnoozeHint = view.findViewById(R.id.alarm_snooze_hint);
        final TextView alarmTimeoutHint = view.findViewById(R.id.alarm_timeout_hint);
        final TextView alarmFadeHint = view.findViewById(R.id.alarm_fade_hint);

        final SeekBar alarmVolume = view.findViewById(R.id.alarm_volume_seekbar);
        final SeekBar alarmSnooze = view.findViewById(R.id.alarm_snooze_seekbar);
        final SeekBar alarmTimeout = view.findViewById(R.id.alarm_timeout_seekbar);

        final CompoundButton alarmFadeToggle = view.findViewById(R.id.alarm_fade);

        alarmVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alarmVolumeHint.setText(String.format("%d%%", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        alarmSnooze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alarmSnoozeHint.setText(getResources().getQuantityString(R.plurals.alarm_snooze_hint_text,
                        progress, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        alarmTimeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    alarmTimeoutHint.setText(R.string.alarm_timeout_hint_text_zero);
                } else {
                    alarmTimeoutHint.setText(getResources().getQuantityString(R.plurals.alarm_timeout_hint_text,
                            progress, progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        alarmFadeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarmFadeHint.setText(isChecked ? R.string.alarm_fade_on_text : R.string.alarm_fade_off_text);
            }
        });

        alarmVolume.setProgress(Integer.valueOf(mHostActivity.getPlayerPref(Player.Pref.ALARM_DEFAULT_VOLUME, "50")));
        alarmSnooze.setProgress(Integer.valueOf(mHostActivity.getPlayerPref(Player.Pref.ALARM_SNOOZE_SECONDS, "600")) / 60);
        alarmTimeout.setProgress(Integer.valueOf(mHostActivity.getPlayerPref(Player.Pref.ALARM_TIMEOUT_SECONDS, "300")) / 60);
        alarmFadeToggle.setChecked("1".equals(mHostActivity.getPlayerPref(Player.Pref.ALARM_FADE_SECONDS, "0")));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setView(view);
        builder.setTitle(getResources().getString(R.string.alarms_settings_dialog_title, mHostActivity.getPlayer().getName()));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHostActivity.onPositiveClick(alarmVolume.getProgress(), alarmSnooze.getProgress() * 60,
                        alarmTimeout.getProgress() * 60, alarmFadeToggle.isChecked());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }
}
