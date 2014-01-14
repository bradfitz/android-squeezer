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

package uk.org.ngo.squeezer.service;

import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.model.Player;

class ConnectionState {

    private static final String TAG = "ConnectionState";

    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    // Connection state:
    private final AtomicBoolean isConnectInProgress = new AtomicBoolean(false);

    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    private final AtomicBoolean mCanMusicfolder = new AtomicBoolean(false);

    private final AtomicBoolean canRandomplay = new AtomicBoolean(false);

    private final AtomicReference<String> preferredAlbumSort = new AtomicReference<String>("album");

    private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();

    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();

    private final AtomicReference<Player> activePlayer = new AtomicReference<Player>();

    private final AtomicReference<Player> defaultPlayer = new AtomicReference<Player>();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<String>();

    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();

    private final AtomicReference<Integer> cliPort = new AtomicReference<Integer>();

    private WifiManager.WifiLock wifiLock;

    void setWifiLock(WifiManager.WifiLock wifiLock) {
        this.wifiLock = wifiLock;
    }

    void updateWifiLock(boolean state) {
        // TODO: this might be running in the wrong thread.  Is wifiLock thread-safe?
        if (state && !wifiLock.isHeld()) {
            Log.v(TAG, "Locking wifi while playing.");
            wifiLock.acquire();
        }
        if (!state && wifiLock.isHeld()) {
            Log.v(TAG, "Unlocking wifi.");
            try {
                wifiLock.release();
                // Seen a crash here with:
                //
                // Permission Denial: broadcastIntent() requesting a sticky
                // broadcast
                // from pid=29506, uid=10061 requires
                // android.permission.BROADCAST_STICKY
                //
                // Catching the exception (which seems harmless) seems better
                // than requesting an additional permission.

                // Seen a crash here with
                //
                // java.lang.RuntimeException: WifiLock under-locked
                // Squeezer_WifiLock
                //
                // Both crashes occurred when the wifi was disabled, on HTC Hero
                // devices running 2.1-update1.
            } catch (SecurityException e) {
                Log.v(TAG, "Caught odd SecurityException releasing wifilock");
            }
        }
    }

    void disconnect(SqueezeService service, boolean loginFailed) {
        Log.v(TAG, "disconnect" + (loginFailed ? ": authentication failure" : ""));
        currentConnectionGeneration.incrementAndGet();
        Socket socket = socketRef.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        socketRef.set(null);
        socketWriter.set(null);
        isConnected.set(false);

        setConnectionState(service, false, false, loginFailed);

        httpPort.set(null);
        activePlayer.set(null);
    }

    private void setConnectionState(final SqueezeService service, final boolean currentState,
            final boolean postConnect, final boolean loginFailed) {
        isConnected.set(currentState);
        if (postConnect) {
            isConnectInProgress.set(false);
        }

        service.executor.execute(new Runnable() {
            @Override
            public void run() {
                int i = service.mServiceCallbacks.beginBroadcast();
                while (i > 0) {
                    i--;
                    try {
                        Log.d(TAG, "pre-call setting callback connection state to: " + currentState);
                        service.mServiceCallbacks.getBroadcastItem(i)
                                .onConnectionChanged(currentState, postConnect, loginFailed);
                        Log.d(TAG, "post-call setting callback connection state.");

                    } catch (RemoteException e) {
                        // The RemoteCallbackList will take care of removing
                        // the dead object for us.
                    }
                }
                service.mServiceCallbacks.finishBroadcast();
            }
        });
    }

    Player getActivePlayer() {
        return activePlayer.get();
    }

    void setActivePlayer(Player player) {
        activePlayer.set(player);
    }

    Player getDefaultPlayer() {
        return defaultPlayer.get();
    }

    void setDefaultPlayer(Player player) {
        defaultPlayer.set(player);
    }

    PrintWriter getSocketWriter() {
        return socketWriter.get();
    }

    void setHttpPort(Integer port) {
        httpPort.set(port);
        Log.v(TAG, "HTTP port is now: " + port);
    }

    void setCanMusicfolder(boolean value) {
        mCanMusicfolder.set(value);
    }

    boolean canMusicfolder() {
        return mCanMusicfolder.get();
    }

    void setCanRandomplay(boolean value) {
        canRandomplay.set(value);
    }

    boolean canRandomplay() {
        return canRandomplay.get();
    }

    public void setPreferedAlbumSort(String value) {
        preferredAlbumSort.set(value);
    }

    public String getPreferredAlbumSort() {
        return preferredAlbumSort.get();
    }

    boolean isConnected() {
        return isConnected.get();
    }

    boolean isConnectInProgress() {
        return isConnectInProgress.get();
    }

    void startListeningThread(SqueezeService service) {
        Thread listeningThread = new ListeningThread(service, socketRef.get(),
                currentConnectionGeneration.incrementAndGet());
        listeningThread.start();
    }

    private class ListeningThread extends Thread {

        private final SqueezeService service;

        private final Socket socket;

        private final int generationNumber;

        private ListeningThread(SqueezeService service, Socket socket, int generationNumber) {
            this.service = service;
            this.socket = socket;
            this.generationNumber = generationNumber;
        }

        @Override
        public void run() {
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 128);
            } catch (IOException e) {
                Log.v(TAG, "IOException while creating BufferedReader: " + e);
                service.disconnect();
                return;
            }
            IOException exception = null;
            while (true) {
                String line;
                try {
                    line = in.readLine();
                } catch (IOException e) {
                    line = null;
                    exception = e;
                }
                if (line == null) {
                    // Socket disconnected.  This is expected
                    // if we're not the main connection generation anymore,
                    // else we should notify about it.
                    if (currentConnectionGeneration.get() == generationNumber) {
                        Log.v(TAG, "Server disconnected; exception=" + exception);
                        service.disconnect(exception == null);
                    } else {
                        // Who cares.
                        Log.v(TAG, "Old generation connection disconnected, as expected.");
                    }
                    return;
                }
                final String inputLine = line;
                service.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        service.onLineReceived(inputLine);
                    }
                });
            }
        }
    }

    void startConnect(final SqueezeService service, ScheduledThreadPoolExecutor executor,
            String hostPort, final String userName, final String password) throws RemoteException {
        Log.v(TAG, "startConnect");
        // Common mistakes, based on crash reports...
        if (hostPort.startsWith("Http://") || hostPort.startsWith("http://")) {
            hostPort = hostPort.substring(7);
        }

        // Ending in whitespace?  From LatinIME, probably?
        while (hostPort.endsWith(" ")) {
            hostPort = hostPort.substring(0, hostPort.length() - 1);
        }

        final int port = parsePort(hostPort);
        final String host = parseHost(hostPort);
        final String cleanHostPort = host + ":" + port;

        currentHost.set(host);
        cliPort.set(port);
        httpPort.set(null);  // not known until later, after connect.

        // Start the off-thread connect.
        executor.execute(new Runnable() {
            @Override
            public void run() {
                service.disconnect();
                Socket socket = new Socket();
                try {
                    Log.d(TAG, "Connecting to: " + cleanHostPort);
                    isConnectInProgress.set(true);
                    socket.connect(new InetSocketAddress(host, port),
                            4000 /* ms timeout */);
                    socketRef.set(socket);
                    Log.d(TAG, "Connected to: " + cleanHostPort);
                    socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                    setConnectionState(service, true, true, false);
                    Log.d(TAG, "connection state broadcasted true.");
                    startListeningThread(service);
                    setDefaultPlayer(null);
                    service.onCliPortConnectionEstablished(userName, password);
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(userName, password.toCharArray());
                        }
                    });
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket timeout connecting to: " + cleanHostPort);
                    setConnectionState(service, false, true, false);
                } catch (IOException e) {
                    Log.e(TAG, "IOException connecting to: " + cleanHostPort);
                    setConnectionState(service, false, true, false);
                }
            }

        });
    }

    private static String parseHost(String hostPort) {
        if (hostPort == null) {
            return "";
        }
        int colonPos = hostPort.indexOf(":");
        if (colonPos == -1) {
            return hostPort;
        }
        return hostPort.substring(0, colonPos);
    }

    private static int parsePort(String hostPort) {
        if (hostPort == null) {
            return Squeezer.getContext().getResources().getInteger(R.integer.DefaultPort);
        }
        int colonPos = hostPort.indexOf(":");
        if (colonPos == -1) {
            return Squeezer.getContext().getResources().getInteger(R.integer.DefaultPort);
        }
        try {
            return Integer.parseInt(hostPort.substring(colonPos + 1));
        } catch (NumberFormatException unused) {
            Log.d(TAG, "Can't parse port out of " + hostPort);
            return Squeezer.getContext().getResources().getInteger(R.integer.DefaultPort);
        }
    }

    Integer getHttpPort() {
        return httpPort.get();
    }

    String getCurrentHost() {
        return currentHost.get();
    }

}
