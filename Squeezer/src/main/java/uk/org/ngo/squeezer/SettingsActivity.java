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

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.Scrobble;
import uk.org.ngo.squeezer.util.ThemeManager;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener, OnSharedPreferenceChangeListener {

    private final String TAG = "SettingsActivity";

    private static final int DIALOG_SCROBBLE_APPS = 0;

    private ISqueezeService service = null;

    private IntEditTextPreference fadeInPref;

    private final ThemeManager mThemeManager = new ThemeManager();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SettingsActivity.this.service = (ISqueezeService) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeManager.onCreate(this);
        super.onCreate(savedInstanceState);

        bindService(new Intent(this, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; service = " + service);

        getPreferenceManager().setSharedPreferencesName(Preferences.NAME);
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        Preferences preferences = new Preferences(this, sharedPreferences);

        fadeInPref = (IntEditTextPreference) findPreference(Preferences.KEY_FADE_IN_SECS);
        fadeInPref.setOnPreferenceChangeListener(this);
        updateFadeInSecondsSummary(sharedPreferences.getInt(Preferences.KEY_FADE_IN_SECS, 0));

        CheckBoxPreference autoConnectPref = (CheckBoxPreference) findPreference(
                Preferences.KEY_AUTO_CONNECT);
        autoConnectPref.setChecked(sharedPreferences.getBoolean(Preferences.KEY_AUTO_CONNECT, true));

        fillScrobblePreferences(sharedPreferences);

        fillThemeSelectionPreferences();

        CheckBoxPreference startSqueezePlayerPref = (CheckBoxPreference) findPreference(
                Preferences.KEY_SQUEEZEPLAYER_ENABLED);
        startSqueezePlayerPref.setChecked(sharedPreferences.getBoolean(Preferences.KEY_SQUEEZEPLAYER_ENABLED, true));
    }

    private void fillScrobblePreferences(SharedPreferences preferences) {
        CheckBoxPreference scrobblePref = (CheckBoxPreference) findPreference(
                Preferences.KEY_SCROBBLE_ENABLED);
        scrobblePref.setOnPreferenceChangeListener(this);

        if (!Scrobble.canScrobble()) {
            scrobblePref.setSummaryOff(getString(R.string.settings_scrobble_noapp));
            scrobblePref.setChecked(false);
        } else {
            scrobblePref.setSummaryOff(getString(R.string.settings_scrobble_off));

            scrobblePref
                    .setChecked(preferences.getBoolean(Preferences.KEY_SCROBBLE_ENABLED, false));

            // If an old KEY_SCROBBLE preference exists, use it, delete it, and
            // upgrade it to the new KEY_SCROBBLE_ENABLED preference.
            if (preferences.contains(Preferences.KEY_SCROBBLE)) {
                boolean enabled = (Integer.parseInt(
                        preferences.getString(Preferences.KEY_SCROBBLE, "0")) > 0);
                scrobblePref.setChecked(enabled);
                Editor editor = preferences.edit();
                editor.putBoolean(Preferences.KEY_SCROBBLE_ENABLED, enabled);
                editor.remove(Preferences.KEY_SCROBBLE);
                editor.apply();
            }
        }
    }

    private void fillThemeSelectionPreferences() {
        ListPreference onSelectThemePref = (ListPreference) findPreference(Preferences.KEY_ON_THEME_SELECT_ACTION);
        ArrayList<String> entryValues = new ArrayList<>();
        ArrayList<String> entries = new ArrayList<>();

        for (ThemeManager.Theme theme : ThemeManager.Theme.values()) {
            entryValues.add(theme.name());
            entries.add(theme.getText(this));
        }

        onSelectThemePref.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
        onSelectThemePref.setEntries(entries.toArray(new String[entries.size()]));
        onSelectThemePref.setDefaultValue(ThemeManager.getDefaultTheme().name());
        if (onSelectThemePref.getValue() == null) {
            onSelectThemePref.setValue(ThemeManager.getDefaultTheme().name());
        } else {
            try {
                ThemeManager.Theme t = ThemeManager.Theme.valueOf(onSelectThemePref.getValue());
            } catch (Exception e) {
                onSelectThemePref.setValue(ThemeManager.getDefaultTheme().name());
            }
        }
        onSelectThemePref.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary(onSelectThemePref, onSelectThemePref.getValue());
    }


    @Override
    public void onResume() {
        super.onResume();
        mThemeManager.onResume(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    private void updateFadeInSecondsSummary(int fadeInSeconds) {
        if (fadeInSeconds == 0) {
            fadeInPref.setSummary(R.string.disabled);
        } else {
            fadeInPref.setSummary(fadeInSeconds + " " + getResources()
                    .getQuantityString(R.plurals.seconds, fadeInSeconds));
        }
    }

    /**
     * Explicitly set the preference's summary based on the value for the selected item.
     * <p>
     * Work around a bug in ListPreference on devices running earlier API versions (not
     * sure when the bug starts) where the preference summary string is not automatically
     * updated when the preference changes. See http://stackoverflow.com/a/7018053/775306
     * for details.
     *
     * @param pref the preference to set
     * @param value the preference's value (might not be set yet)
     */
    private void updateListPreferenceSummary(ListPreference pref, String value) {
        CharSequence[] entries = pref.getEntries();
        int index = pref.findIndexOfValue(value);
        if (index != -1) pref.setSummary(entries[index]);
    }

    /**
     * A preference has been changed by the user, but has not yet been persisted.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        Log.v(TAG, "preference change for: " + key);

        if (Preferences.KEY_FADE_IN_SECS.equals(key)) {
            updateFadeInSecondsSummary(Util.getInt(newValue.toString()));
        }

        if (Preferences.KEY_ON_THEME_SELECT_ACTION.equals(key)) {
            updateListPreferenceSummary((ListPreference) preference, (String) newValue);
        }

        // If the user has enabled Scrobbling but we don't think it will work
        // pop up a dialog with links to Google Play for apps to install.
        if (Preferences.KEY_SCROBBLE_ENABLED.equals(key)) {
            if (newValue.equals(true) && !Scrobble.canScrobble()) {
                showDialog(DIALOG_SCROBBLE_APPS);

                // User hit back, or similar (or maybe went to install an
                // app). Check again to see if scrobbling will work.
                if (!Scrobble.canScrobble()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * A preference has been changed by the user and is going to be persisted.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(TAG, "Preference changed: " + key);

        if (service != null) {
            service.preferenceChanged(key);
        } else {
            Log.v(TAG, "service is null!");
        }
    }

    @Override
    @Deprecated
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_SCROBBLE_APPS:
                final CharSequence[] apps = {
                        "Last.fm", "ScrobbleDroid", "SLS"
                };
                final CharSequence[] urls = {
                        "fm.last.android", "net.jjc1138.android.scrobbler",
                        "com.adam.aslfms"
                };

                final int[] icons = {
                        R.drawable.ic_launcher_lastfm,
                        R.drawable.ic_launcher_scrobbledroid, R.drawable.ic_launcher_sls
                };

                dialog = new Dialog(this);
                dialog.setContentView(R.layout.scrobbler_choice_dialog);
                dialog.setTitle("Scrobbling applications");

                ListView appList = dialog.findViewById(R.id.scrobble_apps);
                appList.setAdapter(new IconRowAdapter(this, apps, icons));

                final Context context = dialog.getContext();
                appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + urls[position]));
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context, R.string.settings_market_not_found,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        }

        return dialog;
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
}
