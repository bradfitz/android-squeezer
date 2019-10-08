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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

public class ConnectionState {

    private static final String TAG = "ConnectionState";

    /** {@link java.util.regex.Pattern} that splits strings on semi-colons. */
    private static final Pattern mSemicolonSplitPattern = Pattern.compile(";");

    public ConnectionState(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
    }

    private final EventBus mEventBus;

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

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    private final Map<String, Player> mPlayers = new ConcurrentHashMap<>();

    /** The active player (the player to which commands are sent by default). */
    private final AtomicReference<Player> mActivePlayer = new AtomicReference<>();

    private final AtomicReference<String> serverVersion = new AtomicReference<>();

    void disconnect(boolean loginFailed) {
        Log.i(TAG, "disconnect" + (loginFailed ? ": authentication failure" : ""));
        if (loginFailed) {
            setConnectionState(LOGIN_FAILED);
        } else {
            setConnectionState(DISCONNECTED);
        }
        serverVersion.set(null);
        mPlayers.clear();
        mActivePlayer.set(null);
    }

    /**
     * Sets a new connection state, and posts a sticky
     * {@link uk.org.ngo.squeezer.service.event.ConnectionChanged} event with the new state.
     *
     * @param connectionState The new connection state.
     */
    void setConnectionState(@ConnectionStates int connectionState) {
        Log.i(TAG, "Setting connection state to: " + connectionState);
        mConnectionState = connectionState;
        mEventBus.postSticky(new ConnectionChanged(mConnectionState));
    }

    public void setPlayers(Map<String, Player> players) {
        mPlayers.clear();
        mPlayers.putAll(players);
    }
    public Player getPlayer(String playerId) {
        return mPlayers.get(playerId);
    }

    public Map<String, Player> getPlayers() {
        return mPlayers;
    }

    public Player getActivePlayer() {
        return mActivePlayer.get();
    }

    public void setActivePlayer(Player player) {
        mActivePlayer.set(player);
    }

    public void setServerVersion(String version) {
        if (Util.atomicReferenceUpdated(serverVersion, version)) {
            maybeSendHandshakeComplete();
        }
    }

    public String getServerVersion() {
        return serverVersion.get();
    }

    private void maybeSendHandshakeComplete() {
        if (isHandshakeComplete()) {
            HandshakeComplete event = new HandshakeComplete(getServerVersion());
            Log.i(TAG, "Handshake complete: " + event);
            mEventBus.postSticky(event);
        }
    }
    private boolean isHandshakeComplete() {
        return serverVersion.get() != null;
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

    boolean isLoginStarted() {
        return mConnectionState == LOGIN_STARTED;
    }

    @Override
    public String toString() {
        return "ConnectionState{" +
                "mConnectionState=" + mConnectionState +
                ", serverVersion=" + serverVersion +
                '}';
    }
}
