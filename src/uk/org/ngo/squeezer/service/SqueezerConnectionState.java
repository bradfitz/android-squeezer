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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.model.SqueezerPlayer;

import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.util.Log;

import uk.org.ngo.squeezer.IServiceCallback;

class SqueezerConnectionState {
    private static final String TAG = "SqueezeService";
    private static final int DEFAULT_PORT = 9090;

    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    // Connection state:
	private final AtomicReference<IServiceCallback> callback = new AtomicReference<IServiceCallback>();
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private final AtomicBoolean canRandomplay = new AtomicBoolean(false);
	private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();
	private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
	private final AtomicReference<SqueezerPlayer> activePlayer = new AtomicReference<SqueezerPlayer>();
	private final AtomicReference<SqueezerPlayer> defaultPlayer = new AtomicReference<SqueezerPlayer>();

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
            wifiLock.release();
        }
	}

    void disconnect() {
        currentConnectionGeneration.incrementAndGet();
        Socket socket = socketRef.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {}
        }
        socketRef.set(null);
        socketWriter.set(null);
        isConnected.set(false);

        setConnectionState(false, false);

        httpPort.set(null);
        activePlayer.set(null);
    }

    private void setConnectionState(boolean currentState, boolean postConnect) {
        isConnected.set(currentState);
        if (callback.get() == null) {
            return;
        }
        try {
            Log.d(TAG, "pre-call setting callback connection state to: " + currentState);
            callback.get().onConnectionChanged(currentState, postConnect);
            Log.d(TAG, "post-call setting callback connection state.");
        } catch (RemoteException e) {
        }
    }

    IServiceCallback getCallback() {
    	return callback.get();
    }

    void setCallback(IServiceCallback callback) {
    	this.callback.set(callback);
    }

    void callbackCompareAndSet(IServiceCallback expect, IServiceCallback update) {
    	callback.compareAndSet(expect, update);
    }

    SqueezerPlayer getActivePlayer() {
    	return activePlayer.get();
    }

    void setActivePlayer(SqueezerPlayer player) {
    	activePlayer.set(player);
    }

    SqueezerPlayer getDefaultPlayer() {
    	return defaultPlayer.get();
    }

    void setDefaultPlayer(SqueezerPlayer player) {
    	defaultPlayer.set(player);
    }

    PrintWriter getSocketWriter() {
    	return socketWriter.get();
    }

    void setCurrentHost(String host) {
    	currentHost.set(host);
		Log.v(TAG, "HTTP port is now: " + httpPort);
    }

    void setCliPort(Integer port) {
    	cliPort.set(port);
		Log.v(TAG, "HTTP port is now: " + httpPort);
    }

    void setHttpPort(Integer port) {
    	httpPort.set(port);
		Log.v(TAG, "HTTP port is now: " + httpPort);
    }

    void setCanRandomplay(boolean value) {
    	canRandomplay.set(value);
    }

	boolean canRandomplay() {
		return canRandomplay.get();
	}

	boolean isConnected() {
		return isConnected.get();
	}

	void startListeningThread(SqueezeService service) {
        Thread listeningThread = new ListeningThread(service, socketRef.get(), currentConnectionGeneration.incrementAndGet());
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
                        service.disconnect();
                    } else {
                        // Who cares.
                        Log.v(TAG, "Old generation connection disconnected, as expected.");
                    }
                    return;
                }
                service.onLineReceived(line);
            }
        }
    }

    void startConnect(final SqueezeService service, ScheduledThreadPoolExecutor executor, String hostPort) throws RemoteException {
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
            public void run() {
                service.disconnect();
                Socket socket = new Socket();
                try {
                    Log.d(TAG, "Connecting to: " + cleanHostPort);
                    socket.connect(new InetSocketAddress(host, port),
                                   4000 /* ms timeout */);
                    socketRef.set(socket);
                    Log.d(TAG, "Connected to: " + cleanHostPort);
                    socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                    Log.d(TAG, "writer == " + socketWriter.get());
                    setConnectionState(true, true);
                    Log.d(TAG, "connection state broadcasted true.");
                	startListeningThread(service);
                    setDefaultPlayer(null);
                    service.onCliPortConnectionEstablished();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket timeout connecting to: " + cleanHostPort);
                    setConnectionState(false, true);
                } catch (IOException e) {
                    Log.e(TAG, "IOException connecting to: " + cleanHostPort);
                    setConnectionState(false, true);
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
            return DEFAULT_PORT;
        }
        int colonPos = hostPort.indexOf(":");
        if (colonPos == -1) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(hostPort.substring(colonPos + 1));
        } catch (NumberFormatException unused) {
            Log.d(TAG, "Can't parse port out of " + hostPort);
            return DEFAULT_PORT;
        }
    }

	Integer getHttpPort() {
		return httpPort.get();
	}

	String getCurrentHost() {
		return currentHost.get();
	}

}
