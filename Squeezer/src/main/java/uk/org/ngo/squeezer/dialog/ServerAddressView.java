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
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.TreeMap;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.util.Logger;

/**
 * Scans the local network for servers, allow the user to choose one, set it as the preferred server
 * for this network, and optionally enter authentication information.
 * <p/>
 * A new network scan can be initiated manually if desired.
 */
public class ServerAddressView extends ScrollView {
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
    void onScanFinished(TreeMap<String, String> mServerMap) {
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
                mServersSpinner.setVisibility(View.VISIBLE);
                mServersAdapter.notifyDataSetChanged();
        }
    }

    private void setServerAddress(String address) {
        Preferences.ServerAddress serverAddress = new Preferences.ServerAddress();
        serverAddress.bssId = mBssId;
        serverAddress.address = address;

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

    /**
     * Scans the local network for servers.
     */
    static class ScanNetworkTask extends AsyncTask<Void, Void, Void> {

        private final String TAG = "scanNetworkTask";

        private final Context mContext;

        private final ServerAddressView mPref;

        /**
         * Map server names to IP addresses.
         */
        private final TreeMap<String, String> mServerMap = new TreeMap<String, String>();

        /**
         * UDP port to broadcast discovery requests to.
         */
        private final int DISCOVERY_PORT = 3483;

        /**
         * Maximum time to wait between discovery attempts (ms).
         */
        private final int DISCOVERY_ATTEMPT_TIMEOUT = 1000;

        ScanNetworkTask(Context context, ServerAddressView pref) {
            mContext = context;
            mPref = pref;
        }

        /**
         * Discover Squeeze servers on the local network.
         * <p/>
         * Do this by sending MAX_DISCOVERY_ATTEMPT UDP broadcasts to port 3483 at approximately
         * DISCOVERY_ATTEMPT_TIMEOUT intervals. Squeeze servers are supposed to listen for this, and
         * respond with a packet that starts 'E' and some information about the server, including
         * its name.
         * <p/>
         * Map the name to an IP address and store in mDiscoveredServers for later use.
         * <p/>
         * See the Slim::Networking::Discovery module in Squeeze server for more details.
         */
        @Override
        protected Void doInBackground(Void... unused) {
            WifiManager.WifiLock wifiLock;
            DatagramSocket socket = null;

            // UDP broadcast data that causes Squeeze servers to reply. The
            // format is 'e', followed by null-terminated tags that indicate the
            // data to return.
            //
            // The Squeeze server uses the size of the request packet to
            // determine the size of the response packet.

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

            // mServerMap.put("Dummy", "127.0.0.1");

            Log.v(TAG, "Locking WiFi while scanning");
            wifiLock.acquire();

            try {
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                boolean timedOut;
                socket = new DatagramSocket();
                DatagramPacket discoveryPacket = new DatagramPacket(data, data.length,
                        broadcastAddress, DISCOVERY_PORT);

                byte[] buf = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);

                socket.setSoTimeout(DISCOVERY_ATTEMPT_TIMEOUT);
                socket.send(discoveryPacket);
                timedOut = false;
                while (!timedOut) {
                    if (isCancelled()) {
                        break;
                    }
                    try {
                        socket.receive(responsePacket);
                        if (buf[0] == (byte) 'E') {
                            // There's no mechanism for the server to return the port
                            // the CLI is listening on, so assume it's the default and
                            // append it to the address.
                            String ipPort = responsePacket.getAddress().getHostAddress() + ":" +
                                    mContext.getResources().getInteger(R.integer.DefaultPort);

                            // Blocks of data are TAG/LENGTH/VALUE, where TAG is
                            // a 4 byte string identifying the item, LENGTH is
                            // the length of the VALUE (e.g., reading \t means
                            // the value is 9 bytes, and VALUE is the actual
                            // value.

                            // Find the 'NAME' block
                            int i = 1;
                            while (i < buf.length && buf[i] != 'N') {
                                i += 4;
                                i += buf[i] + 2;
                            }

                            // Now at first block that starts 'N', should be
                            // NAME.
                            i += 4;

                            // i now pointing at the length of the NAME value.
                            String name = new String(buf, i + 1, buf[i]);

                            publishProgress();
                            mServerMap.put(name, ipPort);
                        }
                    } catch (IOException e) {
                        timedOut = true;
                    }
                }

            } catch (SocketException e) {
                // new DatagramSocket(3483)
                Logger.logException(e);
            } catch (UnknownHostException e) {
                // InetAddress.getByName()
                Logger.logException(e);
            } catch (IOException e) {
                // socket.send()
                Logger.logException(e);
            }

            if (socket != null) {
                socket.close();
            }

            Log.v(TAG, "Scanning complete, unlocking WiFi");
            wifiLock.release();

            // For testing that multiple servers are handled correctly.
            // mServerMap.put("Dummy", "127.0.0.1");
            return null;
        }

        @Override
        protected void onCancelled(Void result) {
            mPref.onScanFinished(mServerMap);
        }

        @Override
        protected void onPostExecute(Void result) {
            mPref.onScanFinished(mServerMap);
        }
    }
}
