package com.danga.squeezer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.util.EncodingUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class SqueezeService extends Service {
    private static final String TAG = "SqueezeService";
    private static final int PLAYBACKSERVICE_STATUS = 1;
	
    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);
	
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();
    private final AtomicReference<IServiceCallback> callback =
        new AtomicReference<IServiceCallback>();
    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
    private final AtomicReference<String> activePlayerId = new AtomicReference<String>();
    private final AtomicReference<Map<String, String>> knownPlayers = 
        new AtomicReference<Map<String, String>>();

    private final AtomicReference<String> currentSong = new AtomicReference<String>();
    private final AtomicReference<String> currentArtist = new AtomicReference<String>();
    private final AtomicReference<String> currentAlbum = new AtomicReference<String>();
    private final AtomicReference<String> currentArtworkTrackId = new AtomicReference<String>();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<String>();
    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();
    private final AtomicReference<Integer> cliPort = new AtomicReference<Integer>();
    
    @Override
        public void onCreate() {
    	super.onCreate();
    	
        // Clear leftover notification in case this service previously got killed while playing                                                
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
    }
	
    @Override
	public IBinder onBind(Intent intent) {
        return squeezeService;
    }
	
    @Override
	public void onDestroy() {
        super.onDestroy();
        disconnect();
        callback.set(null);
    }

    private void disconnect() {
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
        isPlaying.set(false);
        knownPlayers.set(null);
        setConnectionState(false, false);
        clearOngoingNotification();
        currentSong.set(null);
        httpPort.set(null);
    }

    private synchronized void sendCommand(String... commands) {
        if (commands.length == 0) return;
        PrintWriter writer = socketWriter.get();
        if (writer == null) return;
        if (commands.length == 1) {
            Log.v(TAG, "SENDING: " + commands[0]);
            writer.println(commands[0]);
        } else {
            // Get it into one packet by deferring flushing...
            for (String command : commands) {
                writer.print(command + "\n");
            }
            writer.flush();
        }
    }
	
    private void sendPlayerCommand(String command) {
        if (activePlayerId == null) {
            return;
        }
        sendCommand(activePlayerId + " " + command);
    }
	
    private void onLineReceived(String serverLine) {
        Log.v(TAG, "LINE: " + serverLine);
        List<String> tokens = Arrays.asList(serverLine.split(" "));
        if (tokens.size() < 2) {
            return;
        }
        if (serverLine.startsWith("players 0 100 count")) {
            parsePlayerList(tokens);
            return;
        }
        if ("pref".equals(tokens.get(0)) &&
            "httpport".equals(tokens.get(1)) &&
            tokens.size() >= 3) {
            httpPort.set(Integer.parseInt(tokens.get(2)));
            Log.v(TAG, "HTTP port is now: " + httpPort);
            return;
        }
        
        // Player-specific commands follow.  But we ignore all that aren't for our
        // active player.
        String activePlayer = activePlayerId.get();
        if (activePlayer == null || activePlayer.length() == 0 ||
            !decode(tokens.get(0)).equals(activePlayerId.get())) {
            // Different player that we're not interested in.   
            // (yet? maybe later.)
            return;
        }
        String command = tokens.get(1);
        if (command == null) return;
        if (serverLine.contains("prefset server volume")) {
            String newVolume = tokens.get(4);
            Log.v(TAG, "New volume is: " + newVolume);
            sendNewVolumeCallback(Integer.parseInt(newVolume));
            return;
        }
        if (command.equals("play")) {
            setPlayingState(true);
            return;
        }
        if (command.equals("stop")) {
            setPlayingState(false);
            return;
        }
        if (command.equals("pause")) {
            boolean newState = !isPlaying.get();
            if (tokens.size() >= 3) {
                String explicitPause = tokens.get(2); 
                if ("0".equals(explicitPause)) {
                    newState = true;  // playing.  (unpaused)
                } else if ("1".equals(explicitPause)) {
                    newState = false;  // explicitly paused.
                }
            }
            setPlayingState(newState);
            return;
        }
        if (command.equals("status")) {
            parseStatusLine(tokens);
            return;
        }
        if (command.equals("playlist")) {
            if (tokens.size() >= 4 && "newsong".equals(tokens.get(2))) {
                String newSong = decode(tokens.get(3));
                currentSong.set(newSong);
                updateOngoingNotification();
                sendMusicChangedCallback();
                
                // Now also ask for the rest of the status.
                sendPlayerCommand("status - 1 tags:ylqwaJ");
            }
        }

    }

    private void sendNewVolumeCallback(int newVolume) {
        if (callback.get() == null) {
            return;
        }
        try {
            callback.get().onVolumeChange(newVolume);
        } catch (RemoteException e) {
        }
    }

    private void parseStatusLine(List<String> tokens) {
        int n = 0;
        boolean musicHasChanged = false;
        boolean sawArtworkId = false;
        for (String token : tokens) {
            n++;
            if (n <= 2) continue;
            if (token == null || token.length() == 0) continue;
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
                if (n <= 4) continue;  // e.g. "00%3A04%3A20%3A05%3A09%3A36 status - 1 ...."
                Log.e(TAG, "Expected colon in status line token: " + token);
                return;
            }
            String key = decode(token.substring(0, colonPos));
            String value = decode(token.substring(colonPos + 3));
            if (key == null || value == null) continue;
            if (key.equals("mixer volume")) {
                continue;
            }
            if (key.equals("mode")) {
                if (value.equals("pause")) {
                    setPlayingState(false);
                } else if (value.equals("play")) {
                    setPlayingState(true);
                }
                continue;
            }
            if (key.equals("artist")) {
                if (Util.atomicStringUpdated(currentArtist, value)) musicHasChanged = true;
                continue;
            }
            if (key.equals("title")) {
                if (Util.atomicStringUpdated(currentSong, value)) musicHasChanged = true;
                continue;
            }
            if (key.equals("album")) {
                if (Util.atomicStringUpdated(currentAlbum, value)) musicHasChanged = true;
                continue;
            }
            if (key.equals("artwork_track_id")) {
                currentArtworkTrackId.set(value);
                sawArtworkId = true;
            }
            // TODO: the rest ....
            // 00%3A04%3A20%3A17%3A04%3A7f status   player_name%3AOffice player_connected%3A1 player_ip%3A10.0.0.73%3A42648 power%3A1 signalstrength%3A0 mode%3Aplay time%3A99.803 rate%3A1 duration%3A224.705 can_seek%3A1 mixer%20volume%3A25 playlist%20repeat%3A0 playlist%20shuffle%3A0 playlist%20mode%3Adisabled playlist_cur_index%3A5 playlist_timestamp%3A1250053991.01067 playlist_tracks%3A46
        }
        if (musicHasChanged) {
            if (!sawArtworkId) {
                // TODO: we should disambiguate between no artwork because there is no
                // artwork (explicitly known) and no artwork because it's e.g. Pandora,
                // in which case we'd use the current cover.jpg URL.
                currentArtworkTrackId.set(null);
            }
            updateOngoingNotification();
            sendMusicChangedCallback();
        }
    }
    
    private void parsePlayerList(List<String> tokens) {
        Log.v(TAG, "Parsing player list.");
        // TODO: can this block (sqlite lookup via binder call?)  Might want to move it elsewhere.
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
    	final String lastConnectedPlayer = preferences.getString(Preferences.KEY_LASTPLAYER, null);
    	Log.v(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);
        Map<String, String> players = new HashMap<String, String>();
                
        int n = 0;
        int currentPlayerIndex = -1;
        String currentPlayerId = null;
        String currentPlayerName = null;
        String defaultPlayerId = null;
        
        for (String token : tokens) {
            if (++n <= 3) continue;
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
                Log.e(TAG, "Expected colon in playerlist token.");
                return;
            }
            String key = token.substring(0, colonPos);
            String value = decode(token.substring(colonPos + 3));
            Log.v(TAG, "key=" + key + ", value: " + value);
            if ("playerindex".equals(key)) {
                maybeAddPlayerToMap(currentPlayerId, currentPlayerName, players);
                currentPlayerId = null;
                currentPlayerName = null;
                currentPlayerIndex = Integer.parseInt(value);
            } else if ("playerid".equals(key)) {
                currentPlayerId = value;
                if (value.equals(lastConnectedPlayer)) {
                    defaultPlayerId = value;  // Still around, so let's use it.
                }
            } else if ("name".equals(key)) {
                currentPlayerName = value;
            }
        }
        maybeAddPlayerToMap(currentPlayerId, currentPlayerName, players);

        if (defaultPlayerId == null || !players.containsKey(defaultPlayerId)) {
            defaultPlayerId = currentPlayerId;  // arbitrary; last one in list.
        }

        knownPlayers.set(players);
        
        if (callback.get() != null) {
            try {
                callback.get().onPlayersDiscovered();
            } catch (RemoteException e) {}
        }
        
        changeActivePlayer(defaultPlayerId);
    }

    private boolean changeActivePlayer(String playerId) {
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
        activePlayerId.set(playerId);

        // Start an async fetch of its status.
        sendPlayerCommand("status - 1 tags:jylqwaJ");
        
        // TODO: this involves a write and can block (sqlite lookup via binder call), so
        // should be done in an AsyncTask or other thread.
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();              
        editor.putString(Preferences.KEY_LASTPLAYER, playerId);
        editor.commit();
       
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

    // Add String pair to map if both are non-null and non-empty.    
    private static void maybeAddPlayerToMap(String currentPlayerId,
            String currentPlayerName, Map<String, String> players) {
        if (currentPlayerId != null && !currentPlayerId.equals("") && 
            currentPlayerName != null && !currentPlayerName.equals("")) {
            Log.v(TAG, "Adding player: " + currentPlayerId + ", " + currentPlayerName);
            players.put(currentPlayerId, currentPlayerName);
        }
    }

    private String decode(String substring) {
        try {
            return URLDecoder.decode(substring, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private void onCliPortConnectionEstablished() {
        Thread listeningThread = new ListeningThread(socketRef.get(),
                                                     currentConnectionGeneration.incrementAndGet());
        listeningThread.start();

        sendCommand("listen 1",
                "players 0 100",   // get first 100 players
                "pref httpport ?"  // learn the HTTP port (needed for images)
        );
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
	
    private void setPlayingState(boolean state) {
        isPlaying.set(state);
        updateOngoingNotification();
		
        if (callback.get() == null) {
            return;
        }
        try {
            callback.get().onPlayStatusChanged(state);
        } catch (RemoteException e) {
        }

    }

    private void updateOngoingNotification() {
        if (!isPlaying.get()) {
            clearOngoingNotification();
            return;
        }
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification status = new Notification();
        //status.contentView = views;
        Intent showNowPlaying = new Intent(this, SqueezerActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, showNowPlaying, 0);
        String song = currentSong.get();
        if (song == null) song = "";
        status.setLatestEventInfo(this, "Music Playing", song, pIntent);
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.stat_notify_musicplayer;
        nm.notify(PLAYBACKSERVICE_STATUS, status);
    }

    private void sendMusicChangedCallback() {
        if (callback.get() == null) {
            return;
        }
        try {
            callback.get().onMusicChanged();
        } catch (RemoteException e) {
        }
    }

    private void clearOngoingNotification() {
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
    }

    private final ISqueezeService.Stub squeezeService = new ISqueezeService.Stub() {

        public void registerCallback(IServiceCallback callback) throws RemoteException {
	    	SqueezeService.this.callback.set(callback);
	    	callback.onConnectionChanged(isConnected.get(), false);
	    }
	    
	    public void unregisterCallback(IServiceCallback callback) throws RemoteException {
	    	SqueezeService.this.callback.compareAndSet(callback, null);
	    }

	    public int adjustVolumeBy(int delta) throws RemoteException {
            if (delta > 0) {
                sendPlayerCommand("mixer volume %2B" + delta);
            } else if (delta < 0) {
                sendPlayerCommand("mixer volume " + delta);
            }
            return 50 + delta;  // TODO: return non-blocking dead-reckoning value
        }

        public boolean isConnected() throws RemoteException {
            return isConnected.get();
        }

        public void startConnect(final String hostPort) throws RemoteException {
            int colonPos = hostPort.indexOf(":");
            boolean noPort = colonPos == -1;
            final int port = noPort? 9090 : Integer.parseInt(hostPort.substring(colonPos + 1));
            final String host = noPort ? hostPort : hostPort.substring(0, colonPos);
            currentHost.set(host);
            cliPort.set(port);
            httpPort.set(null);  // not known until later, after connect.
            
            // Start the off-thread connect.
            executor.execute(new Runnable() {
                public void run() {
                    SqueezeService.this.disconnect();
                    Socket socket = new Socket();
                    try {
                        socket.connect(new InetSocketAddress(host, port),
                                       4000 /* ms timeout */);
                        socketRef.set(socket);
                        Log.d(TAG, "Connected to: " + hostPort);
                        socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                        Log.d(TAG, "writer == " + socketWriter.get());
                        setConnectionState(true, true);
                        Log.d(TAG, "connection state broadcasted true.");
                        onCliPortConnectionEstablished();
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "Socket timeout connecting to: " + hostPort);
                        setConnectionState(false, true);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException connecting to: " + hostPort);
                        setConnectionState(false, true);
                    }
                }

            });
        }

        public void disconnect() throws RemoteException {
            if (!isConnected()) return;
            SqueezeService.this.disconnect();
        }
		
        public boolean togglePausePlay() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
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

        public boolean play() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            Log.v(TAG, "play..");
            isPlaying.set(true);
            sendPlayerCommand("play");
            Log.v(TAG, "played.");
            return true;
        }

        public boolean stop() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            isPlaying.set(false);
            sendPlayerCommand("stop");
            return true;
        }

        public boolean nextTrack() throws RemoteException {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            sendPlayerCommand("button jump_fwd");
            return true;
        }
        
        public boolean previousTrack() throws RemoteException {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            sendPlayerCommand("button jump_rew");
            return true;
        }
        
        public boolean isPlaying() throws RemoteException {
            return isPlaying.get();
        }

        public boolean getPlayers(List<String> playerIds, List<String> playerNames)
            throws RemoteException {
            Map<String, String> players = knownPlayers.get();
            if (players == null) {
                return false;
            }
            for (String playerId : players.keySet()) {
                playerIds.add(playerId);
                playerNames.add(players.get(playerId));
            }
            return true;
        }

        public boolean setActivePlayer(String playerId) throws RemoteException {
            return changeActivePlayer(playerId);
        }

        public String getActivePlayerId() throws RemoteException {
            String playerId = activePlayerId.get();
            return playerId == null ? "" : playerId;
        }

        public String getActivePlayerName() throws RemoteException {
            String playerId = activePlayerId.get();
            Map<String, String> players = knownPlayers.get();
            if (players == null) {
                return null;
            }
            return players.get(playerId);
        }

        public String currentAlbum() throws RemoteException {
            return Util.nonNullString(currentAlbum);
        }

        public String currentArtist() throws RemoteException {
            return Util.nonNullString(currentArtist);
        }

        public String currentSong() throws RemoteException {
            return Util.nonNullString(currentSong);
        }

        public String currentAlbumArtUrl() throws RemoteException {
            Integer port = httpPort.get();
            if (port == null || port == 0) return "";
            String artworkTrackId = currentArtworkTrackId.get();
            if (artworkTrackId != null) {
                Log.v(TAG, "artwork track ID = " + artworkTrackId);
                return "http://" + currentHost.get() + ":" + port
                    + "/music/" + artworkTrackId + "/cover.jpg";
            } else {
                // Return the "current album art" URL instead, with the cache-buster
                // of the song name in it, to force the activity to reload when
                // listening to e.g. Pandora, where there is no artwork_track_id (tag J)
                // in the status.
                return "http://" + currentHost.get() + ":" + port
                    + "/music/current/cover?player=" + activePlayerId.get()
                    + "&song=" + URLEncoder.encode(currentSong());
            }
        }
    };

    private class ListeningThread extends Thread {
        private final Socket socket;
        private final int generationNumber; 
        public ListeningThread(Socket socket, int generationNumber) {
            this.socket = socket;
            this.generationNumber = generationNumber;
        }
		
        @Override
            public void run() {
            BufferedReader in;
            try {
                in = new BufferedReader(
                                        new InputStreamReader(socket.getInputStream()),
                                        128);
            } catch (IOException e) {
                Log.v(TAG, "IOException while creating BufferedReader: " + e);
                SqueezeService.this.disconnect();
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
                        SqueezeService.this.disconnect();
                    } else {
                        // Who cares.
                        Log.v(TAG, "Old generation connection disconnected, as expected.");
                    }
                    return;
                }
                SqueezeService.this.onLineReceived(line);
            }
        }
    }
 }
