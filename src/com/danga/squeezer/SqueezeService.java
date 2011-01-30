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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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

import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.itemlists.IServiceGenreListCallback;
import com.danga.squeezer.itemlists.IServicePlayerListCallback;
import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.itemlists.IServiceYearListCallback;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerPlayer;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezeService extends Service {
    private static final String TAG = "SqueezeService";
    private static final int PLAYBACKSERVICE_STATUS = 1;

	private static final int PAGESIZE = 20;
	private static final String ALBUMTAGS = "alyj";
    private static final String SONGTAGS = "alyJ";
	private static final String SONGTAGS_STATUS = SONGTAGS + "K";
    private static final int DEFAULT_PORT = 9090;

    private static final Map<String, Set<String>> acceptedTaggedParameters = initializeTaggedParameters();
	
	private static Map<String, Set<String>> initializeTaggedParameters() {
		Map<String, Set<String>> acceptedTaggedParameters = new HashMap<String, Set<String>>();
		acceptedTaggedParameters.put("players", new HashSet<String>(Arrays.asList("playerprefs",
				"charset")));
		acceptedTaggedParameters.put("artists", new HashSet<String>(Arrays.asList("search",
				"genre_id", "album_id", "tags", "charset")));
		acceptedTaggedParameters.put("albums", new HashSet<String>(Arrays
				.asList("search", "genre_id", "artist_id", "track_id", "year", "compilation", "sort",
						"tags", "charset")));
		acceptedTaggedParameters.put("years", new HashSet<String>(Arrays.asList("charset")));
		acceptedTaggedParameters.put("genres", new HashSet<String>(Arrays.asList("search",
				"artist_id", "album_id", "track_id", "year", "tags", "charset")));
		acceptedTaggedParameters.put("status", new HashSet<String>(Arrays.asList("tags", "charset",
				"subscribe")));
		acceptedTaggedParameters.put("search", new HashSet<String>(Arrays.asList("term", "charset")));
		return acceptedTaggedParameters;
	}
	
    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    // Connection state:
    // TODO: this is getting ridiculous. Move this into ConnectionState class.
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private final AtomicBoolean canRandomplay = new AtomicBoolean(false);
	private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();
	private final AtomicReference<IServiceCallback> callback = new AtomicReference<IServiceCallback>();
	private final AtomicReference<IServicePlayerListCallback> playerListCallback = new AtomicReference<IServicePlayerListCallback>();
	private final AtomicReference<IServiceAlbumListCallback> albumListCallback = new AtomicReference<IServiceAlbumListCallback>();
	private final AtomicReference<IServiceArtistListCallback> artistListCallback = new AtomicReference<IServiceArtistListCallback>();
	private final AtomicReference<IServiceYearListCallback> yearListCallback = new AtomicReference<IServiceYearListCallback>();
	private final AtomicReference<IServiceGenreListCallback> genreListCallback = new AtomicReference<IServiceGenreListCallback>();
	private final AtomicReference<IServiceSongListCallback> songListCallback = new AtomicReference<IServiceSongListCallback>();
	private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
	private final AtomicReference<SqueezerPlayer> activePlayer = new AtomicReference<SqueezerPlayer>();
	private final AtomicReference<SqueezerPlayer> defaultPlayer = new AtomicReference<SqueezerPlayer>();
    
    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<String>();
    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();
    private final AtomicReference<Integer> cliPort = new AtomicReference<Integer>();

    PlayerState playerState = new PlayerState();
    
    private boolean debugLogging = false;
	private int maxListSize;
    
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
        getPreferences();
    }

	private void getPreferences() {
		debugLogging = preferences.getBoolean(Preferences.KEY_DEBUG_LOGGING, false);
        maxListSize = preferences.getInt(Preferences.KEY_MAX_ROWS, 100);
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
        setConnectionState(false, false);
        clearOngoingNotification();
        playerState.clear();
        httpPort.set(null);
        activePlayer.set(null);
    }

    // We register when asynchronous fetches are initiated, and when callbacks are unregistered
    // because these events will make responses from pending requests obsolete
    int _correlationid = 0;
    Map<String, Integer> cmdCorrelationIds = new HashMap<String, Integer>();
    Map<Class<?>, Integer> typeCorrelationIds = new HashMap<Class<?>, Integer>();
    
    private boolean checkCorrelation(Integer registeredCorralationId, int currentCorrelationId) {
    	if (registeredCorralationId == null) return true;
    	return (currentCorrelationId >= registeredCorralationId);
    }
    
    private boolean checkCorrelation(String cmd, int correlationid) {
    	return checkCorrelation(cmdCorrelationIds.get(cmd), correlationid);
    }
    
    private boolean checkCorrelation(Class<? extends SqueezerItem> type, int correlationid) {
    	return checkCorrelation(typeCorrelationIds.get(type), correlationid);
    }

    /**
     * Send an asynchronous request to SqueezeboxServer for the specified items.
     * <p>
     * If start is zero, it means the the list is being reordered, which will make any pending
     * replies, for the given items, obsolete.
     * <p>
     * This will always order one item, to learn the number of items from the server. Remaining
     * items will then be automatically ordered when the response arrives. See
     * {@link #parseSqueezerList(boolean, String, String, List, SqueezerListHandler)} for details.
     * 
     * @param playerid Id of the current player or null
     * @param cmd Identifies the 
     * @param start
     * @param parameters
     */
	private void requestItems(String playerid, String cmd, int start, List<String> parameters) {
		if (start == 0) cmdCorrelationIds.put(cmd, _correlationid);
		StringBuilder sb = new StringBuilder(cmd + " " + start + " "  + 1);
		if (playerid != null) sb.insert(0, playerid + " ");
		if (parameters != null)
			for (String parameter: parameters)
				sb.append(" " + parameter);
		sendCommand(sb.toString());
	}

	private void requestItems(String cmd, int start, List<String> parameters) {
		requestItems(null, cmd, start, parameters);
	}

	private void requestItems(String cmd, int start) {
		requestItems(cmd, start, null);
	}

	private void requestPlayerItems(String cmd, int start, List<String> parameters) {
        if (activePlayer.get() == null) {
            return;
        }
        requestItems(activePlayer.get().getId(), cmd, start, parameters);
	}
   
    private synchronized void sendCommand(String... commands) {
        if (commands.length == 0) return;
        PrintWriter writer = socketWriter.get();
        if (writer == null) return;
        if (commands.length == 1) {
            Log.v(TAG, "SENDING: " + commands[0]);
			writer.println(commands[0] + " correlationid:" + _correlationid++);
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
        	parseSqueezerList("playerindex", tokens, new SqueezerListHandler() {
                List<SqueezerPlayer> players = new ArrayList<SqueezerPlayer>();

        		public Class<? extends SqueezerItem> getDataType() {
        			return SqueezerPlayer.class;
        		}
				
				public void add(Map<String, String> record) {
					SqueezerPlayer player = new SqueezerPlayer(record);
					// Discover the last connected player (if any, otherwise just pick the first one)
					if (defaultPlayer.get() == null || player.getId().equals(lastConnectedPlayer))
						defaultPlayer.set(player);
	                players.add(player);
				}
				
				public boolean processList(boolean rescan, int count, int max, int start) {
					if (playerListCallback.get() != null) {
						// If the player list activity is active, pass the discovered players to it
						try {
							playerListCallback.get().onPlayersReceived(count, max, start, players);
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
        	parseSqueezerList(tokens, new AlbumListHandler());
        	return;
        }
        if (serverLine.startsWith("artists ")) {
        	parseSqueezerList(tokens, new ArtistListHandler());
        	return;
        }
        if (serverLine.startsWith("years ")) {
        	parseSqueezerList("year", tokens, new YearListHandler());
        	return;
        }
        if (serverLine.startsWith("genres ")) {
        	parseSqueezerList(tokens, new GenreListHandler());
        	return;
        }
        if (serverLine.startsWith("search ")) {
        	SqueezeParserInfo genreParserInfo = new SqueezeParserInfo("genres_count", "genre_id", new GenreListHandler());
        	SqueezeParserInfo albumParserInfo = new SqueezeParserInfo("albums_count", "album_id", new AlbumListHandler());
        	SqueezeParserInfo artistParserInfo = new SqueezeParserInfo("contributors_count", "contributor_id", new ArtistListHandler());
        	SqueezeParserInfo songParserInfo = new SqueezeParserInfo("tracks_count", "track_id", new SongListHandler());
        	parseSqueezerList(false, tokens, genreParserInfo, albumParserInfo, artistParserInfo, songParserInfo);
        	return;
        }
		if (serverLine.startsWith("pref ")) {
			if ("httpport".equals(tokens.get(1)) && tokens.size() >= 3) {
				httpPort.set(Integer.parseInt(tokens.get(2)));
				Log.v(TAG, "HTTP port is now: " + httpPort);
			}
			return;
		}
		if (serverLine.startsWith("can ")) {
			if ("randomplay".equals(tokens.get(1)) && tokens.size() >= 3) {
				canRandomplay.set(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
				Log.v(TAG, "HTTP port is now: " + httpPort);
			}
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
        	parsePause(tokens.size() >= 3 ? tokens.get(2) : null);
            return;
        }
        if (command.equals("status")) {
        	if (tokens.size() >= 3 && "-".equals(tokens.get(2)))
        		parseStatusLine(tokens);
        	else {
            	parseSqueezerList(true, "playlist index", "playlist_tracks", tokens, new SongListHandler());
        	}
            return;
        }
        if (command.equals("playlist")) {
            parsePlaylistNotification(tokens);
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

	private void parsePlaylistNotification(List<String> tokens) {
		String notification = tokens.get(2);
		if ("newsong".equals(notification)) {
			if (tokens.size() >= 4) {
				String newSong = decode(tokens.get(3));
				playerState.setCurrentSong(newSong);
				updateOngoingNotification();
				sendMusicChangedCallback();

				// Now also ask for the rest of the status.
				sendPlayerCommand("status - 1 tags:" + SONGTAGS_STATUS);
			}
		} else if ("stop".equals(notification)) {
			setPlayingState(false);
		} else if ("pause".equals(notification)) {
			parsePause(tokens.size() >= 4 ? tokens.get(3) : null);
		}
	}
    
	private void parsePause(String explicitPause) {
		boolean playing = playerState.isPlaying();
		if ("0".equals(explicitPause)) {
			if (playing) setPlayingState(false);
		} else if ("1".equals(explicitPause)) {
			if (!playing) setPlayingState(true);
		}
	}
	
	private void parseMode(String newMode) {
		boolean playing = playerState.isPlaying();
		if ("pause".equals(newMode)) {
			if (playing) setPlayingState(false);
		} else if ("stop".equals(newMode)) {
			if (playing) setPlayingState(false);
		} else if ("play".equals(newMode)) {
			if (!playing) setPlayingState(true);
		}
	}
	
    private void parseStatusLine(List<String> tokens) {
		HashMap<String, String> tokenMap = new HashMap<String, String>();
        String key, value;
		for (String token : tokens) {
            if (token == null || token.length() == 0) continue;
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
	            key = decode(token);
	            value = null;
            } else {
	            key = decode(token.substring(0, colonPos));
	            value = decode(token.substring(colonPos + 3));
            }
			tokenMap.put(key, value);
		}
        
	    boolean musicHasChanged = false;
	    musicHasChanged |= playerState.currentAlbumUpdated(tokenMap.get("album"));
	    musicHasChanged |= playerState.currentArtistUpdated(tokenMap.get("artist"));
	    musicHasChanged |= playerState.currentSongUpdated(tokenMap.get("title"));
	    musicHasChanged |= playerState.currentArtworkTrackIdUpdated(tokenMap.get("artwork_track_id"));
        musicHasChanged |= playerState.currentArtworkUrlUpdated(tokenMap.get("artwork_url"));

        playerState.setPoweredOn(Util.parseDecimalIntOrZero(tokenMap.get("power")) == 1);
	    
        parseMode(tokenMap.get("mode"));
	    
        if (musicHasChanged) {
            updateOngoingNotification();
            sendMusicChangedCallback();
        }

	    int time = Util.parseDecimalIntOrZero(tokenMap.get("time"));
	    int duration = Util.parseDecimalIntOrZero(tokenMap.get("duration"));
        int lastTime = playerState.getCurrentTimeSecond(0);
        if (musicHasChanged || time != lastTime) {
            playerState.setCurrentTimeSecond(time);
            playerState.setCurrentSongDuration(duration);
            sendNewTimeCallback(time, duration);
        }
    }
    
    private void changeActivePlayer(SqueezerPlayer newPlayer) {
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
	            sendCommand(URLEncoder.encode(oldPlayerId) + " status - 1 subscribe:-");
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

	/**
     * Data for {@link SqueezeService#parseSqueezerList(boolean, List, SqueezeParserInfo...)}
     * 
     * @author kaa
     */
    private class SqueezeParserInfo {
    	private String item_delimiter;
    	private String count_id;
    	private SqueezerListHandler handler;

    	/**
    	 * @param countId The label for the tag which contains the total number of results, normally "count".
    	 * @param itemDelimiter As defined for each extended query format command in the squeezeserver CLI documentation.
    	 * @param handler Callback to receive the parsed data.
    	 */
    	public SqueezeParserInfo(String countId, String itemDelimiter, SqueezerListHandler handler) {
			count_id = countId;
			item_delimiter = itemDelimiter;
			this.handler = handler;
		}
    }

    /**
     * <p>
     * Implement this and give it to {@link SqueezeService#parseSqueezerList(List, SqueezerListHandler)} for each
     * extended query format command you which to support.
     * </p>
     * @author Kurt Aaholst
     */
	private interface SqueezerListHandler {
		/**
		 * @return The type of item this handler can handle
		 */
		Class<? extends SqueezerItem> getDataType();
		
		/**
		 * Called for each item received in the current reply. Just store this internally.
		 * @param record
		 */
		void add(Map<String, String> record);
		
		/**
		 * Called when the current reply is completely parsed. Pass the information on to your activity now. If there are
		 * any more data, it is automatically ordered by {@link SqueezeService#parseSqueezerList(List, SqueezerListHandler)}
		 * @param rescan Set if SqueezeServer is currently doing a scan of the music library.
		 * @param count Total number of result for the current query.
		 * @param max The current configured default maximum list size.
		 * @param start Offset for the current list in total results.
		 * @return
		 */
		boolean processList(boolean rescan, int count, int max, int start);
	}

    private class YearListHandler implements SqueezerListHandler {
		List<SqueezerYear> years = new ArrayList<SqueezerYear>();

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerYear.class;
		}

		public void add(Map<String, String> record) {
			years.add(new SqueezerYear(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (yearListCallback.get() != null) {
				try {
					yearListCallback.get().onYearsReceived(count, max, start, years);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

    private class GenreListHandler implements SqueezerListHandler {
		List<SqueezerGenre> genres = new ArrayList<SqueezerGenre>();

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerYear.class;
		}

		public void add(Map<String, String> record) {
			genres.add(new SqueezerGenre(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (genreListCallback.get() != null) {
				try {
					genreListCallback.get().onGenresReceived(count, max, start, genres);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}

	}

	private class ArtistListHandler implements SqueezerListHandler {
		List<SqueezerArtist> artists = new ArrayList<SqueezerArtist>();

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerArtist.class;
		}

		public void add(Map<String, String> record) {
			artists.add(new SqueezerArtist(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (artistListCallback.get() != null) {
				try {
					artistListCallback.get().onArtistsReceived(count, max, start, artists);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

	private class AlbumListHandler implements SqueezerListHandler {
		List<SqueezerAlbum> albums = new ArrayList<SqueezerAlbum>();

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerAlbum.class;
		}

		public void add(Map<String, String> record) {
			albums.add(new SqueezerAlbum(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (albumListCallback.get() != null) {
				try {
					albumListCallback.get().onAlbumsReceived(count, max, start, albums);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

	private class SongListHandler implements SqueezerListHandler {
		List<SqueezerSong> songs = new ArrayList<SqueezerSong>();

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerSong.class;
		}

		public void add(Map<String, String> record) {
			songs.add(new SqueezerSong(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (songListCallback.get() != null) {
				try {
					songListCallback.get().onSongsReceived(count, max, start, songs);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

	/**
	 * <h1>Generic method to parse replies for queries in extended query format</h1>
	 * <p>
	 * This is the control center for asynchronous and paging receiving of data from SqueezeServer.<br/>
	 * Transfer of each data type are started by an asynchronous request by one of the public method in this module.
	 * This method will forward the data using the supplied {@link SqueezerListHandler}, and and order the next page if necessary,
	 * repeating the current query parameters.<br/>
	 * Activities should just initiate the request, and supply a callback to receive a page of data.
	 * <p>
	 * @param playercmd Set this for replies for the current player, to skip the playerid
	 * @param tokens List of tokens with value or key:value.
	 * @param parserInfos Data for each list you expect for the current repsonse
	 */
	private void parseSqueezerList(boolean playercmd, List<String> tokens, SqueezeParserInfo... parserInfos) {
		Log.v(TAG, "Parsing list: " + tokens);

		int n = (playercmd ? -1 : 0);
		String playerid = "", cmd = null;
		int start = 0, itemsPerResponse = 0;
		int correlationid = 0;
		boolean rescan = false;
		Map<String, String> taggedParameters = new HashMap<String, String>();
		Set<String> countIdSet = new HashSet<String>();
		Map<String, SqueezeParserInfo> itemDelimeterMap = new HashMap<String, SqueezeParserInfo>();
		Map<String, Integer> counts = new HashMap<String, Integer>();
		Map<String, String> record = null;

		for (SqueezeParserInfo parserInfo: parserInfos) {
			countIdSet.add(parserInfo.count_id);
			itemDelimeterMap.put(parserInfo.item_delimiter, parserInfo);
		}
		
		SqueezeParserInfo parserInfo = null;
		for (String token : tokens) {
			switch (n) {
			case -1:
				playerid = decode(token) + " ";
				break;
			case 0:
				cmd = token;
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
				
				if (key.equals("rescan"))
					rescan = (Util.parseDecimalIntOrZero(value) == 1);
				else if (key.equals("correlationid"))
					correlationid = Util.parseDecimalIntOrZero(value);
				if (countIdSet.contains(key))
					counts.put(key, Util.parseDecimalIntOrZero(value));
				else {
					if (itemDelimeterMap.get(key) != null) {
						if (record != null) {
							parserInfo.handler.add(record);
							if (debugLogging)
								Log.v(TAG, "record=" + record);
						}
						parserInfo = itemDelimeterMap.get(key);
						record = new HashMap<String, String>();
					}
					if (record != null)
						record.put(key, value);
					else {
						Set<String> acceptedTaggedParameterSet = acceptedTaggedParameters.get(cmd);
						if (acceptedTaggedParameterSet != null && acceptedTaggedParameterSet.contains(key))
							taggedParameters.put(key, value);
					}
				}
				break;
			}
			n++;
		}
		
		if (record != null) parserInfo.handler.add(record);

		processLists: if (checkCorrelation(cmd, correlationid)) {
			int end = start + itemsPerResponse;
			int max = 0;
			for (SqueezeParserInfo parser: parserInfos) {
				if (checkCorrelation(parser.handler.getDataType(), correlationid)) {
				Integer count = counts.get(parser.count_id);
				int countValue = count == null ? 0 : count;
				if (count != null || start == 0) {
					if (!parser.handler.processList(rescan, countValue, maxListSize, start))
						break processLists;
					if (countValue > max)
						max = (count < maxListSize || end > maxListSize ) ? count : maxListSize;
				}
				}
			}
			if (end < max) {
				int pageSize = (end + PAGESIZE > max ? pageSize = max - end : PAGESIZE);
				StringBuilder cmdline = new StringBuilder(cmd + " "	+ end + " " + pageSize);
				for (Entry<String, String> entry : taggedParameters.entrySet())
					cmdline.append(" " + entry.getKey() + ":" + entry.getValue());
				sendCommand(playerid + cmdline.toString());
			}
		}
	}

	/**
	 * Shortcut for {@link #parseSqueezerList(int, String, String, List, SqueezerListHandler)}, with offset=0.
	 * @param item_delimiter
	 * @param tokens
	 * @param handler
	 */
	private void parseSqueezerList(boolean playerCmd, String item_delimiter, String count_id, List<String> tokens, SqueezerListHandler handler) {
		SqueezeParserInfo parserInfo = new SqueezeParserInfo(count_id, item_delimiter, handler);
		parseSqueezerList(playerCmd, tokens, parserInfo);
	}

	/**
	 * Shortcut for {@link #parseSqueezerList(String, List, SqueezerListHandler)}, with item_delimeter = "id".
	 * @param tokens
	 * @param handler
	 */
	private void parseSqueezerList(String item_delimiter, List<String> tokens, SqueezerListHandler handler) {
		parseSqueezerList(false, item_delimiter, "count", tokens, handler);
	}

	/**
	 * Shortcut for {@link #parseSqueezerList(String, List, SqueezerListHandler)}, with item_delimeter = "id".
	 * @param tokens
	 * @param handler
	 */
	private void parseSqueezerList(List<String> tokens, SqueezerListHandler handler) {
		parseSqueezerList("id", tokens, handler);
	}
	
    private void updatePlayerSubscriptionState() {
        // Subscribe or unsubscribe to the player's realtime status updates
        // depending on whether we have an Activity or some sort of client
        // that cares about second-to-second updates.
        if (callback.get() != null) {
            sendPlayerCommand("status - 1 subscribe:1 tags:" + SONGTAGS_STATUS);
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
                "can randomplay ?",   // learn random play function functionality
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
        
        playerState.setPlaying(state);
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
        boolean playing = playerState.isPlaying();  
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
        String song = playerState.getCurrentSongNonNull();
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
        	return canPower() && !playerState.isPoweredOn();
        }
        
        public boolean canPowerOff() {
        	return canPower() && playerState.isPoweredOn();
        }
        
        private boolean canPower() {
            SqueezerPlayer player = activePlayer.get();
        	return isConnected.get() && player != null && player.isCanpoweroff();
        }
        
        public boolean canRandomplay() {
        	return canRandomplay.get();
        }
		
        public boolean togglePausePlay() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            Log.v(TAG, "pause...");
            if (playerState.isPlaying()) {
                // NOTE: we never send ambiguous "pause" toggle commands (without the '1')
                // because then we'd get confused when they came back in to us, not being
                // able to differentiate ours coming back on the listen channel vs. those
                // of those idiots at the dinner party messing around.
                sendPlayerCommand("pause 1");
            } else {
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
            playerState.setPlaying(true);
            sendPlayerCommand("play");
            Log.v(TAG, "played.");
            return true;
        }

        public boolean stop() throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            playerState.setPlaying(false);
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
        
        public boolean playSong(SqueezerSong song) throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("playlistcontrol cmd:load track_id:" + song.getId());
            return true;
        }
        
        public boolean playAlbum(SqueezerAlbum album) throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("playlistcontrol cmd:load album_id:" + album.getId());
            return true;
        }
        
        public boolean randomPlay(String type) throws RemoteException {
            if (!isConnected()) {
                return false;
            }
            sendPlayerCommand("randomplay " + type);
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
            return playerState.isPlaying();
        }

        public void setActivePlayer(SqueezerPlayer player) throws RemoteException {
            changeActivePlayer(player);
        }

        public String getActivePlayerId() throws RemoteException {
            SqueezerPlayer player = activePlayer.get();
            return player == null ? "" : player.getId();
        }

        public String getActivePlayerName() throws RemoteException {
            SqueezerPlayer player = activePlayer.get();
            return (player != null ? player.getName() : null);
        }

        public String currentAlbum() throws RemoteException {
            return playerState.getCurrentAlbumNonNull();
        }

        public String currentArtist() throws RemoteException {
            return playerState.getCurrentArtistNonNull();
        }

        public String currentSong() throws RemoteException {
            return playerState.getCurrentSongNonNull();
        }

        public String currentAlbumArtUrl() throws RemoteException {
            Integer port = httpPort.get();
            if (port == null || port == 0) return "";
            String artworkTrackId = playerState.getCurrentArtworkTrackId();
            String artworkUrl = playerState.getCurrentArtworkUrl();
            if (artworkTrackId != null) {
				return "http://" + currentHost.get() + ":" + port
						+ artworkTrackIdUrl(artworkTrackId);
            } else 
            if (artworkUrl != null) 
                return artworkUrl;
            return "";
        }

        public String getAlbumArtUrl(String artworkTrackId) throws RemoteException {
            Integer port = httpPort.get();
            if (port == null || port == 0) return "";
			return "http://" + currentHost.get() + ":" + port + artworkTrackIdUrl(artworkTrackId);
        }

        private String artworkTrackIdUrl(String artworkTrackId) {
			return "/music/" + artworkTrackId + "/cover.jpg";
        }

        public int getSecondsElapsed() throws RemoteException {
        	return playerState.getCurrentTimeSecond(0);
        }

        public int getSecondsTotal() throws RemoteException {
            return playerState.getCurrentSongDuration(0);
        }

        public void preferenceChanged(String key) throws RemoteException {
            Log.v(TAG, "Preference changed: " + key);
            if (Preferences.KEY_NOTIFY_OF_CONNECTION.equals(key)) {
                updateOngoingNotification();
                return;
            }
            getPreferences();
        }
        
        /* Start an async fetch of the SqueezeboxServer's players */
        public boolean players(int start) throws RemoteException {
            if (!isConnected()) return false;
            requestItems("players", start);
            return true;
        }

		public void registerPlayerListCallback(IServicePlayerListCallback callback)
				throws RemoteException {
            Log.v(TAG, "PlayerListCallback attached.");
	    	SqueezeService.this.playerListCallback.set(callback);
		}

		public void unregisterPlayerListCallback(IServicePlayerListCallback callback) throws RemoteException {
            Log.v(TAG, "PlayerListCallback detached.");
	    	SqueezeService.this.playerListCallback.compareAndSet(callback, null);
			typeCorrelationIds.put(SqueezerPlayer.class, _correlationid);
		}
        
        /* Start an async fetch of the SqueezeboxServer's albums, which are matching the given parameters */
		public boolean albums(int start, String sortOrder, String searchString,
				SqueezerArtist artist, SqueezerYear year, SqueezerGenre genre)
				throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
            parameters.add("tags:" + ALBUMTAGS);
     		parameters.add("sort:" + sortOrder);
			if (searchString != null && searchString.length() > 0)
				parameters.add("search:" + searchString);
			if (artist != null)
				parameters.add("artist_id:" + artist.getId());
			if (year != null)
				parameters.add("year:" + year.getId());
			if (genre != null)
				parameters.add("genre_id:" + genre.getId());
            requestItems("albums", start, parameters);
            return true;
        }
        
		public void registerAlbumListCallback(IServiceAlbumListCallback callback) throws RemoteException {
            Log.v(TAG, "AlbumListCallback attached.");
	    	SqueezeService.this.albumListCallback.set(callback);
		}

		public void unregisterAlbumListCallback(IServiceAlbumListCallback callback) throws RemoteException {
            Log.v(TAG, "AlbumListCallback detached.");
	    	SqueezeService.this.albumListCallback.compareAndSet(callback, null);
			typeCorrelationIds.put(SqueezerAlbum.class, _correlationid);
		}
        
        /* Start an async fetch of the SqueezeboxServer's artists */
   		public boolean artists(int start, String searchString, SqueezerGenre genre) throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
			if (searchString != null && searchString.length() > 0)
				parameters.add("search:" + searchString);
			if (genre != null)
				parameters.add("genre_id:" + genre.getId());
			requestItems("artists", start, parameters);
            return true;
        }

		public void registerArtistListCallback(IServiceArtistListCallback callback) throws RemoteException {
            Log.v(TAG, "ArtistListCallback attached.");
	    	SqueezeService.this.artistListCallback.set(callback);
		}

		public void unregisterArtistListCallback(IServiceArtistListCallback callback) throws RemoteException {
            Log.v(TAG, "ArtistListCallback detached.");
	    	SqueezeService.this.artistListCallback.compareAndSet(callback, null);
			typeCorrelationIds.put(SqueezerArtist.class, _correlationid);
		}
        
        /* Start an async fetch of the SqueezeboxServer's years */
        public boolean years(int start) throws RemoteException {
            if (!isConnected()) return false;
            requestItems("years", start);
            return true;
        }

		public void registerYearListCallback(IServiceYearListCallback callback) throws RemoteException {
            Log.v(TAG, "YearListCallback attached.");
	    	SqueezeService.this.yearListCallback.set(callback);
		}

		public void unregisterYearListCallback(IServiceYearListCallback callback) throws RemoteException {
            Log.v(TAG, "YearListCallback detached.");
	    	SqueezeService.this.yearListCallback.compareAndSet(callback, null);
			typeCorrelationIds.put(SqueezerYear.class, _correlationid);
		}
        
        /* Start an async fetch of the SqueezeboxServer's genres */
        public boolean genres(int start) throws RemoteException {
            if (!isConnected()) return false;
            requestItems("genres", start);
            return true;
        }

		public void registerGenreListCallback(IServiceGenreListCallback callback) throws RemoteException {
            Log.v(TAG, "GenreListCallback attached.");
	    	SqueezeService.this.genreListCallback.set(callback);
		}

		public void unregisterGenreListCallback(IServiceGenreListCallback callback) throws RemoteException {
            Log.v(TAG, "GenreListCallback detached.");
	    	SqueezeService.this.genreListCallback.compareAndSet(callback, null);
			typeCorrelationIds.put(SqueezerGenre.class, _correlationid);
		}
        
		/* Start an async fetch of the SqueezeboxServer's songs */
		public boolean songs(int start, String sortOrder, String searchString, SqueezerAlbum album,
				SqueezerArtist artist, SqueezerYear year, SqueezerGenre genre) throws RemoteException {
			if (!isConnected())
				return false;
			List<String> parameters = new ArrayList<String>();
			parameters.add("tags:" + SONGTAGS);
			parameters.add("sort:" + sortOrder);
			if (searchString != null && searchString.length() > 0)
				parameters.add("search:" + searchString);
			if (album != null)
				parameters.add("album_id:" + album.getId());
			if (artist != null)
				parameters.add("artist_id:" + artist.getId());
			if (year != null)
				parameters.add("year:" + year.getId());
			if (genre != null)
				parameters.add("genre_id:" + genre.getId());
			requestItems("songs", start, parameters);
			return true;
		}
        
        /* Start an async fetch of the SqueezeboxServer's current playlist */
        public boolean currentPlaylist(int start) throws RemoteException {
            if (!isConnected()) return false;
            requestPlayerItems("status", start, Arrays.asList("tags:" + SONGTAGS));
            return true;
        }

		public void registerSongListCallback(IServiceSongListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback attached.");
	    	SqueezeService.this.songListCallback.set(callback);
		}

		public void unregisterSongListCallback(IServiceSongListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback detached.");
	    	SqueezeService.this.songListCallback.compareAndSet(callback, null);
			typeCorrelationIds.put(SqueezerSong.class, _correlationid);
		}
        
        /* Start an asynchronous search of the SqueezeboxServer's library */
   		public boolean search(int start, String searchString) throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
			if (searchString != null && searchString.length() > 0)
				parameters.add("term:" + searchString);
			requestItems("search", start, parameters);
            return true;
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
