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
import java.util.ArrayList;
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
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.danga.squeezer.model.SqueezeAlbum;
import com.danga.squeezer.model.SqueezeArtist;
import com.danga.squeezer.model.SqueezePlayer;
import com.danga.squeezer.model.SqueezeSong;

public class SqueezeService extends Service {
    private static final String TAG = "SqueezeService";
    private static final int PLAYBACKSERVICE_STATUS = 1;

	private static final int PAGESIZE = 20;
	private static final String ALBUMTAGS = "alyj";
    private static final String SONGTAGS = "alyJ";
	private static final String SONGTAGS_STATUS = SONGTAGS + "qw";
    private static final int DEFAULT_PORT = 9090;
	
    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    // Connection state:
    // TODO: this is getting ridiculous. Move this into ConnectionState class.
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPoweredOn = new AtomicBoolean(false);
    private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();
    private final AtomicReference<IServiceCallback> callback =
        new AtomicReference<IServiceCallback>();
    private final AtomicReference<IServicePlayerListCallback> playerListCallback =
        new AtomicReference<IServicePlayerListCallback>();
    private final AtomicReference<IServiceAlbumListCallback> albumListCallback =
        new AtomicReference<IServiceAlbumListCallback>();
    private final AtomicReference<IServiceArtistListCallback> artistListCallback =
        new AtomicReference<IServiceArtistListCallback>();
    private final AtomicReference<IServiceSongListCallback> songListCallback =
        new AtomicReference<IServiceSongListCallback>();
    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
    private final AtomicReference<SqueezePlayer> activePlayer = new AtomicReference<SqueezePlayer>();
    private final AtomicReference<SqueezePlayer> defaultPlayer = new AtomicReference<SqueezePlayer>();

    private final AtomicReference<String> currentSong = new AtomicReference<String>();
    private final AtomicReference<String> currentArtist = new AtomicReference<String>();
    private final AtomicReference<String> currentAlbum = new AtomicReference<String>();
    private final AtomicReference<String> currentArtworkTrackId = new AtomicReference<String>();
    private final AtomicReference<Integer> currentTimeSecond = new AtomicReference<Integer>();
    private final AtomicReference<Integer> currentSongDuration = new AtomicReference<Integer>();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<String>();
    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();
    private final AtomicReference<Integer> cliPort = new AtomicReference<Integer>();
    
    private boolean debugLogging = false;
    
    private WifiManager.WifiLock wifiLock;
    private SharedPreferences preferences;

    @Override
        public void onCreate() {
    	super.onCreate();
    	
        // Clear leftover notification in case this service previously got killed while playing                                                
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
        
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(
                WifiManager.WIFI_MODE_FULL, "Squeezer_WifiLock");
        
        preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        debugLogging = preferences.getBoolean(Preferences.KEY_DEBUG_LOGGING, false);
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
        setConnectionState(false, false);
        clearOngoingNotification();
        currentSong.set(null);
        currentArtist.set(null);
        currentAlbum.set(null);
        currentArtworkTrackId.set(null);
        httpPort.set(null);
        activePlayer.set(null);
        currentTimeSecond.set(null);
        currentSongDuration.set(null);
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
        if (activePlayer.get() == null) {
            return;
        }
        sendCommand(activePlayer.get().getId() + " " + command);
    }
	
    private void onLineReceived(String serverLine) {
        if (debugLogging) Log.v(TAG, "LINE: " + serverLine);
        List<String> tokens = Arrays.asList(serverLine.split(" "));
        if (tokens.size() < 2) {
            return;
        }
        if (serverLine.startsWith("players ")) {
            // TODO: can this block (sqlite lookup via binder call?)  Might want to move it elsewhere.
        	final String lastConnectedPlayer = preferences.getString(Preferences.KEY_LASTPLAYER, null);
        	Log.v(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);
        	parseSqueezerList("playerindex", tokens, new SqueezeListHandler() {
                List<SqueezePlayer> players = new ArrayList<SqueezePlayer>();
				
				public void add(Map<String, String> record) {
					SqueezePlayer player = new SqueezePlayer(record);
					// Discover the last connected player (if any, otherwise just pixh the first one)
					if (defaultPlayer.get() == null || player.getId().equals(lastConnectedPlayer))
						defaultPlayer.set(player);
	                players.add(player);
				}
				
				public boolean processList(int count, int start) {
					if (playerListCallback.get() != null) {
						// If the player list activity is active, pass the discovered players to it
						try {
							playerListCallback.get().onPlayersReceived(count, start, players);
						} catch (RemoteException e) {
							Log.e(TAG, e.toString());
							return false;
						}
					} else
					if (start + players.size() >= count) {
						// Otherwise set the last connected player as the active player
			        	if (defaultPlayer.get() != null) changeActivePlayer(defaultPlayer.get());
					}
					return true;
				}
			});
            return;
        }
        if (serverLine.startsWith("albums ")) {
        	parseSqueezerList(tokens, new SqueezeListHandler() {
				List<SqueezeAlbum> albums = new ArrayList<SqueezeAlbum>();
				
				public void add(Map<String, String> record) {
					albums.add(new SqueezeAlbum(record));
				}
				
				public boolean processList(int count, int start) {
					if (albumListCallback.get() != null) {
						try {
							albumListCallback.get().onAlbumsReceived(count, start, albums);
							return true;
						} catch (RemoteException e) {
							Log.e(TAG, e.toString());
						}
					}
					return false;
				}
			});
        	return;
        }
        if (serverLine.startsWith("artists ")) {
        	parseSqueezerList(tokens, new SqueezeListHandler() {
				List<SqueezeArtist> artists = new ArrayList<SqueezeArtist>();
				
				public void add(Map<String, String> record) {
					artists.add(new SqueezeArtist(record));
				}
				
				public boolean processList(int count, int start) {
					if (artistListCallback.get() != null) {
						try {
							artistListCallback.get().onArtistsReceived(count, start, artists);
							return true;
						} catch (RemoteException e) {
							Log.e(TAG, e.toString());
						}
					}
					return false;
				}
			});
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
        String activePlayerId = (activePlayer.get() != null ? activePlayer.get().getId() : null);
        if (activePlayerId == null || activePlayerId.length() == 0 ||
            !decode(tokens.get(0)).equals(activePlayerId)) {
            // Different player that we're not interested in.   
            // (yet? maybe later.)
            return;
        }
        String command = tokens.get(1);
        if (command == null) return;
        if (serverLine.contains("prefset server volume")) {
            String newVolume = tokens.get(4);
            Log.v(TAG, "New volume is: " + newVolume);
            sendNewVolumeCallback(Util.parseDecimalIntOrZero(newVolume));
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
        	if (tokens.size() >= 3 && "-".equals(tokens.get(2)))
        		parseStatusLine(tokens);
        	else {
            	parseSqueezerList(1, "playlist index", "playlist_tracks", tokens, new SqueezeListHandler() {
    				List<SqueezeSong> songs = new ArrayList<SqueezeSong>();
    				
    				public void add(Map<String, String> record) {
    					songs.add(new SqueezeSong(record));
    				}
    				
    				public boolean processList(int count, int start) {
    					if (songListCallback.get() != null) {
    						try {
    							songListCallback.get().onSongsReceived(count, start, songs);
    							return true;
    						} catch (RemoteException e) {
    							Log.e(TAG, e.toString());
    						}
    					}
    					return false;
    				}
    			});
        	}
            return;
        }
        if (command.equals("playlist")) {
            if (tokens.size() >= 4 && "newsong".equals(tokens.get(2))) {
                String newSong = decode(tokens.get(3));
                currentSong.set(newSong);
                updateOngoingNotification();
                sendMusicChangedCallback();
                
                // Now also ask for the rest of the status.
                sendPlayerCommand("status - 1 tags:" + SONGTAGS_STATUS);
            }
            return;
        }

    }

	private void sendNewVolumeCallback(int newVolume) {
        if (callback.get() == null) return;
        try {
            callback.get().onVolumeChange(newVolume);
        } catch (RemoteException e) {
        }
    }

    private void sendNewTimeCallback(int secondsIn, int secondsTotal) {
        if (callback.get() == null) return;
        try {
            callback.get().onTimeInSongChange(secondsIn, secondsTotal);
        } catch (RemoteException e) {
        }
    }
    
    private void parseStatusLine(List<String> tokens) {
        int n = 0;
        boolean musicHasChanged = false;
        boolean sawArtworkId = false;
        int time = 0;
        int duration = 0;

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
            } else
            if (key.equals("mode")) {
                if (value.equals("pause")) {
                    setPlayingState(false);
                } else if (value.equals("play")) {
                    setPlayingState(true);
                }
                continue;
            } else
            if (key.equals("artist")) {
                if (Util.atomicStringUpdated(currentArtist, value)) musicHasChanged = true;
                continue;
            } else
            if (key.equals("title")) {
                if (Util.atomicStringUpdated(currentSong, value)) musicHasChanged = true;
                continue;
            } else
            if (key.equals("album")) {
                if (Util.atomicStringUpdated(currentAlbum, value)) musicHasChanged = true;
                continue;
            } else
            if (key.equals("artwork_track_id")) {
                currentArtworkTrackId.set(value);
                sawArtworkId = true;
                continue;
            } else
            if (key.equals("time")) {
                time = Util.parseDecimalIntOrZero(value);
                continue;
            } else
            if (key.equals("duration")) {
                duration = Util.parseDecimalIntOrZero(value);
                continue;
            } else
            if (key.equals("power")) {
            	isPoweredOn.set(Util.parseDecimalIntOrZero(value) == 1);
                continue;
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
        Integer lastTimeInteger = currentTimeSecond.get();
        int lastTime = lastTimeInteger == null ? 0 : lastTimeInteger.intValue();
        if (musicHasChanged || time != lastTime) {
            currentTimeSecond.set(time);
            currentSongDuration.set(duration);
            sendNewTimeCallback(time, duration);
        }
    }
    
    private void changeActivePlayer(SqueezePlayer newPlayer) {
		if (newPlayer == null) {
            return;
        }

        Log.v(TAG, "Active player now: " + newPlayer);
        final String playerId = newPlayer.getId();
        String oldPlayerId =  (activePlayer.get() != null ? activePlayer.get().getId() : null);
        boolean changed = false;
        if (oldPlayerId == null || !oldPlayerId.equals(playerId)) {
        	if (oldPlayerId != null) {
	            // Unsubscribe from the old player's status.  (despite what
	            // the docs say, multiple subscribes can be active and flood us.)
	            sendCommand(URLEncoder.encode(oldPlayerId) + " status - 1 subscribe:0");
        	}

            activePlayer.set(newPlayer);
            changed = true;
        }
        
        // Start an async fetch of its status.
        sendPlayerCommand("status - 1 tags:" + SONGTAGS_STATUS);

        if (changed) {
            updatePlayerSubscriptionState();
        
            // NOTE: this involves a write and can block (sqlite lookup via binder call), so
            // should be done off-thread, so we can process service requests & send our callback
            // as quickly as possible.
            executor.execute(new Runnable() {
                public void run() {
                    Log.v(TAG, "Saving " + Preferences.KEY_LASTPLAYER + "=" + playerId);
                    final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(Preferences.KEY_LASTPLAYER, playerId);
                    editor.commit();
                }
            });
        }
       
        if (callback.get() != null) {
            try {
                callback.get().onPlayerChanged(playerId, newPlayer.getName());
            } catch (RemoteException e) {}
        }
    }


	private interface SqueezeListHandler {
		void add(Map<String, String> record);
		boolean processList(int count, int start);
	}

	private void parseSqueezerList(int offset, String item_delimiter, String count_id, List<String> tokens, SqueezeListHandler handler) {
		Log.v(TAG, "Parsing list: " + tokens);
		int n = -offset;
		Map<String, String> record = null;
		StringBuilder cmd = new StringBuilder();
		String tags = null, artist_id = null;
		int start = 0, itemsPerResponse = 0, count = 0;
		for (String token : tokens) {
			if (n < 0) { n++; continue; }
			switch (n) {
			case 0:
				cmd.append(token);
				break;
			case 1:
				start = Util.parseDecimalIntOrZero(token);
				break;
			case 2:
				itemsPerResponse = Util.parseDecimalIntOrZero(token);
				break;
			default:
				int colonPos = token.indexOf("%3A");
				if (colonPos == -1) {
					Log.e(TAG, "Expected colon in list token. '" + token + "'");
					return;
				}
				String key = decode(token.substring(0, colonPos));
				String value = decode(token.substring(colonPos + 3));
				if (debugLogging)
					Log.v(TAG, "key=" + key + ", value: " + value);
				if (key.equals(count_id))
					count = Util.parseDecimalIntOrZero(value);
				else if (key.equals("tags"))
					tags = value;
				else if (key.equals("artist_id"))
					artist_id = value;
				else if (key.equals(item_delimiter)) {
					if (record != null)
						handler.add(record);
					record = new HashMap<String, String>();
				}
				if (record != null)
					record.put(key, value);
				break;
			}
			n++;
		}
		
		if (record != null) {
			handler.add(record);
		}

		if (handler.processList(count, start)) {
			if (start + itemsPerResponse < count) {
				cmd.append(" " + (start + itemsPerResponse) + " " + PAGESIZE);
				if (tags != null)
					cmd.append(" tags:" + tags);
				if (artist_id != null)
					cmd.append(" artist_id:" + artist_id);
				sendCommand(cmd.toString());
			}
		}
	}

	private void parseSqueezerList(String item_delimiter, List<String> tokens, SqueezeListHandler handler) {
		parseSqueezerList(0, item_delimiter, "count", tokens, handler);
	}

	private void parseSqueezerList(List<String> tokens, SqueezeListHandler handler) {
		parseSqueezerList("id", tokens, handler);
	}
	
    private void updatePlayerSubscriptionState() {
        // Subscribe or unsubscribe to the player's realtime status updates
        // depending on whether we have an Activity or some sort of client
        // that cares about second-to-second updates.
        if (callback.get() != null) {
            sendPlayerCommand("status - 1 subscribe:1");
        } else {
            sendPlayerCommand("status - 1 subscribe:-");
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

        defaultPlayer.set(null);
        sendCommand("listen 1",
                "players 0 1",   // initiate an async player fetch
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
        // TODO: this might be running in the wrong thread.  Is wifiLock thread-safe?
        if (state && !wifiLock.isHeld()) {
            Log.v(TAG, "Locking wifi while playing.");
            wifiLock.acquire();
        }
        if (!state && wifiLock.isHeld()) {
            Log.v(TAG, "Unlocking wifi.");
            wifiLock.release();
        }
        
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
        boolean playing = isPlaying.get();  
        if (!playing) {
            if (!preferences.getBoolean(Preferences.KEY_NOTIFY_OF_CONNECTION, false)) {
                clearOngoingNotification();
                return;
            }
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
        if (playing) {
            status.setLatestEventInfo(this, "Music Playing", song, pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
        } else {
            status.setLatestEventInfo(this, "Squeezer's Connected", "No music is playing.", pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.logo;
        }
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
            Log.v(TAG, "Callback attached.");
	    	SqueezeService.this.callback.set(callback);
	    	updatePlayerSubscriptionState();
	    }
	    
	    public void unregisterCallback(IServiceCallback callback) throws RemoteException {
            Log.v(TAG, "Callback detached.");
	    	SqueezeService.this.callback.compareAndSet(callback, null);
            updatePlayerSubscriptionState();
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

        public void startConnect(String hostPort) throws RemoteException {
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
                    SqueezeService.this.disconnect();
                    Socket socket = new Socket();
                    try {
                        socket.connect(new InetSocketAddress(host, port),
                                       4000 /* ms timeout */);
                        socketRef.set(socket);
                        Log.d(TAG, "Connected to: " + cleanHostPort);
                        socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                        Log.d(TAG, "writer == " + socketWriter.get());
                        setConnectionState(true, true);
                        Log.d(TAG, "connection state broadcasted true.");
                        onCliPortConnectionEstablished();
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

        public void disconnect() throws RemoteException {
            if (!isConnected()) return;
            SqueezeService.this.disconnect();
        }

        public boolean powerOn() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("power 1");
            return true;
        }

        public boolean powerOff() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("power 0");
            return true;
        }
        
        public boolean canPowerOn() {
        	return canPower() && !isPoweredOn.get();
        }
        
        public boolean canPowerOff() {
        	return canPower() && isPoweredOn.get();
        }
        
        private boolean canPower() {
            SqueezePlayer player = activePlayer.get();
        	return isConnected.get() && player != null && player.isCanpoweroff();
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
        
        public boolean playAlbum(SqueezeAlbum album) throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("playlistcontrol cmd:load album_id:" + album.getId());
            return true;
        }
        
        public boolean playlistIndex(int index) throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("playlist index " + index);
            return true;
        }
        
        public boolean isPlaying() throws RemoteException {
            return isPlaying.get();
        }

        public void setActivePlayer(SqueezePlayer player) throws RemoteException {
            changeActivePlayer(player);
        }

        public String getActivePlayerId() throws RemoteException {
            SqueezePlayer player = activePlayer.get();
            return player == null ? "" : player.getId();
        }

        public String getActivePlayerName() throws RemoteException {
            SqueezePlayer player = activePlayer.get();
            return (player != null ? player.getName() : null);
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
            	SqueezePlayer player = activePlayer.get();
            	if (player == null) return "";
                return "http://" + currentHost.get() + ":" + port
                    + "/music/current/cover?player=" + player.getId()
                    + "&song=" + URLEncoder.encode(currentSong());
            }
        }

        public int getSecondsElapsed() throws RemoteException {
            Integer seconds = currentTimeSecond.get();
            return seconds == null ? 0 : seconds.intValue();
        }

        public int getSecondsTotal() throws RemoteException {
            Integer seconds = currentSongDuration.get();
            return seconds == null ? 0 : seconds.intValue();
        }

        public void preferenceChanged(String key) throws RemoteException {
            Log.v(TAG, "Preference changed: " + key);
            if (Preferences.KEY_NOTIFY_OF_CONNECTION.equals(key)) {
                updateOngoingNotification();
                return;
            }
            if (Preferences.KEY_DEBUG_LOGGING.equals(key)) {
                debugLogging = preferences.getBoolean(key, false);
                return;
            }
        }
        
        /* Start and async fetch of the squeezeservers players */
        public boolean players() throws RemoteException {
            if (!isConnected()) return false;
            sendCommand("players 0 1");
            return true;
        }

		public void registerPlayerListCallback(IServicePlayerListCallback callback)
				throws RemoteException {
            Log.v(TAG, "PlayerListCallback attached.");
	    	SqueezeService.this.playerListCallback.set(callback);
		}

		public void unregisterPlayerListCallback(IServicePlayerListCallback callback)
				throws RemoteException {
            Log.v(TAG, "PlayerListCallback detached.");
	    	SqueezeService.this.playerListCallback.compareAndSet(callback, null);
		}
        
        /* Start an async fetch of the squeezeservers albums, which are matching the given parameters */
        public boolean albums(SqueezeArtist artist) throws RemoteException {
            if (!isConnected()) return false;
            StringBuilder sb = new StringBuilder("albums 0 1 tags:" + ALBUMTAGS);
            if (artist != null)
            	sb.append(" artist_id:"+ artist.getId());
            sendCommand(sb.toString());
            return true;
        }
        
		public void registerAlbumListCallback(IServiceAlbumListCallback callback) throws RemoteException {
            Log.v(TAG, "AlbumListCallback attached.");
	    	SqueezeService.this.albumListCallback.set(callback);
		}

		public void unregisterAlbumListCallback(IServiceAlbumListCallback callback) throws RemoteException {
            Log.v(TAG, "AlbumListCallback detached.");
	    	SqueezeService.this.albumListCallback.compareAndSet(callback, null);
		}
        
        /* Start and async fetch of the squeezeservers artists */
        public boolean artists() throws RemoteException {
            if (!isConnected()) return false;
            sendCommand("artists 0 1");
            return true;
        }

		public void registerArtistListCallback(IServiceArtistListCallback callback) throws RemoteException {
            Log.v(TAG, "ArtistListCallback attached.");
	    	SqueezeService.this.artistListCallback.set(callback);
		}

		public void unregisterArtistListCallback(IServiceArtistListCallback callback) throws RemoteException {
            Log.v(TAG, "ArtistListCallback detached.");
	    	SqueezeService.this.artistListCallback.compareAndSet(callback, null);
		}
        
        /* Start and async fetch of the squeezeservers current playlist */
        public boolean songs() throws RemoteException {
            if (!isConnected()) return false;
            sendCommand("status 0 1 tags:" + SONGTAGS);
            return true;
        }

		public void registerSongListCallback(IServiceSongListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback attached.");
	    	SqueezeService.this.songListCallback.set(callback);
		}

		public void unregisterSongListCallback(IServiceSongListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback detached.");
	    	SqueezeService.this.songListCallback.compareAndSet(callback, null);
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
}
