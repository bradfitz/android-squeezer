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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Map.Entry;
import java.util.TreeMap;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.util.ScanNetworkTask;

/**
 * Scans the local network for servers, allow the user to choose one, set it as the preferred server
 * for this network, and optionally enter authentication information.
 * <p/>
 * A new network scan can be initiated manually if desired.
 */
public class ServerAddressView extends ScrollView implements ScanNetworkTask.ScanNetworkCallback {
    private final Preferences mPreferences;
    private final String mBssId;

    private final EditText mServerAddressEditText;
    private final Spinner mServersSpinner;
    private final EditText mUserNameEditText;
    private final EditText mPasswordEditText;
    private View mScanResults;
    private View mScanProgress;

    private ScanNetworkTask mScanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> mDiscoveredServers;

    private ArrayAdapter<String> mServersAdapter;

    public ServerAddressView(final Context context) {
        super(context);

        mPreferences = new Preferences(context);
        Preferences.ServerAddress serverAddress = mPreferences.getServerAddress();
        mBssId = serverAddress.bssId;

        inflate(context, R.layout.server_address_dialog, this);

        mServerAddressEditText = (EditText) findViewById(R.id.server_address);
        mUserNameEditText = (EditText) findViewById(R.id.username);
        mPasswordEditText = (EditText) findViewById(R.id.password);
        setServerAddress(serverAddress.address);

        // Set up the servers spinner.
        mServersAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
        mServersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServersSpinner = (Spinner) findViewById(R.id.found_servers);
        mServersSpinner.setAdapter(mServersAdapter);
        mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

        mScanResults = findViewById(R.id.scan_results);
        mScanProgress = findViewById(R.id.scan_progress);
        mScanProgress.setVisibility(View.GONE);
        Button scanButton = (Button) findViewById(R.id.scan_button);

        // Only support network scanning on WiFi.
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        boolean isWifi = ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
        if (isWifi) {
            startNetworkScan(context);
            scanButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    startNetworkScan(context);
                }
            });
        } else {
            TextView scan_msg = (TextView) findViewById(R.id.scan_msg);
            scan_msg.setText(context.getText(R.string.settings_server_scanning_disabled_msg));
            scanButton.setEnabled(false);
        }
    }

    public void savePreferences() {
        String address = mServerAddressEditText.getText().toString();

        // Append the default port if necessary.
        if (!address.contains(":")) {
            address += ":" + getResources().getInteger(R.integer.DefaultPort);
        }

        Preferences.ServerAddress serverAddress = mPreferences.saveServerAddress(address);

        final String serverName = getServerName(address);
        if (serverName != null) {
            mPreferences.saveServerName(serverAddress, serverName);
        }

        final String userName = mUserNameEditText.getText().toString();
        final String password = mPasswordEditText.getText().toString();
        mPreferences.saveUserCredentials(serverAddress, userName, password);
    }

    public void onDismiss() {
        // Stop scanning
        if (mScanNetworkTask != null) {
            mScanNetworkTask.cancel(true);
        }
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan(Context context) {
        mScanResults.setVisibility(View.GONE);
        mScanProgress.setVisibility(View.VISIBLE);
        mScanNetworkTask = new ScanNetworkTask(context, this);
        mScanNetworkTask.execute();
    }

    /**
     * Called when server scanning has finished.
     * @param mServerMap Discovered servers
     */
    public void onScanFinished(TreeMap<String, String> mServerMap) {
        mScanResults.setVisibility(View.VISIBLE);
        mServersSpinner.setVisibility(View.GONE);
        mScanProgress.setVisibility(View.GONE);

        if (mScanNetworkTask == null) {
            return;
        }

        mDiscoveredServers = mServerMap;
        mScanNetworkTask = null;

        switch (mDiscoveredServers.size()) {
            case 0:
                // Do nothing, no servers found.
                break;

            case 1:
                // Populate the edit text widget with the address found.
                setServerAddress(mDiscoveredServers.get(mDiscoveredServers.firstKey()));
                break;

            default:
                // Show the spinner so the user can choose a server.
                mServersAdapter.clear();
                for (Entry<String, String> e : mDiscoveredServers.entrySet()) {
                    mServersAdapter.add(e.getKey());
                }
                int position = getServerPosition(mServerAddressEditText.getText().toString());
                if (position >= 0) mServersSpinner.setSelection(position);
                mServersSpinner.setVisibility(View.VISIBLE);
                mServersAdapter.notifyDataSetChanged();
        }
    }

    private void setServerAddress(String address) {
        String currentHostPort = mServerAddressEditText.getText().toString();
        String currentHost = Util.parseHost(currentHostPort);
        int currentPort = Util.parsePort(currentHostPort);

        String host = Util.parseHost(address);
        int port = Util.parsePort(address);

        if (host.equals(currentHost)) {
            port = currentPort;
        }

        Preferences.ServerAddress serverAddress = new Preferences.ServerAddress();
        serverAddress.bssId = mBssId;
        serverAddress.address = host + ":" + port;

        mServerAddressEditText.setText(serverAddress.address);
        mUserNameEditText.setText(mPreferences.getUserName(serverAddress));
        mPasswordEditText.setText(mPreferences.getPassword(serverAddress));
    }

    private String getServerName(String ipPort) {
        if (mDiscoveredServers != null)
            for (Entry<String, String> entry : mDiscoveredServers.entrySet())
                if (ipPort.equals(entry.getValue()))
                    return entry.getKey();
        return null;
    }

    private int getServerPosition(String ipPort) {
        if (mDiscoveredServers != null) {
            String host = Util.parseHost(ipPort);
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
            setServerAddress(serverAddress);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

}
