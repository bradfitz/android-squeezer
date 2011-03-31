package com.danga.squeezer.service;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.RemoteException;
import android.util.Log;

import com.danga.squeezer.Preferences;
import com.danga.squeezer.SqueezerItem;
import com.danga.squeezer.Util;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerPlayer;
import com.danga.squeezer.model.SqueezerPlaylist;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerYear;

class SqueezerCLIImpl {
    private static final String TAG = "SqueezerMusicLibraryCmds";

	private static final int PAGESIZE = 20;
	
	class ExtendedQueryFormatCmd {
		boolean playerSpecific;
		String cmd;
		private Set<String> taggedParameters;
		private SqueezeParserInfo[] parserInfos;

		private ExtendedQueryFormatCmd(boolean playerSpecific, String cmd, Set<String> taggedParameters, SqueezeParserInfo... parserInfos) {
			this.playerSpecific = playerSpecific;
			this.cmd = cmd;
			this.taggedParameters = taggedParameters;
			this.parserInfos = parserInfos;
		}

		public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters, String itemDelimiter, SqueezerListHandler handler) {
			this(false, cmd, taggedParameters, new SqueezeParserInfo(itemDelimiter, handler));
		}

		public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters, SqueezerListHandler handler) {
			this(false, cmd, taggedParameters, new SqueezeParserInfo(handler));
		}
		
	}

	final ExtendedQueryFormatCmd[] extQueryFormatCmds = initializeExtQueryFormatCmds();
	final Map<String, ExtendedQueryFormatCmd> extQueryFormatCmdMap = initializeExtQueryFormatCmdMap();

	private ExtendedQueryFormatCmd[] initializeExtQueryFormatCmds() {
		List<ExtendedQueryFormatCmd> list = new ArrayList<ExtendedQueryFormatCmd>();
		
		list.add(
			new ExtendedQueryFormatCmd(
					"players",
					new HashSet<String>(Arrays.asList("playerprefs", "charset")),
					"playerid",
		        	new SqueezerListHandler() {
						SqueezerPlayer defaultPlayer;
			        	String lastConnectedPlayer;
		                private List<SqueezerPlayer> players;

		        		public Class<? extends SqueezerItem> getDataType() {
		        			return SqueezerPlayer.class;
		        		}
		        		
		        		public void clear() {
				        	lastConnectedPlayer = service.preferences.getString(Preferences.KEY_LASTPLAYER, null);
		        			defaultPlayer = null;
				        	Log.v(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);
			                players = new ArrayList<SqueezerPlayer>(){private static final long serialVersionUID = 4283732322286492895L;};
		        		}
						
						public void add(Map<String, String> record) {
							SqueezerPlayer player = new SqueezerPlayer(record);
							// Discover the last connected player (if any, otherwise just pick the first one)
							if (defaultPlayer == null || player.getId().equals(lastConnectedPlayer))
								defaultPlayer = player;
			                players.add(player);
						}
						
						public boolean processList(boolean rescan, int count, int max, int start) {
							if (service.playerListCallback.get() != null) {
								// If the player list activity is active, pass the discovered players to it
								try {
									service.playerListCallback.get().onPlayersReceived(count, max, start, players);
								} catch (RemoteException e) {
									Log.e(TAG, e.toString());
									return false;
								}
							} else
							if (start + players.size() >= count) {
								// Otherwise set the last connected player as the active player
					        	if (defaultPlayer != null)
					        		service.changeActivePlayer(defaultPlayer);
							}
							return true;
						}
					}
				)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"artists",
				new HashSet<String>(Arrays.asList("search",	"genre_id", "album_id", "tags", "charset")),
				new ArtistListHandler()
			)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"albums",
				new HashSet<String>(Arrays.asList("search", "genre_id", "artist_id", "track_id", "year", "compilation", "sort", "tags", "charset")),
				new AlbumListHandler()
			)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"years",
				new HashSet<String>(Arrays.asList("charset")),
				"year",
				new YearListHandler()
			)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"genres",
				new HashSet<String>(Arrays.asList("search", "artist_id", "album_id", "track_id", "year", "tags", "charset")),
				new GenreListHandler()
			)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"songs",
				new HashSet<String>(Arrays.asList("genre_id", "artist_id", "album_id", "year", "search", "tags", "sort", "charset")),
				new SongListHandler()
			)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"playlists",
				new HashSet<String>(Arrays.asList("search", "tags", "charset")),
				new PlaylistsHandler())
		);
		list.add(
			new ExtendedQueryFormatCmd(
				"playlists tracks",
				new HashSet<String>(Arrays.asList("playlist_id", "tags", "charset")),
				"playlist index",
				new SongListHandler())
		);
		list.add(
			new ExtendedQueryFormatCmd(
				false,
				"search",
				new HashSet<String>(Arrays.asList("term", "charset")),
	        	new SqueezeParserInfo("genres_count", "genre_id", new GenreListHandler()),
	        	new SqueezeParserInfo("albums_count", "album_id", new AlbumListHandler()),
	        	new SqueezeParserInfo("contributors_count", "contributor_id", new ArtistListHandler()),
	        	new SqueezeParserInfo("tracks_count", "track_id", new SongListHandler())
			)
		);
		list.add(
			new ExtendedQueryFormatCmd(
				true,
				"status",
				new HashSet<String>(Arrays.asList("tags", "charset", "subscribe")),
				new SqueezeParserInfo("playlist_tracks", "playlist index", new SongListHandler())
			)
		);

		return list.toArray(new ExtendedQueryFormatCmd[]{});
	}
	
    private Map<String, ExtendedQueryFormatCmd> initializeExtQueryFormatCmdMap() {
        Map<String, ExtendedQueryFormatCmd> map = new HashMap<String, ExtendedQueryFormatCmd>();
        for (ExtendedQueryFormatCmd cmd: extQueryFormatCmds)
        	map.put(cmd.cmd, cmd);
        return map;
	}


	private SqueezeService service;

	SqueezerCLIImpl(SqueezeService service) {
		this.service = service;
	}

	
	// All requests are tagged with a correlation id, which can be used when
	// asynchronous responses are received.
    int _correlationid = 0;
    
    synchronized void sendCommand(String... commands) {
        if (commands.length == 0) return;
        PrintWriter writer = service.connectionState.getSocketWriter();
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
	
    void sendPlayerCommand(String command) {
        if (service.connectionState.getActivePlayer() == null) {
            return;
        }
        sendCommand(Util.encode(service.connectionState.getActivePlayer().getId()) + " " + command);
    }
    
    boolean checkCorrelation(Integer registeredCorralationId, int currentCorrelationId) {
    	if (registeredCorralationId == null) return true;
    	return (currentCorrelationId >= registeredCorralationId);
    }
    

    // We register when asynchronous fetches are initiated, and when callbacks are unregistered
    // because these events will make responses from pending requests obsolete
    private Map<String, Integer> cmdCorrelationIds = new HashMap<String, Integer>();
    private Map<Class<?>, Integer> typeCorrelationIds = new HashMap<Class<?>, Integer>();
    
    void cancelRequests(Class<?> clazz) {
		typeCorrelationIds.put(clazz, _correlationid);
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
		if (playerid != null) sb.insert(0, Util.encode(playerid) + " ");
		if (parameters != null)
			for (String parameter: parameters)
				sb.append(" " + Util.encode(parameter));
		sendCommand(sb.toString());
	}

	void requestItems(String cmd, int start, List<String> parameters) {
		requestItems(null, cmd, start, parameters);
	}

	void requestItems(String cmd, int start) {
		requestItems(cmd, start, null);
	}

	void requestPlayerItems(String cmd, int start, List<String> parameters) {
        if (service.connectionState.getActivePlayer() == null) {
            return;
        }
        requestItems(service.connectionState.getActivePlayer().getId(), cmd, start, parameters);
	}

   
	
	/**
     * Data for {@link SqueezerCLIImpl#parseSqueezerList(boolean, List, SqueezeParserInfo...)}
     * 
     * @author kaa
     */
    private static class SqueezeParserInfo {
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
    	
    	public SqueezeParserInfo(String itemDelimiter, SqueezerListHandler handler) {
    		this("count", itemDelimiter, handler);
    	}
    	
    	public SqueezeParserInfo(SqueezerListHandler handler) {
    		this("id", handler);
    	}
    }

    /**
     * <p>
     * Implement this and give it to {@link SqueezerCLIImpl#parseSqueezerList(List, SqueezerListHandler)} for each
     * extended query format command you wish to support.
     * </p>
     * @author Kurt Aaholst
     */
	private static interface SqueezerListHandler {
		/**
		 * @return The type of item this handler can handle
		 */
		Class<? extends SqueezerItem> getDataType();
		
		/**
		 * Prepare for parsing an extended query format response
		 */
		void clear();
		
		/**
		 * Called for each item received in the current reply. Just store this internally.
		 * @param record
		 */
		void add(Map<String, String> record);
		
		/**
		 * Called when the current reply is completely parsed. Pass the information on to your activity now. If there are
		 * any more data, it is automatically ordered by {@link SqueezerCLIImpl#parseSqueezerList(List, SqueezerListHandler)}
		 * @param rescan Set if SqueezeServer is currently doing a scan of the music library.
		 * @param count Total number of result for the current query.
		 * @param max The current configured default maximum list size.
		 * @param start Offset for the current list in total results.
		 * @return
		 */
		boolean processList(boolean rescan, int count, int max, int start);
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
	void parseSqueezerList(ExtendedQueryFormatCmd cmd, List<String> tokens) {
		Log.v(TAG, "Parsing list: " + tokens);

		int ofs = cmd.cmd.split(" ").length + (cmd.playerSpecific ? 1 : 0);
		int actionsCount = 0;
		String playerid = (cmd.playerSpecific ? tokens.get(0) + " " : "");
		int start = Util.parseDecimalIntOrZero(tokens.get(ofs));
		int itemsPerResponse = Util.parseDecimalIntOrZero(tokens.get(ofs+1));

		int correlationid = 0;
		boolean rescan = false;
		Map<String, String> taggedParameters = new HashMap<String, String>();
		Set<String> countIdSet = new HashSet<String>();
		Map<String, SqueezeParserInfo> itemDelimeterMap = new HashMap<String, SqueezeParserInfo>();
		Map<String, Integer> counts = new HashMap<String, Integer>();
		Map<String, String> record = null;

		for (SqueezeParserInfo parserInfo: cmd.parserInfos) {
			parserInfo.handler.clear();
			countIdSet.add(parserInfo.count_id);
			itemDelimeterMap.put(parserInfo.item_delimiter, parserInfo);
		}
		
		SqueezeParserInfo parserInfo = null;
		for (int idx = ofs+2; idx < tokens.size(); idx++) { String token = tokens.get(idx);
			int colonPos = token.indexOf("%3A");
			if (colonPos == -1) {
				Log.e(TAG, "Expected colon in list token. '" + token + "'");
				return;
			}
			String key = Util.decode(token.substring(0, colonPos));
			String value = Util.decode(token.substring(colonPos + 3));
			if (service.debugLogging)
				Log.v(TAG, "key=" + key + ", value: " + value);
			
			if (key.equals("rescan"))
				rescan = (Util.parseDecimalIntOrZero(value) == 1);
			else if (key.equals("correlationid"))
				correlationid = Util.parseDecimalIntOrZero(value);
			else if (key.equals("actions")) // Apparently squeezer returns some commands which are included in the count of the current request
				actionsCount++;
			if (countIdSet.contains(key))
				counts.put(key, Util.parseDecimalIntOrZero(value));
			else {
				if (itemDelimeterMap.get(key) != null) {
					if (record != null) {
						parserInfo.handler.add(record);
						if (service.debugLogging)
							Log.v(TAG, "record=" + record);
					}
					parserInfo = itemDelimeterMap.get(key);
					record = new HashMap<String, String>();
				}
				if (record != null)
					record.put(key, value);
				else
					if (cmd.taggedParameters.contains(key))
						taggedParameters.put(key, token);
			}
		}
		
		if (record != null) parserInfo.handler.add(record);

		processLists: if (checkCorrelation(cmd.cmd, correlationid)) {
			int end = start + itemsPerResponse;
			int max = 0;
			for (SqueezeParserInfo parser: cmd.parserInfos) {
				if (checkCorrelation(parser.handler.getDataType(), correlationid)) {
					Integer count = counts.get(parser.count_id);
					int countValue = (count == null ? 0 : count);
					if (count != null || start == 0) {
						if (!parser.handler.processList(rescan, countValue - actionsCount, service.maxListSize, start))
							break processLists;
						if (countValue > max)
							max = (count < service.maxListSize || end > service.maxListSize ) ? count : service.maxListSize;
					}
				}
			}
			if (end < max) {
				int pageSize = (end + PAGESIZE > max ? pageSize = max - end : PAGESIZE);
				StringBuilder cmdline = new StringBuilder(cmd.cmd + " "	+ end + " " + pageSize);
				for (String parameter : taggedParameters.values())
					cmdline.append(" " + parameter);
				sendCommand(playerid + cmdline.toString());
			}
		}
	}


    private class YearListHandler implements SqueezerListHandler {
		List<SqueezerYear> years;


		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerYear.class;
		}
		
		public void clear() {
			years = new ArrayList<SqueezerYear>(){private static final long serialVersionUID = 1321113152942485275L;};
		}

		public void add(Map<String, String> record) {
			years.add(new SqueezerYear(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (service.yearListCallback.get() != null) {
				try {
					service.yearListCallback.get().onYearsReceived(count, max, start, years);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

    private class GenreListHandler implements SqueezerListHandler {
		List<SqueezerGenre> genres;

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerGenre.class;
		}
		
		public void clear() {
			genres = new ArrayList<SqueezerGenre>(){private static final long serialVersionUID = 581979365656327794L;};
		}

		public void add(Map<String, String> record) {
			genres.add(new SqueezerGenre(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (service.genreListCallback.get() != null) {
				try {
					service.genreListCallback.get().onGenresReceived(count, max, start, genres);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}

	}

	private class ArtistListHandler implements SqueezerListHandler {
		List<SqueezerArtist> artists;

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerArtist.class;
		}
		
		public void clear() {
			artists = new ArrayList<SqueezerArtist>(){private static final long serialVersionUID = 3995870292581536540L;};
			
		}

		public void add(Map<String, String> record) {
			artists.add(new SqueezerArtist(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (service.artistListCallback.get() != null) {
				try {
					service.artistListCallback.get().onArtistsReceived(count, max, start, artists);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

	private class AlbumListHandler implements SqueezerListHandler {
		List<SqueezerAlbum> albums;

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerAlbum.class;
		}
		
		public void clear() {
			albums = new ArrayList<SqueezerAlbum>(){private static final long serialVersionUID = 3702842875796811666L;};			
		}

		public void add(Map<String, String> record) {
			albums.add(new SqueezerAlbum(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (service.albumListCallback.get() != null) {
				try {
					service.albumListCallback.get().onAlbumsReceived(count, max, start, albums);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

	private class SongListHandler implements SqueezerListHandler {
		List<SqueezerSong> songs;

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerSong.class;
		}
		
		public void clear() {
			songs = new ArrayList<SqueezerSong>(){private static final long serialVersionUID = -6269354970999944842L;};			
		}

		public void add(Map<String, String> record) {
			songs.add(new SqueezerSong(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (service.songListCallback.get() != null) {
				try {
					service.songListCallback.get().onSongsReceived(count, max, start, songs);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}
	}

    private class PlaylistsHandler implements SqueezerListHandler {
		List<SqueezerPlaylist> playlists;

		public Class<? extends SqueezerItem> getDataType() {
			return SqueezerPlaylist.class;
		}
		
		public void clear() {
			playlists = new ArrayList<SqueezerPlaylist>(){ private static final long serialVersionUID = 3636348382591470060L; };			
		}

		public void add(Map<String, String> record) {
			playlists.add(new SqueezerPlaylist(record));
		}

		public boolean processList(boolean rescan, int count, int max, int start) {
			if (service.playlistsCallback.get() != null) {
				try {
					service.playlistsCallback.get().onPlaylistsReceived(count, max, start, playlists);
					return true;
				} catch (RemoteException e) {
					Log.e(TAG, e.toString());
				}
			}
			return false;
		}

	}
    
}
