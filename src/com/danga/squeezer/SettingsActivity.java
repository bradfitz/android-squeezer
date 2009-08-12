package com.danga.squeezer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {
	private final String TAG = "SettingsActivity";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(Preferences.NAME);
        addPreferencesFromResource(R.xml.preferences);

        // Both not yet implemented, so disable.  TODO(bradfitz): implement.
        CheckBoxPreference autoDiscoverPref = (CheckBoxPreference) findPreference(Preferences.KEY_AUTO_DISCOVER);
        autoDiscoverPref.setEnabled(false);
        CheckBoxPreference autoConnectPref = (CheckBoxPreference) findPreference(Preferences.KEY_AUTO_CONNECT);
        autoConnectPref.setEnabled(false);

        EditTextPreference addrPref = (EditTextPreference) findPreference(Preferences.KEY_SERVERADDR);
        addrPref.setOnPreferenceChangeListener(this);
        
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        String currentCliPort = preferences.getString(
                Preferences.KEY_SERVERADDR, "");
        if (currentCliPort.length() > 0) {
            addrPref.setSummary(currentCliPort);
        } else {
            addrPref.setSummary("IP & Port to the SqueezeCenter CLI. e.g. 10.0.0.5:9090");
        }
    }

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		final String key = preference.getKey();
		Log.d(TAG, "preference change for: " + key);
		if (Preferences.KEY_SERVERADDR.equals(key)) {
			final String ipPort = newValue.toString();
			// TODO: check that it looks valid?
			preference.setSummary(ipPort);
			return true;
		}
		return false;
	}
	
	static void show(Context context) {
        final Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
}
