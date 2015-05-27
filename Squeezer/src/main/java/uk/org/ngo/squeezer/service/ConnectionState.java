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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Predicate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

import static com.google.common.collect.Collections2.filter;

public class ConnectionState {

    private static final String TAG = "ConnectionState";

    /** {@link java.util.regex.Pattern} that splits strings on semi-colons. */
    private static final Pattern mSemicolonSplitPattern = Pattern.compile(";");

    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    // Connection state machine
    @IntDef({DISCONNECTED, CONNECTION_STARTED, CONNECTION_FAILED, CONNECTION_COMPLETED,
            LOGIN_STARTED, LOGIN_FAILED, LOGIN_COMPLETED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionStates {}
    /** Ordinarily disconnected from the server. */
    public static final int DISCONNECTED = 0;
    /** A connection has been started. */
    public static final int CONNECTION_STARTED = 1;
    /** The connection to the server did not complete. */
    public static final int CONNECTION_FAILED = 2;
    /** The connection to the server completed. */
    public static final int CONNECTION_COMPLETED = 3;
    /** The login process has started. */
    public static final int LOGIN_STARTED = 4;
    /** The login process has failed, the server is disconnected. */
    public static final int LOGIN_FAILED = 5;
    /** The login process completed, the handshake can start. */
    public static final int LOGIN_COMPLETED = 6;

    @ConnectionStates
    private volatile int mConnectionState = DISCONNECTED;

    /** Does the server support "favorites items" queries? */
    private final AtomicBoolean mCanFavorites = new AtomicBoolean(false);

    private final AtomicBoolean mCanMusicfolder = new AtomicBoolean(false);

    /** Does the server support "myapps items" queries? */
    private final AtomicBoolean mCanMyApps = new AtomicBoolean(false);

    private final AtomicBoolean canRandomplay = new AtomicBoolean(false);

    private final AtomicReference<String> preferredAlbumSort = new AtomicReference<String>("album");

    private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();

    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();

    private final AtomicReference<Player> activePlayer = new AtomicReference<Player>();

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    private final Map<String, Player> mPlayers = new HashMap<String, Player>();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<String>();

    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();

    private final AtomicReference<Integer> cliPort = new AtomicReference<Integer>();

    private final AtomicReference<String> userName = new AtomicReference<String>();

    private final AtomicReference<String> password = new AtomicReference<String>();

    private final AtomicReference<String[]> mediaDirs = new AtomicReference<String[]>();

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

        if (loginFailed) {
            setConnectionState(service, LOGIN_FAILED);
        } else {
            setConnectionState(service, DISCONNECTED);
        }

        httpPort.set(null);
        activePlayer.set(null);
        mediaDirs.set(null);
    }

    /**
     * Sets a new connection state, and posts a sticky
     * {@link uk.org.ngo.squeezer.service.event.ConnectionChanged} event with the new state.
     *
     * @param service The service that contains the eventbus to post the event to.
     * @param connectionState The new connection state.
     */
    void setConnectionState(@NonNull SqueezeService service,
            @ConnectionStates int connectionState) {
        Log.d(TAG, "Setting connection state to: " + connectionState);
        mConnectionState = connectionState;
        service.mEventBus.postSticky(new ConnectionChanged(mConnectionState));
    }

    @Nullable Player getActivePlayer() {
        return activePlayer.get();
    }

    void setActivePlayer(@Nullable Player player) {
        activePlayer.set(player);
    }

    @Nullable public PlayerState getActivePlayerState() {
        if (activePlayer.get() == null)
            return null;

        return activePlayer.get().getPlayerState();
    }

    @Nullable public PlayerState getPlayerState(String playerId) {
        Player player = mPlayers.get(playerId);

        if (player == null)
            return null;

        return player.getPlayerState();
    }

    /** Filter predicate that matches connected players. */
    private static final Predicate<Player> sPlayerConnectedPredicate = new Predicate<Player>() {
        @Override
        public boolean apply(@javax.annotation.Nullable Player input) {
            return input != null && input.getConnected();
        }
    };

    List<Player> getPlayers() {
        return new ArrayList<Player>(mPlayers.values());  // XXX: Immutable list? Return the map?
    }

    java.util.Collection<Player> getConnectedPlayers() {
        return filter(getPlayers(), sPlayerConnectedPredicate);
    }

    void clearPlayers() {
        mPlayers.clear();
    }

    public void addPlayer(Player player) {
        mPlayers.put(player.getId(), player);
    }

    void addPlayers(List<Player> players) {
        for (Player player : players) {
            mPlayers.put(player.getId(), player);
        }
    }

    @Nullable
    Player getPlayer(String playerId) {
        return mPlayers.get(playerId);
    }

    PrintWriter getSocketWriter() {
        return socketWriter.get();
    }

    void setHttpPort(Integer port) {
        httpPort.set(port);
        Log.v(TAG, "HTTP port is now: " + port);
    }

    public String[] getMediaDirs() {
        String[] dirs = mediaDirs.get();
        return dirs == null ? new String[0] : dirs;
    }

    public void setMediaDirs(String dirs) {
        mediaDirs.set(mSemicolonSplitPattern.split(dirs));
    }

    void setCanFavorites(boolean value) {
        mCanFavorites.set(value);
    }

    boolean canFavorites() {
        return mCanFavorites.get();
    }

    void setCanMusicfolder(boolean value) {
        mCanMusicfolder.set(value);
    }

    boolean canMusicfolder() {
        return mCanMusicfolder.get();
    }

    void setCanMyApps(boolean value) {
        mCanMyApps.set(value);
    }

    boolean canMyApps() {
        return mCanMyApps.get();
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

    /**
     * @return True if the socket connection to the server has completed.
     */
    boolean isConnected() {
        switch (mConnectionState) {
            case CONNECTION_COMPLETED:
            case LOGIN_STARTED:
            case LOGIN_COMPLETED:
                return true;

            default:
                return false;
        }
    }

    /**
     * @return True if the socket connection to the server has started, but not yet
     *     completed (successfully or unsuccessfully).
     */
    boolean isConnectInProgress() {
        return mConnectionState == CONNECTION_STARTED;
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
            Log.d(TAG, "Listening thread started");
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

                // If a login attempt was in progress and this is a line that does not start
                // with "login " then the login must have been successful (otherwise the
                // server would have disconnected), so update the connection state accordingly.
                if (mConnectionState == LOGIN_STARTED && !inputLine.startsWith("login ")) {
                    setConnectionState(service, LOGIN_COMPLETED);
                }
                service.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        service.onLineReceived(inputLine);
                    }
                });
            }
        }
    }

    void startConnect(final SqueezeService service, String hostPort, final String userName,
                      final String password) {
        Log.v(TAG, "startConnect");
        // Common mistakes, based on crash reports...
        if (hostPort.startsWith("Http://") || hostPort.startsWith("http://")) {
            hostPort = hostPort.substring(7);
        }

        // Ending in whitespace?  From LatinIME, probably?
        while (hostPort.endsWith(" ")) {
            hostPort = hostPort.substring(0, hostPort.length() - 1);
        }

        final int port = Util.parsePort(hostPort);
        final String host = Util.parseHost(hostPort);
        final String cleanHostPort = host + ":" + port;

        currentHost.set(host);
        cliPort.set(port);
        httpPort.set(null);  // not known until later, after connect.
        this.userName.set(userName);
        this.password.set(password);

        // Start the off-thread connect.
        service.executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Ensuring service is disconnected");
                service.disconnect();
                Socket socket = new Socket();
                try {
                    Log.d(TAG, "Connecting to: " + cleanHostPort);
                    setConnectionState(service, CONNECTION_STARTED);
                    socket.connect(new InetSocketAddress(host, port),
                            4000 /* ms timeout */);
                    socketRef.set(socket);
                    Log.d(TAG, "Connected to: " + cleanHostPort);
                    socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                    setConnectionState(service, CONNECTION_COMPLETED);
                    startListeningThread(service);
                    service.onCliPortConnectionEstablished(userName, password);
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(userName, password.toCharArray());
                        }
                    });
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket timeout connecting to: " + cleanHostPort);
                    setConnectionState(service, CONNECTION_FAILED);
                } catch (IOException e) {
                    Log.e(TAG, "IOException connecting to: " + cleanHostPort);
                    setConnectionState(service, CONNECTION_FAILED);
                }
            }

        });
    }

    Integer getHttpPort() {
        return httpPort.get();
    }

    String getUserName() {
        return userName.get();
    }

    String getPassword() {
        return password.get();
    }

    String getCurrentHost() {
        return currentHost.get();
    }

}
