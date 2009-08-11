package com.danga.squeezeremote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
        
        EditTextPreference addrPref = (EditTextPreference) findPreference(Preferences.KEY_SERVERADDR);
        addrPref.setSummary("ip:port");
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
