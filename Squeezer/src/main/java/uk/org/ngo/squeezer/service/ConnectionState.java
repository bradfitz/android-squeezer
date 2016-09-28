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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

public class ConnectionState {

    private static final String TAG = "ConnectionState";

    /** {@link java.util.regex.Pattern} that splits strings on semi-colons. */
    private static final Pattern mSemicolonSplitPattern = Pattern.compile(";");

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

    private final AtomicReference<String> serverVersion = new AtomicReference<>();

    private final AtomicReference<String> preferredAlbumSort = new AtomicReference<>("album");

    private final AtomicReference<String[]> mediaDirs = new AtomicReference<>();

    void disconnect(EventBus eventBus, boolean loginFailed) {
        Log.v(TAG, "disconnect" + (loginFailed ? ": authentication failure" : ""));
        if (loginFailed) {
            setConnectionState(eventBus, LOGIN_FAILED);
        } else {
            setConnectionState(eventBus, DISCONNECTED);
        }
        mediaDirs.set(null);
    }

    /**
     * Sets a new connection state, and posts a sticky
     * {@link uk.org.ngo.squeezer.service.event.ConnectionChanged} event with the new state.
     *
     * @param eventBus The eventbus to post the event to.
     * @param connectionState The new connection state.
     */
    void setConnectionState(@NonNull EventBus eventBus,
            @ConnectionStates int connectionState) {
        Log.d(TAG, "Setting connection state to: " + connectionState);
        mConnectionState = connectionState;
        eventBus.postSticky(new ConnectionChanged(mConnectionState));
    }

    public String[] getMediaDirs() {
        String[] dirs = mediaDirs.get();
        return dirs == null ? new String[0] : dirs;
    }

    public void setMediaDirs(Object[] dirs) {
        String[] value = new String[dirs == null ? 0 : dirs.length];
        if (dirs != null) {
            for (int i = 0; i < dirs.length; i++) {
                value[i] = (String) dirs[i];
            }
        }
        mediaDirs.set(value);
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

    public void setServerVersion(String version) {
        serverVersion.set(version);
    }

    public String getServerVersion() {
        return serverVersion.get();
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

    boolean isLoginStarted() {
        return mConnectionState == LOGIN_STARTED;
    }

}
