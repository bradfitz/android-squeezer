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

package uk.org.ngo.squeezer.dialogs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Shows a preference dialog that allows the user to scan the local network
 * for servers, choose a server from the results of the scan, or enter the
 * name/address of a server directly.
 * <p>
 * As far as I can tell it's tricky to show a dialog from another dialog in
 * Android (without dismissing the first one), which makes showing a
 * ProgressDialog during the scan difficult.  Hence the gyrations to enable
 * and disable various dialog controls during the scan.
 *
 * @author nik
 *
 */
public class ServerAddressPreference extends DialogPreference {
    private EditText mServerAddressEditText;
    private Button mScanBtn;
    private ProgressBar mScanProgressBar;
    private Spinner mServersSpinner;

    private scanNetworkTask mScanTask;
    private boolean mScanInProgress = false;

    private final ConnectivityManager mConnectivityManager =
            (ConnectivityManager) Squeezer.getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

    private final ArrayList<String> mDiscoveredServers = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter;

    private final Context mContext;

    public ServerAddressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setDialogLayoutResource(R.layout.server_address_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mServerAddressEditText = (EditText) view.findViewById(R.id.server_address);
        mScanBtn = (Button) view.findViewById(R.id.scan_btn);
        mScanProgressBar = (ProgressBar) view.findViewById(R.id.scan_progress);
        mServersSpinner = (Spinner) view.findViewById(R.id.found_servers);

        // If there's no server address configured then set the default text
        // in the edit box to our IP address, trimmed of the last octet.
        String serveraddr = getPersistedString("");

        if (serveraddr.length() == 0) {
            WifiManager mWifiManager =
                    (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();

            String ipstr = Formatter.formatIpAddress(mWifiInfo.getIpAddress());
            int i = ipstr.lastIndexOf(".");
            serveraddr = ipstr.substring(0, ++i);
        }

        // Move the cursor to the end of the address.
        mServerAddressEditText.setText(serveraddr);
        mServerAddressEditText.setSelection(serveraddr.length());

        // Set up the servers spinner.
        mAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item,
                mDiscoveredServers);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

        // Only support network scanning on WiFi.
        if (mConnectivityManager.getActiveNetworkInfo().getType()
                == ConnectivityManager.TYPE_WIFI) {
            mScanBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mScanInProgress == false) {
                        onScanStart();
                    } else {
                        onScanFinish();
                    }
                }
            });
        } else {
            TextView scan_msg = (TextView) view.findViewById(R.id.scan_msg);
            scan_msg.setText(mContext.getText(R.string.settings_server_scanning_disabled_msg));
            mScanBtn.setEnabled(false);
        }
    }


    /**
     * Starts scanning for servers.
     * <p>
     * Disables and enables various parts of the UI.
     */
    void onScanStart() {
        Log.v("DIALOG", "Start scanning");

        // TODO: The Android way would appear to be to have an 'X' widget
        // on the right of the progress bar that would cancel the scan (and
        // hide the scan_button.
        mScanBtn.setText(R.string.settings_server_scan_stop);

        mServersSpinner.setVisibility(View.GONE);

        mScanProgressBar.setProgress(0);
        mScanProgressBar.setVisibility(View.VISIBLE);

        mServerAddressEditText.setEnabled(false);

        mScanTask = new scanNetworkTask();
        mScanTask.execute(mContext.getResources().getInteger(R.integer.DefaultPort));
        mScanInProgress = true;
    }

    /**
     * Called when server scanning has finished.
     * <p>
     * Adjusts the UI as necessary.
     */
    void onScanFinish() {
        mScanBtn.setText(R.string.settings_server_scan_start);
        mScanTask.cancel(true);

        mScanProgressBar.setVisibility(View.GONE);

        mServerAddressEditText.setEnabled(true);
        mScanInProgress = false;

        switch (mDiscoveredServers.size()) {
            case 0:
                // Do nothing, no servers found.
                break;
            case 1:
                // Populate the edit text widget with the address found.
                mServerAddressEditText.setText(mDiscoveredServers.get(0));
                break;
            default:
                // Show the spinner so the user can choose a server.
                mServersSpinner.setVisibility(View.VISIBLE);
                mServersSpinner.setAdapter(mAdapter);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // Stop scanning
        if (mScanInProgress) {
            mScanTask.cancel(true);
            mScanInProgress = false;
        }

        if (positiveResult) {
            String addr = mServerAddressEditText.getText().toString();
            persistString(addr);
            callChangeListener(addr);
        }
    }

    /**
     * Inserts the selected address in to the edittext widget.
     *
     * @author nik
     */
    public class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent,
                View view, int pos, long id) {
            mServerAddressEditText.setText(parent.getItemAtPosition(pos).toString());
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    /**
     * Scans the local network for servers.
     *
     * @author nik
     */
    private class scanNetworkTask extends AsyncTask<Integer, Integer, Integer> {
        String TAG = "scanNetworkTask";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mDiscoveredServers.clear();
        }

        /**
         * Performs a scan of the local network.
         *
         * @param ports An array of ports to scan on each host. Only the first
         *            port is scanned.
         */
        @Override
        protected Integer doInBackground(Integer... ports) {
            WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiManager.WifiLock wifiLock = wm.createWifiLock(TAG);

            Log.v(TAG, "Locking WiFi while scanning");
            wifiLock.acquire();

            WifiInfo wifiInfo = wm.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();

            // Dumb approach - go from .1 to .254, skipping ourselves.
            int subnet = ip & 0x00ffffff;
            int lastOctet;
            Socket socket;

            for (lastOctet = 1; lastOctet <= 254; lastOctet++) {
                int addressToCheck = (lastOctet << 24) | subnet;

                if (addressToCheck == ip)
                    continue;

                // Everything else prefers to deal with IP addresses as strings,
                // so convert here, and use throughout.
                String addrStr = Formatter.formatIpAddress(addressToCheck);
                Log.v(TAG, "Will check: " + addrStr);

                // Check for cancellation before starting any lengthy activity
                if (isCancelled())
                    break;

                // Try and connect. Ignore errors, on success close the
                // socket and note the IP address.
                socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(addrStr, ports[0]),
                            500 /* ms timeout */);
                } catch (IOException e) {
                } // Expected, can be ignored.

                if (socket.isConnected()) {
                    mDiscoveredServers.add(addrStr); // Note the address

                    // TODO: Indicate this in the UI somehow (increment a
                    // counter?)

                    try {
                        socket.close();
                    } catch (IOException e) {
                    } // Expected, can be ignored.

                }

                // Send message that we've checked this one.
                publishProgress(lastOctet);
            }

            Log.v(TAG, "Scanning complete, unlocking WiFi");
            wifiLock.release();
            return lastOctet;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mScanProgressBar.setProgress(values[0]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onScanFinish();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            onScanFinish();
        }
    }
}
