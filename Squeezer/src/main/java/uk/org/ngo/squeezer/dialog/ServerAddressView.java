/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.dialog;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Map.Entry;
import java.util.TreeMap;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.util.ScanNetworkTask;

/**
 * Scans the local network for servers, allow the user to choose one, set it as the preferred server
 * for this network, and optionally enter authentication information.
 * <p>
 * A new network scan can be initiated manually if desired.
 */
public class ServerAddressView extends LinearLayout implements ScanNetworkTask.ScanNetworkCallback {
    private Preferences mPreferences;
    private Preferences.ServerAddress mServerAddress;

    private RadioButton mSqueezeNetworkButton;
    private RadioButton mLocalServerButton;
    private EditText mServerAddressEditText;
    private TextView mServerName;
    private Spinner mServersSpinner;
    private EditText mUserNameEditText;
    private EditText mPasswordEditText;
    private View mScanResults;
    private View mScanProgress;

    private ScanNetworkTask mScanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> mDiscoveredServers;

    private ArrayAdapter<String> mServersAdapter;

    public ServerAddressView(final Context context) {
        super(context);
        initialize(context);
    }

    public ServerAddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(final Context context) {
        inflate(context, R.layout.server_address_view, this);
        if (!isInEditMode()) {
            mPreferences = new Preferences(context);
            mServerAddress = mPreferences.getServerAddress();
            if (mServerAddress.localAddress() == null) {
                Preferences.ServerAddress cliServerAddress = mPreferences.getCliServerAddress();
                if (cliServerAddress.localAddress() != null) {
                    mServerAddress.setAddress(cliServerAddress.localHost());
                }
            }

            mSqueezeNetworkButton = findViewById(R.id.squeezeNetwork);
            mLocalServerButton = findViewById(R.id.squeezeServer);

            mServerAddressEditText = findViewById(R.id.server_address);
            mUserNameEditText = findViewById(R.id.username);
            mPasswordEditText = findViewById(R.id.password);

            final OnClickListener onNetworkSelected = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setSqueezeNetwork(view.getId() == R.id.squeezeNetwork);
                }
            };
            mSqueezeNetworkButton.setOnClickListener(onNetworkSelected);
            mLocalServerButton.setOnClickListener(onNetworkSelected);

            // Set up the servers spinner.
            mServersAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
            mServersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mServerName = findViewById(R.id.server_name);
            mServersSpinner = findViewById(R.id.found_servers);
            mServersSpinner.setAdapter(mServersAdapter);

            mScanResults = findViewById(R.id.scan_results);
            mScanProgress = findViewById(R.id.scan_progress);
            mScanProgress.setVisibility(GONE);
            TextView scanDisabledMessage = findViewById(R.id.scan_disabled_msg);

            setSqueezeNetwork(mServerAddress.squeezeNetwork);
            setServerAddress(mServerAddress.localAddress());

            // Only support network scanning on WiFi.
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            boolean isWifi = ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
            if (isWifi) {
                scanDisabledMessage.setVisibility(GONE);
                startNetworkScan(context);
                Button scanButton = findViewById(R.id.scan_button);
                scanButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        startNetworkScan(context);
                    }
                });
            } else {
                mScanResults.setVisibility(GONE);
            }
        }
    }

    public void savePreferences() {
        mServerAddress.squeezeNetwork = mSqueezeNetworkButton.isChecked();
        String address = mServerAddressEditText.getText().toString();
        mServerAddress.setAddress(address);
        mPreferences.saveServerAddress(mServerAddress);

        mPreferences.saveServerName(mServerAddress, getServerName(address));

        String username = mUserNameEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        mPreferences.saveUserCredentials(mServerAddress, username, password);
    }

    @Override
    protected void onDetachedFromWindow() {
        // Stop scanning
        if (mScanNetworkTask != null) {
            mScanNetworkTask.cancel();
        }

        super.onDetachedFromWindow();
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan(Context context) {
        mScanResults.setVisibility(GONE);
        mScanProgress.setVisibility(VISIBLE);
        mScanNetworkTask = new ScanNetworkTask(context, this);
        new Thread(mScanNetworkTask).start();
    }

    /**
     * Called when server scanning has finished.
     * @param serverMap Discovered servers, key is the server name, value is the IP address.
     */
    public void onScanFinished(TreeMap<String, String> serverMap) {
        mScanResults.setVisibility(VISIBLE);
        mServerName.setVisibility(GONE);
        mServersSpinner.setVisibility(GONE);
        mScanProgress.setVisibility(GONE);
        mServersAdapter.clear();

        if (mScanNetworkTask == null) {
            return;
        }

        mDiscoveredServers = serverMap;

        mScanNetworkTask = null;

        if (mDiscoveredServers.size() == 0) {
            // No servers found, manually enter address
            // Populate the edit text widget with current address stored in preferences.
            setServerAddress(mServerAddress.localAddress());
            mServerAddressEditText.setEnabled(true);
            mServerName.setVisibility(VISIBLE);
        } else {
            // Show the spinner so the user can choose a server or to manually enter address.
            // Don't fire onItemSelected by calling notifyDataSetChanged and
            // setSelection(pos, false) before setting OnItemSelectedListener
            mServersSpinner.setOnItemSelectedListener(null);

            for (Entry<String, String> e : mDiscoveredServers.entrySet()) {
                mServersAdapter.add(e.getKey());
            }
            mServersAdapter.add(getContext().getString(R.string.settings_manual_serveraddr));
            mServersAdapter.notifyDataSetChanged();

            // First look the stored server name in the list of found servers
            String addressOfStoredServerName = mDiscoveredServers.get(mPreferences.getServerName(mServerAddress));
            int position = getServerPosition(addressOfStoredServerName);

            // If that fails, look for the stored server address in the list of found servers
            if (position < 0) {
                position = getServerPosition(mServerAddress.localAddress());
            }

            mServersSpinner.setSelection((position < 0 ? mServersAdapter.getCount() - 1 : position), false);
            mServerAddressEditText.setEnabled(position < 0 && !mServerAddress.squeezeNetwork);

            mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
            mServersSpinner.setVisibility(VISIBLE);
        }
    }

    private void setSqueezeNetwork(boolean isSqueezeNetwork) {
        mSqueezeNetworkButton.setChecked(isSqueezeNetwork);
        mLocalServerButton.setChecked(!isSqueezeNetwork);
        setEditServerAddressAvailability(isSqueezeNetwork);
        mUserNameEditText.setEnabled(!isSqueezeNetwork);
        mPasswordEditText.setEnabled(!isSqueezeNetwork);
    }

    private void setServerAddress(String address) {
        mServerAddress.setAddress(address);

        mServerAddressEditText.setText(mServerAddress.localAddress());
        mUserNameEditText.setText(mPreferences.getUsername(mServerAddress));
        mPasswordEditText.setText(mPreferences.getPassword(mServerAddress));
    }

    private void setEditServerAddressAvailability(boolean isSqueezeNetwork) {
        if (isSqueezeNetwork) {
            mServerAddressEditText.setEnabled(false);
        } else if (mServersAdapter.getCount() == 0) {
            mServerAddressEditText.setEnabled(true);
        } else {
            mServerAddressEditText.setEnabled(mServersSpinner.getSelectedItemPosition() == mServersSpinner.getCount() - 1);
        }
    }

    private String getServerName(String ipPort) {
        if (mDiscoveredServers != null)
            for (Entry<String, String> entry : mDiscoveredServers.entrySet())
                if (ipPort.equals(entry.getValue()))
                    return entry.getKey();
        return null;
    }

    private int getServerPosition(String host) {
        if (host != null && mDiscoveredServers != null) {
            int position = 0;
            for (Entry<String, String> entry : mDiscoveredServers.entrySet()) {
                if (host.equals(entry.getValue()))
                    return position;
                position++;
            }
        }
        return -1;
    }

    /**
     * Inserts the selected address in to the edit text widget.
     */
    private class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String serverAddress = mDiscoveredServers.get(parent.getItemAtPosition(pos).toString());
            setSqueezeNetwork(false);
            setServerAddress(serverAddress);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

}
