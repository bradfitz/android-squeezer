package uk.org.ngo.squeezer.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.TreeMap;

import uk.org.ngo.squeezer.R;

/**
 * Scans the local network for servers.
 */
public class ScanNetworkTask extends android.os.AsyncTask<Void, Void, Void> {
    private static final String TAG = "scanNetworkTask";

    private final Context mContext;

    private final ScanNetworkCallback callback;

    /**
     * Map server names to IP addresses.
     */
    private final TreeMap<String, String> mServerMap = new TreeMap<String, String>();

    /**
     * UDP port to broadcast discovery requests to.
     */
    private static final int DISCOVERY_PORT = 3483;

    /**
     * Maximum time to wait between discovery attempts (ms).
     */
    private static final int DISCOVERY_ATTEMPT_TIMEOUT = 1000;

    public ScanNetworkTask(Context context, ScanNetworkCallback callback) {
        mContext = context;
        this.callback = callback;
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
                        String host = responsePacket.getAddress().getHostAddress();

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
                        mServerMap.put(name, host);
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
        callback.onScanFinished(mServerMap);
    }

    @Override
    protected void onPostExecute(Void result) {
        callback.onScanFinished(mServerMap);
    }

    public interface ScanNetworkCallback {
        void onScanFinished(TreeMap<String, String> mServerMap);
    }
}
