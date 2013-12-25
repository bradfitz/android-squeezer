/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

import com.google.common.base.Joiner;

import org.acra.ACRA;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceAlbumListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceArtistListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceGenreListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceMusicFolderListCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlayerListCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlaylistsCallback;
import uk.org.ngo.squeezer.itemlist.IServicePluginItemListCallback;
import uk.org.ngo.squeezer.itemlist.IServicePluginListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceSongListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceYearListCallback;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;

class CliClient {

    private static final String TAG = "CliClient";

    /**
     * Join multiple strings (skipping nulls) together with newlines.
     */
    private static final Joiner mNewlineJoiner = Joiner.on("\n").skipNulls();

    class ExtendedQueryFormatCmd {

        final boolean playerSpecific;

        final boolean prefixed;

        final String cmd;

        final private Set<String> taggedParameters;

        final private SqueezeParserInfo[] parserInfos;

        private ExtendedQueryFormatCmd(boolean playerSpecific, boolean prefixed, String cmd,
                Set<String> taggedParameters, SqueezeParserInfo... parserInfos) {
            this.playerSpecific = playerSpecific;
            this.prefixed = prefixed;
            this.cmd = cmd;
            this.taggedParameters = taggedParameters;
            this.parserInfos = parserInfos;
        }

        private ExtendedQueryFormatCmd(boolean playerSpecific, String cmd,
                Set<String> taggedParameters, SqueezeParserInfo... parserInfos) {
            this(playerSpecific, false, cmd, taggedParameters, parserInfos);
        }

        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                String itemDelimiter, ListHandler<? extends Item> handler) {
            this(false, cmd, taggedParameters, new SqueezeParserInfo(itemDelimiter, handler));
        }

        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                ListHandler<? extends Item> handler) {
            this(false, cmd, taggedParameters, new SqueezeParserInfo(handler));
        }

    }

    final ExtendedQueryFormatCmd[] extQueryFormatCmds = initializeExtQueryFormatCmds();

    final Map<String, ExtendedQueryFormatCmd> extQueryFormatCmdMap
            = initializeExtQueryFormatCmdMap();

    private ExtendedQueryFormatCmd[] initializeExtQueryFormatCmds() {
        List<ExtendedQueryFormatCmd> list = new ArrayList<ExtendedQueryFormatCmd>();

        list.add(
                new ExtendedQueryFormatCmd(
                        "players",
                        new HashSet<String>(Arrays.asList("playerprefs", "charset")),
                        "playerid",
                        new BaseListHandler<Player>() {
                            Player defaultPlayer;

                            String lastConnectedPlayer;

                            @Override
                            public void clear() {
                                final SharedPreferences preferences = service
                                        .getSharedPreferences(Preferences.NAME,
                                                Context.MODE_PRIVATE);
                                lastConnectedPlayer = preferences
                                        .getString(Preferences.KEY_LASTPLAYER, null);
                                defaultPlayer = null;
                                Log.v(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);
                                super.clear();
                            }

                            @Override
                            public void add(Map<String, String> record) {
                                Player player = new Player(record);
                                // Discover the last connected player (if any, otherwise just pick the first one)
                                if (defaultPlayer == null || player.getId()
                                        .equals(lastConnectedPlayer)) {
                                    defaultPlayer = player;
                                }
                                items.add(player);
                            }

                            @Override
                            public boolean processList(boolean rescan, int count, int start,
                                    Map<String, String> parameters) {
                                IServicePlayerListCallback callback = service.playerListCallback
                                        .get();
                                if (callback != null) {
                                    // If the player list activity is active, pass the discovered players to it
                                    try {
                                        callback.onPlayersReceived(count,
                                                start, items);
                                    } catch (RemoteException e) {
                                        Log.e(TAG, e.toString());
                                        return false;
                                    }
                                } else if (start + items.size() >= count) {
                                    // Otherwise set the last connected player as the active player
                                    if (defaultPlayer != null) {
                                        service.changeActivePlayer(defaultPlayer);
                                    }
                                }
                                return true;
                            }
                        }
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "artists",
                        new HashSet<String>(
                                Arrays.asList("search", "genre_id", "album_id", "tags", "charset")),
                        new ArtistListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "albums",
                        new HashSet<String>(
                                Arrays.asList("search", "genre_id", "artist_id", "track_id", "year",
                                        "compilation", "sort", "tags", "charset")),
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
                        new HashSet<String>(
                                Arrays.asList("search", "artist_id", "album_id", "track_id", "year",
                                        "tags", "charset")),
                        new GenreListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "musicfolder",
                        new HashSet<String>(Arrays.asList("folder_id", "url", "tags", "charset")),
                        new MusicFolderListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "songs",
                        new HashSet<String>(
                                Arrays.asList("genre_id", "artist_id", "album_id", "year", "search",
                                        "tags", "sort", "charset")),
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
                        new SqueezeParserInfo("contributors_count", "contributor_id",
                                new ArtistListHandler()),
                        new SqueezeParserInfo("tracks_count", "track_id", new SongListHandler())
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        true,
                        "status",
                        new HashSet<String>(Arrays.asList("tags", "charset", "subscribe")),
                        new SqueezeParserInfo("playlist_tracks", "playlist index",
                                new SongListHandler())
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "radios",
                        new HashSet<String>(Arrays.asList("sort", "charset")),
                        "icon",
                        new PluginListHandler())
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "apps",
                        new HashSet<String>(Arrays.asList("sort", "charset")),
                        "icon",
                        new PluginListHandler())
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        true, true,
                        "items",
                        new HashSet<String>(
                                Arrays.asList("item_id", "search", "want_url", "charset")),
                        new SqueezeParserInfo(new PluginItemListHandler()))
        );

        return list.toArray(new ExtendedQueryFormatCmd[]{});
    }

    private Map<String, ExtendedQueryFormatCmd> initializeExtQueryFormatCmdMap() {
        Map<String, ExtendedQueryFormatCmd> map = new HashMap<String, ExtendedQueryFormatCmd>();
        for (ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            map.put(cmd.cmd, cmd);
        }
        return map;
    }

    private final SqueezeService service;

    private int pageSize;

    CliClient(SqueezeService service) {
        this.service = service;
    }

    void initialize() {
        pageSize = service.getResources().getInteger(R.integer.PageSize);
    }


    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private int _correlationid = 0;


    /**
     * Send the supplied commands to the SqueezeboxServer.
     * <p/>
     * <b>All</b> data to the server goes through this method
     * <p/>
     * <b>Note</b> don't call this from the main (UI) thread. If you are unsure if you are on the
     * main thread, then use {@link #sendCommand(String...)} instead.
     *
     * @param commands List of commands to send
     */
    synchronized void sendCommandImmediately(String... commands) {
        if (commands.length == 0) {
            return;
        }
        PrintWriter writer = service.connectionState.getSocketWriter();
        if (writer == null) {
            return;
        }

        String formattedCommands = mNewlineJoiner.join(commands);
        Log.v(TAG, "SENDING: " + formattedCommands);
        ACRA.getErrorReporter().putCustomData("lastCommands", formattedCommands);
        writer.println(formattedCommands);
        writer.flush();
    }

    /**
     * Send the supplied commands to the SqueezeboxServer.
     * <p/>
     * This method takes care to avoid performing network operations on the main thread. Use {@link
     * #sendCommandImmediately(String...)} if you are sure you are not on the main thread (eg if
     * called from the listening thread).
     *
     * @param commands List of commands to send
     */
    void sendCommand(final String... commands) {
        if (service.mainThread != Thread.currentThread()) {
            sendCommandImmediately(commands);
        } else {
            service.executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendCommandImmediately(commands);
                }
            });
        }
    }

    /**
     * Send the specified command for the active player to the SqueezeboxServer
     *
     * @param command The command to send
     */
    void sendPlayerCommand(final String command) {
        if (service.connectionState.getActivePlayer() == null) {
            return;
        }
        sendCommand(Util.encode(service.connectionState.getActivePlayer().getId()) + " " + command);
    }

    /**
     * Check whether this response is obsolete, i.e. if the correlation id for the current response
     * is lower than the last registered minimum.
     *
     * @param registeredCorrelationId Minimum correlation id
     * @param currentCorrelationId The correlation id of the current response
     *
     * @return True if the response is valid
     */
    boolean checkCorrelation(Integer registeredCorrelationId, int currentCorrelationId) {
        if (registeredCorrelationId == null) {
            return true;
        }
        return (currentCorrelationId >= registeredCorrelationId);
    }


    // We register when asynchronous fetches are initiated, and when callbacks are unregistered
    // because these events will make responses from pending requests obsolete
    private final Map<String, Integer> cmdCorrelationIds = new HashMap<String, Integer>();

    private final Map<Class<?>, Integer> typeCorrelationIds = new HashMap<Class<?>, Integer>();

    /**
     * Call this when a callback for received SqueezeboxServer items are unregistered. <p>Responses
     * which come in after this time, i.e. which have a correlation id lower than the current, are
     * ignored.
     *
     * @param clazz Type of the callback which are unregistered
     */
    void cancelRequests(Class<?> clazz) {
        typeCorrelationIds.put(clazz, _correlationid);
    }

    /**
     * Check whether this response is obsolete.
     *
     * @param cmd Type of the request
     * @param correlationid The correlation id of the current response
     *
     * @return True if the response is valid
     */
    private boolean checkCorrelation(String cmd, int correlationid) {
        return checkCorrelation(cmdCorrelationIds.get(cmd), correlationid);
    }

    /**
     * Check whether this response is obsolete.
     *
     * @param type Type of the callback to receive the items
     * @param correlationid The correlation id of the current response
     *
     * @return True if the response is valid
     */
    private boolean checkCorrelation(Class<? extends Item> type, int correlationid) {
        return checkCorrelation(typeCorrelationIds.get(type), correlationid);
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p/>
     * If start is zero, it means the the list is being reordered, which will make any pending
     * replies, for the given items, obsolete.
     * <p/>
     * This will always order one item, to learn the number of items from the server. Remaining
     * items will then be automatically ordered when the response arrives. See {@link
     * #parseSqueezerList(boolean, String, String, List, ListHandler)} for details.
     *
     * @param playerid Id of the current player or null
     * @param cmd Identifies the
     * @param start
     * @param parameters
     */
    private void requestItems(String playerid, String cmd, int start, List<String> parameters) {
        if (start == 0) {
            cmdCorrelationIds.put(cmd, _correlationid);
        }
        final StringBuilder sb = new StringBuilder(
                cmd + " " + start + " " + (start == 0 ? 1 : pageSize));
        if (playerid != null) {
            sb.insert(0, Util.encode(playerid) + " ");
        }
        if (parameters != null) {
            for (String parameter : parameters) {
                sb.append(" " + Util.encode(parameter));
            }
        }
        sendCommand(sb.toString() + " correlationid:" + _correlationid++);
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

    void requestPlayerItems(String cmd, int start) {
        requestPlayerItems(cmd, start, null);
    }

    /**
     * Data for {@link CliClient#parseSqueezerList(boolean, List, SqueezeParserInfo...)}
     *
     * @author kaa
     */
    private static class SqueezeParserInfo {

        private final String item_delimiter;

        private final String count_id;

        private final ListHandler<? extends Item> handler;

        /**
         * @param countId The label for the tag which contains the total number of results, normally
         * "count".
         * @param itemDelimiter As defined for each extended query format command in the
         * squeezeserver CLI documentation.
         * @param handler Callback to receive the parsed data.
         */
        public SqueezeParserInfo(String countId, String itemDelimiter,
                ListHandler<? extends Item> handler) {
            count_id = countId;
            item_delimiter = itemDelimiter;
            this.handler = handler;
        }

        public SqueezeParserInfo(String itemDelimiter,
                ListHandler<? extends Item> handler) {
            this("count", itemDelimiter, handler);
        }

        public SqueezeParserInfo(ListHandler<? extends Item> handler) {
            this("id", handler);
        }
    }

    /**
     * Generic method to parse replies for queries in extended query format
     * <p/>
     * This is the control center for asynchronous and paging receiving of data from SqueezeServer.
     * <p/>
     * Transfer of each data type are started by an asynchronous request by one of the public method
     * in this module. This method will forward the data using the supplied {@link ListHandler}, and
     * and order the next page if necessary, repeating the current query parameters.
     * <p/>
     * Activities should just initiate the request, and supply a callback to receive a page of
     * data.
     *
     * @param playercmd Set this for replies for the current player, to skip the playerid
     * @param tokens List of tokens with value or key:value.
     * @param parserInfos Data for each list you expect for the current repsonse
     */
    void parseSqueezerList(ExtendedQueryFormatCmd cmd, List<String> tokens) {
        Log.v(TAG, "Parsing list: " + tokens);

        int ofs = cmd.cmd.split(" ").length + (cmd.playerSpecific ? 1 : 0) + (cmd.prefixed ? 1 : 0);
        int actionsCount = 0;
        String playerid = (cmd.playerSpecific ? tokens.get(0) + " " : "");
        String prefix = (cmd.prefixed ? tokens.get(cmd.playerSpecific ? 1 : 0) + " " : "");
        int start = Util.parseDecimalIntOrZero(tokens.get(ofs));
        int itemsPerResponse = Util.parseDecimalIntOrZero(tokens.get(ofs + 1));

        int correlationid = 0;
        boolean rescan = false;
        Map<String, String> taggedParameters = new HashMap<String, String>();
        Map<String, String> parameters = new HashMap<String, String>();
        Set<String> countIdSet = new HashSet<String>();
        Map<String, SqueezeParserInfo> itemDelimeterMap = new HashMap<String, SqueezeParserInfo>();
        Map<String, Integer> counts = new HashMap<String, Integer>();
        Map<String, String> record = null;

        for (SqueezeParserInfo parserInfo : cmd.parserInfos) {
            parserInfo.handler.clear();
            countIdSet.add(parserInfo.count_id);
            itemDelimeterMap.put(parserInfo.item_delimiter, parserInfo);
        }

        SqueezeParserInfo parserInfo = null;
        for (int idx = ofs + 2; idx < tokens.size(); idx++) {
            String token = tokens.get(idx);
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
                Log.e(TAG, "Expected colon in list token. '" + token + "'");
                return;
            }
            String key = Util.decode(token.substring(0, colonPos));
            String value = Util.decode(token.substring(colonPos + 3));
            Log.v(TAG, "key=" + key + ", value: " + value);

            if (key.equals("rescan")) {
                rescan = (Util.parseDecimalIntOrZero(value) == 1);
            } else if (key.equals("correlationid")) {
                correlationid = Util.parseDecimalIntOrZero(value);
                taggedParameters.put(key, token);
            } else if (key.equals("actions")) {
                // Apparently squeezer returns some commands which are
                // included in the count of the current request
                actionsCount++;
            }
            if (countIdSet.contains(key)) {
                counts.put(key, Util.parseDecimalIntOrZero(value));
            } else {
                if (itemDelimeterMap.get(key) != null) {
                    if (record != null) {
                        parserInfo.handler.add(record);
                        Log.v(TAG, "record=" + record);
                    }
                    parserInfo = itemDelimeterMap.get(key);
                    record = new HashMap<String, String>();
                }
                if (record != null) {
                    record.put(key, value);
                } else if (cmd.taggedParameters.contains(key)) {
                    taggedParameters.put(key, token);
                } else {
                    parameters.put(key, value);
                }
            }
        }

        if (record != null) {
            parserInfo.handler.add(record);
            Log.v(TAG, "record=" + record);
        }

        processLists:
        if (checkCorrelation(cmd.cmd, correlationid)) {
            int end = start + itemsPerResponse;
            int max = 0;
            for (SqueezeParserInfo parser : cmd.parserInfos) {
                if (checkCorrelation(parser.handler.getDataType(), correlationid)) {
                    Integer count = counts.get(parser.count_id);
                    int countValue = (count == null ? 0 : count);
                    if (count != null || start == 0) {
                        if (!parser.handler
                                .processList(rescan, countValue - actionsCount, start,
                                        parameters)) {
                            break processLists;
                        }
                        if (countValue > max) {
                            max = countValue;
                        }
                    }
                }
            }
            if (end % pageSize != 0 && end < max) {
                int count = (end + pageSize > max ? max - end : pageSize - itemsPerResponse);
                StringBuilder cmdline = new StringBuilder(cmd.cmd + " " + end + " " + count);
                for (String parameter : taggedParameters.values()) {
                    cmdline.append(" " + parameter);
                }
                sendCommandImmediately(playerid + prefix + cmdline.toString());
            }
        }
    }

    private class YearListHandler extends BaseListHandler<Year> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServiceYearListCallback callback = service.yearListCallback.get();
            if (callback != null) {
                try {
                    callback.onYearsReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class GenreListHandler extends BaseListHandler<Genre> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServiceGenreListCallback callback = service.genreListCallback.get();
            if (callback != null) {
                try {
                    callback.onGenresReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class ArtistListHandler extends BaseListHandler<Artist> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServiceArtistListCallback callback = service.artistListCallback.get();
            if (callback != null) {
                try {
                    callback.onArtistsReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class AlbumListHandler extends BaseListHandler<Album> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServiceAlbumListCallback callback = service.albumListCallback.get();
            if (callback != null) {
                try {
                    callback.onAlbumsReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class MusicFolderListHandler extends BaseListHandler<MusicFolderItem> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServiceMusicFolderListCallback callback = service.musicFolderListCallback.get();
            if (callback != null) {
                try {
                    callback.onMusicFoldersReceived(count, start,
                            items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }

            return false;
        }
    }

    private class SongListHandler extends BaseListHandler<Song> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServiceSongListCallback callback = service.songListCallback.get();
            if (callback != null) {
                try {
                    // TODO use parameters to update the current player state
                    // service.playerState.update(parameters);
                    callback.onSongsReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class PlaylistsHandler extends BaseListHandler<Playlist> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServicePlaylistsCallback callback = service.playlistsCallback.get();
            if (callback != null) {
                try {
                    callback.onPlaylistsReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class PluginListHandler extends BaseListHandler<Plugin> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServicePluginListCallback callback = service.pluginListCallback.get();
            if (callback != null) {
                try {
                    callback.onPluginsReceived(count, start, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

    private class PluginItemListHandler extends BaseListHandler<PluginItem> {

        @Override
        public boolean processList(boolean rescan, int count, int start,
                Map<String, String> parameters) {
            IServicePluginItemListCallback callback = service.pluginItemListCallback.get();
            if (callback != null) {
                try {
                    callback.onPluginItemsReceived(count, start,
                            parameters, items);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
            return false;
        }
    }

}
