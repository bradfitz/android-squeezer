package com.danga.squeezeremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
    private final AtomicReference<IServiceCallback> callback = new AtomicReference<IServiceCallback>();
    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
    private String activePlayerId = null;
    private Thread listeningThread = null;
	
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
        setConnectionState(false);
    }

    private synchronized void sendCommand(String command) {
        PrintWriter writer = socketWriter.get();
        if (writer == null) return;
        Log.v(TAG, "SENDING: " + command);
        writer.println(command);
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
        if (serverLine.startsWith("players 0 100 count")) {
            parsePlayerList(tokens);
            return;
        }
    }

    private void parsePlayerList(List<String> tokens) {
        Map<String, String> players = new HashMap<String, String>();
        int n = 0;
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
        }
        if (callback.get() != null) {
            try {
                callback.get().onPlayersDiscovered();
            } catch (RemoteException e) {
            }
        }
    }
	
    private String decode(String substring) {
        try {
            return URLDecoder.decode(substring, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private void startListeningThread() {
        listeningThread = new ListeningThread(socketRef.get(),
                                              currentConnectionGeneration.incrementAndGet());
        listeningThread.start();

        sendCommand("listen 1");
        // Get info on the first 100 players.
        sendCommand("players 0 100");
    }

    private void setConnectionState(boolean currentState) {
        isConnected.set(currentState);
        if (callback.get() == null) {
            return;
        }
        try {
            Log.d(TAG, "pre-call setting callback connection state to: " + currentState);
            callback.get().onConnectionChanged(currentState);
            Log.d(TAG, "post-call setting callback connection state.");
        } catch (RemoteException e) {
        }
    }
	
    private void setPlayingState(boolean state) {
        isPlaying.set(state);
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (state) {
            Notification status = new Notification();
            //status.contentView = views;
            PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                                                              new Intent(this, SqueezeRemoteActivity.class), 0);
            status.setLatestEventInfo(this, "Music Playing", "Content Text", pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
            //status.contentIntent = PendingIntent.getActivity(this, 0,
            //        new Intent(this, SqueezeRemoteActivity.class), 0);
            nm.notify(PLAYBACKSERVICE_STATUS, status);
        } else {
            nm.cancel(PLAYBACKSERVICE_STATUS);
        }
		
        if (callback.get() == null) {
            return;
        }
        try {
            callback.get().onPlayStatusChanged(state);
        } catch (RemoteException e) {
        }

    }

    private final ISqueezeService.Stub squeezeService = new ISqueezeService.Stub() {

	    public void registerCallback(IServiceCallback callback) throws RemoteException {
	    	SqueezeService.this.callback.set(callback);
	    	callback.onConnectionChanged(isConnected.get());
	    }
	    
	    public void unregisterCallback(IServiceCallback callback) throws RemoteException {
	    	SqueezeService.this.callback.compareAndSet(callback, null);
	    }

            public int adjustVolumeBy(int delta) throws RemoteException {
                return 0;
            }

            public boolean isConnected() throws RemoteException {
                return isConnected.get();
            }

            public void startConnect(final String hostPort) throws RemoteException {
                int colonPos = hostPort.indexOf(":");
                boolean noPort = colonPos == -1;
                final int port = noPort? 9090 : Integer.parseInt(hostPort.substring(colonPos + 1));
                final String host = noPort ? hostPort : hostPort.substring(0, colonPos);
                executor.execute(new Runnable() {
                        public void run() {
                            SqueezeService.this.disconnect();
                            Socket socket = new Socket();
                            try {
                                socket.connect(
                                               new InetSocketAddress(host, port),
                                               1500 /* ms timeout */);
                                socketRef.set(socket);
                                Log.d(TAG, "Connected to: " + hostPort);
                                socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                                Log.d(TAG, "writer == " + socketWriter.get());
                                setConnectionState(true);
                                Log.d(TAG, "connection state broadcasted true.");
                                startListeningThread();
                            } catch (SocketTimeoutException e) {
                                Log.e(TAG, "Socket timeout connecting to: " + hostPort);
                            } catch (IOException e) {
                                Log.e(TAG, "IOException connecting to: " + hostPort);
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

            public boolean isPlaying() throws RemoteException {
                return isPlaying.get();
            }

            public boolean getPlayers(List<String> playerId, List<String> playerName)
                throws RemoteException {
                playerId.add("00:04:20:17:04:7f");
                playerName.add("Office");
                playerId.add("00:04:20:05:09:36");
                playerName.add("House");
                return true;
            }

            public boolean setActivePlayer(String playerId) throws RemoteException {
                activePlayerId = playerId;
                return true;
            }

            public String getActivePlayer() throws RemoteException {
                return activePlayerId == null ? "" : activePlayerId;
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
            while (true) {
                String line;
                try {
                    line = in.readLine();
                } catch (IOException e) {
                    Log.v(TAG, "IOException while reading from server: " + e);
                    SqueezeService.this.disconnect();
                    return;
                }
                if (line == null) {
                    // Socket disconnected.  This is expected
                    // if we're not the main connection generation anymore,
                    // else we should notify about it.
                    if (currentConnectionGeneration.get() == generationNumber) {
                        Log.v(TAG, "Server disconnected.");
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
