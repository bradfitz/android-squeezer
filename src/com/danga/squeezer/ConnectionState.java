package com.danga.squeezer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

public class ConnectionState {
    private final String TAG = "ConnectionState";

    private final int connectionGeneration;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    
    private final ConcurrentHashMap<String, PlayerState> playerState =
        new ConcurrentHashMap<String, PlayerState>();
    
    private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();

    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
        private final AtomicReference<String> activePlayerId = new AtomicReference<String>();

        private final AtomicReference<Map<String, String>> knownPlayers = 
        new AtomicReference<Map<String, String>>();

    // Where we connected (or are connecting) to:
    private final String host;
    private final int cliPort;

    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();  // set post-connect
    
    public ConnectionState(int connectionGeneration, String host, int cliPort) {
        this.connectionGeneration = connectionGeneration;
        this.host = host;
        this.cliPort = cliPort;
    }
        
    public boolean isConnected() {
        return this.isConnected.get();
    }

    public void disconnect() {
        Socket socket = socketRef.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {}
            socketRef.set(null);
        }
        isConnected.set(false);
        activePlayerId.set(null);
    }

    public void sendPlayerCommand(String command) {
        String playerId = activePlayerId.get();
        if (playerId == null) {
            return;
        }
        // TODO: impl
        
    }

    public String getActivePlayerId() {
        return activePlayerId.get();
    }

    public boolean changeActivePlayer(String playerId) {
        Map<String, String> players = knownPlayers.get();
        if (players == null) {
            Log.v(TAG, "Can't set player; none known.");
            return false;
        }
        if (!players.containsKey(playerId)) {
            Log.v(TAG, "Player " + playerId + " not known.");
            return false;
        }

        Log.v(TAG, "Active player now: " + playerId + ", " + players.get(playerId));
        String oldPlayerId =  activePlayerId();
        boolean changed = Util.atomicStringUpdated(activePlayerId, playerId);

        if (oldPlayerId != null && !oldPlayerId.equals(playerId)) {
            // Unsubscribe from the old player's status.  (despite what
            // the docs say, multiple subscribes can be active and flood us.)
            sendCommand(URLEncoder.encode(oldPlayerId) + " status - 1 subscribe:0");
        }
        
        // Start an async fetch of its status.
        sendPlayerCommand("status - 1 tags:jylqwaJ");

        if (changed) {
            updatePlayerSubscriptionState();
        
            // NOTE: this involves a write and can block (sqlite lookup via binder call), so
            // should be done off-thread, so we can process service requests & send our callback
            // as quickly as possible.
            executor.execute(new Runnable() {
                public void run() {
                    final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();              
                    editor.putString(Preferences.KEY_LASTPLAYER, playerId);
                    editor.commit();
                }
            });
        }
        
        if (callback.get() != null) {
            try {
                if (playerId != null && players.containsKey(playerId)) {
                    callback.get().onPlayerChanged(
                            playerId, players.get(playerId));
                } else {
                    callback.get().onPlayerChanged("", "");
                }
            } catch (RemoteException e) {}
        }
        return true;
    }

    public int adjustVolumeBy(int delta) {
        if (delta > 0) {
            sendPlayerCommand("mixer volume %2B" + delta);
        } else if (delta < 0) {
            sendPlayerCommand("mixer volume " + delta);
        }
        return 50 + delta;  // TODO: return non-blocking dead-reckoning value
    }

    public boolean togglePlayPause() {
        Log.v(TAG, "pause...");
        if (isPlaying.get()) {
            setPlayingState(false);
            // NOTE: we never send ambiguous "pause" toggle commands (without the '1')
            // because then we'd get confused when they came back in to us, not being
            // able to differentiate ours coming back on the listen channel vs. those
            // of those idiots at the dinner party messing around.
            sendPlayerCommand("pause 1");
        } else {
            setPlayingState(true);
            // TODO: use 'pause 0 <fade_in_secs>' to fade-in if we knew it was
            // actually paused (as opposed to not playing at all) 
            sendPlayerCommand("play");
        }
        Log.v(TAG, "paused.");
        return true;
    }

    public boolean play() {
        Log.v(TAG, "play..");
        isPlaying.set(true);
        sendPlayerCommand("play");
        Log.v(TAG, "played.");
        return true;
    }    

    public boolean stop() {
        if (!isConnected()) {
            return false;
        }
        isPlaying.set(false);
        sendPlayerCommand("stop");
        return true;
    }
}
