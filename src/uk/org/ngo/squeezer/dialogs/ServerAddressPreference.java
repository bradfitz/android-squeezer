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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.acra.ErrorReporter;

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

    /** Map server names to IP addresses. */
    private final TreeMap<String, String> mDiscoveredServers = new TreeMap<String, String>();

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
        mAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item);
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
        mScanTask.execute();
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
                mServerAddressEditText
                        .setText(mDiscoveredServers.get(mDiscoveredServers.firstKey()));
                break;
            default:
                // Show the spinner so the user can choose a server.
                mAdapter.clear();
                for (Entry<String, String> e : mDiscoveredServers.entrySet()) {
                    mAdapter.add(e.getKey());
                }
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
            mServerAddressEditText.setText(mDiscoveredServers.get(parent.getItemAtPosition(pos)
                    .toString()));
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
    private class scanNetworkTask extends AsyncTask<Void, Long, Void> {
        String TAG = "scanNetworkTask";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mDiscoveredServers.clear();
        }

        /**
         * Discover Squeezerservers on the local network.
         * <p>
         * Do this by sending 20 UDP broadcasts to port 3483 at approximately
         * 500ms intervals. Squeezeservers are supposed to listen for this, and
         * respond with a packet that starts 'E' and some information about the
         * server, including its name.
         * <p>
         * Map the name to an IP address and store in mDiscoveredServers for
         * later use.
         * <p>
         * See the Slim::Networking::Discovery module in Squeezeserver for more
         * details.
         */
        @Override
        protected Void doInBackground(Void... unused) {
            WifiManager.WifiLock wifiLock = null;
            DatagramSocket socket = null;

            // UDP broadcast data that causes Squeezeservers to reply.  The format
            // is 'e', followed by null-terminated tags that indicate the data to
            // return.
            //
            // The Squeezeserver uses the size of the request packet to determine
            // the size of the response packet.

            byte[] request = {
                    'e', // 'existence' ?
                    'I', 'P', 'A', 'D', 0, // Include IP address
                    'N', 'A', 'M', 'E', 0, // Include server name
                    'J', 'S', 'O', 'N', 0, // Include server port
            };
            byte[] data = new byte[512];
            System.arraycopy(request, 0, data, 0, request.length);

            WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            wifiLock = wm.createWifiLock(TAG);

            Log.v(TAG, "Locking WiFi while scanning");
            wifiLock.acquire();

            try {
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");

                socket = new DatagramSocket();// 3483);
                DatagramPacket discoveryPacket = new DatagramPacket(data, data.length, broadcastAddr, 3483);

                byte[] buf = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);

                // Wait up to 500ms after each send, and scan for 10 seconds in
                // total.
                socket.setSoTimeout(500);
                long startTime = System.currentTimeMillis();
                long endTime = startTime + 10000;
                long now;

                socket.send(discoveryPacket);

                while ((now = System.currentTimeMillis()) <= endTime) {
                    boolean timedOut = false;

                    try {
                        socket.receive(responsePacket);
                    } catch (IOException e) {
                        timedOut = true;
                    }

                    if (!timedOut) {
                        if (buf[0] == (byte) 'E') {
                            String serverAddr = responsePacket.getAddress().getHostAddress();

                            // Blocks of data are TAG/LENGTH/VALUE, where TAG is
                            // a 4 byte string identifying the item, LENGTH is the
                            // length of the VALUE (e.g., reading \t means the value
                            // is 9 bytes, and VALUE is the actual value.

                            // Find the 'NAME' block
                            int i = 1;
                            while (buf[i] != 'N') {
                                i += 4;
                                i += buf[i] + 2;
                            }

                            // Now at first block that starts 'N', should be
                            // NAME.
                            i += 4;

                            // i now pointing at the length of the NAME value.
                            String name = new String(buf, i+1, buf[i]);
                            mDiscoveredServers.put(name, serverAddr);
                        }
                    } else {
                        socket.send(discoveryPacket);
                    }

                    publishProgress((now - startTime) / 500);
                }
            } catch (SocketException e) {
                // new DatagramSocket(3483)
                ErrorReporter.getInstance().handleException(e);
            } catch (UnknownHostException e) {
                // InetAddress.getByName()
                ErrorReporter.getInstance().handleException(e);
            } catch (IOException e) {
                // socket.send()
                ErrorReporter.getInstance().handleException(e);
            }

            if (socket != null)
                socket.close();

            Log.v(TAG, "Scanning complete, unlocking WiFi");
            if (wifiLock != null)
                wifiLock.release();

            // For testing that multiple servers are handled correctly.
            // mDiscoveredServers.put("Dummy", "127.0.0.1");
            return null;
        }

        /**
         * Update the progress bar. The main progress value corresponds to how
         * many servers have been discovered (up to 20), the secondary progress
         * value corresponds to how far through the 10 second discovery process
         * we are, in 500ms chunks.
         */
        @Override
        protected void onProgressUpdate(Long... values) {
            mScanProgressBar.setSecondaryProgress(values[0].intValue());
            mScanProgressBar.setProgress(Math.min(mDiscoveredServers.size(), 20));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onScanFinish();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            onScanFinish();
        }
    }
}
