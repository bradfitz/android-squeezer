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

import android.net.Uri;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Joiner;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayerPrefReceived;
import uk.org.ngo.squeezer.service.event.PlayerVolume;
import uk.org.ngo.squeezer.service.event.PlayersChanged;
import uk.org.ngo.squeezer.service.event.PlaylistCreateFailed;
import uk.org.ngo.squeezer.service.event.PlaylistRenameFailed;
import uk.org.ngo.squeezer.service.event.PlaylistTracksAdded;
import uk.org.ngo.squeezer.service.event.PlaylistTracksDeleted;

class CliClient extends BaseClient {

    private static final String TAG = "CliClient";


    /** {@link java.util.regex.Pattern} that splits strings on spaces. */
    private static final Pattern mSpaceSplitPattern = Pattern.compile(" ");

    /**
     * Join multiple strings (skipping nulls) together with newlines.
     */
    private static final Joiner mNewlineJoiner = Joiner.on("\n").skipNulls();

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    private final Map<String, Player> mPlayers = new HashMap<>();


    /** The types of command handler. */
    @IntDef(flag=true, value={
            HANDLER_LIST_GLOBAL, HANDLER_LIST_PREFIXED, HANDLER_LIST_PLAYER_SPECIFIC,
            HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC, HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface HandlerListType {}
    public static final int HANDLER_LIST_GLOBAL = 1;
    public static final int HANDLER_LIST_PREFIXED = 1 << 1;
    public static final int HANDLER_LIST_PLAYER_SPECIFIC = 1 << 2;
    public static final int HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC = 1 << 3;
    public static final int HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC = 1 << 4;

    /**
     * Represents a command that can be sent to the server using the extended query format.
     * <p>
     * Extended queries have the following structure:
     * <p>
     * <code>[&lt;playerid>] &lt;command> &lt;start> &lt;itemsPerResponse> &lt;tagged-params> ...</code>
     * <ul>
     *     <li><code>&lt;playerid></code> - unique player identifier</li>
     *     <li><code>&lt;command></code> - command to send</li>
     *     <li><code>&lt;start></code> - 0-based index of the first item to return</li>
     *     <li><code>&lt;itemsPerResponse></code> - number of items to return per chunk</li>
     *     <li><code>&lt;tagged-params></code> - one or more <code>tag:value</code> pairs</li>
     * </ul>
     */
    static class ExtendedQueryFormatCmd {
        private static final int PLAYER_SPECIFIC_HANDLER_LISTS =
                HANDLER_LIST_PLAYER_SPECIFIC | HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC | HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC;

        private static final int PREFIXED_HANDLER_LISTS =
                HANDLER_LIST_PREFIXED | HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC;

        @HandlerListType
        final int handlerList;

        /** True if this is a player-specific command (i.e., the command should send a player ID). */
        final private boolean playerSpecific;


        final private boolean prefixed;

        final String cmd;

        final private Set<String> taggedParameters;

        final private SqueezeParserInfo[] parserInfos;

        /**
         * A command sent to the server.
         *
         * @param handlerList The command's type.
         * @param cmd The command to send.
         * @param taggedParameters Tagged parameters to send
         * @param parserInfos ?
         */
        public ExtendedQueryFormatCmd(@HandlerListType int handlerList, String cmd,
                                      Set<String> taggedParameters, SqueezeParserInfo... parserInfos) {
            this.handlerList = handlerList;
            playerSpecific = (PLAYER_SPECIFIC_HANDLER_LISTS & handlerList) != 0;
            prefixed = (PREFIXED_HANDLER_LISTS & handlerList) != 0;
            this.cmd = cmd;
            this.taggedParameters = taggedParameters;
            this.parserInfos = parserInfos;
        }

        /**
         * A global command to the server where items in the response have a delimiter other than "id:".
         *
         * @param cmd The command to send to the server.
         * @param taggedParameters The keys for any tagged parameters to send.
         * @param handler The handler used to construct new model objects from the response.
         */
        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                                      ListHandler<? extends Item> handler, String... columns) {
            this(HANDLER_LIST_GLOBAL, cmd, taggedParameters, new SqueezeParserInfo(handler, columns));
        }

        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                                      String itemDelimiter, ListHandler<? extends Item> handler) {
            this(HANDLER_LIST_GLOBAL, cmd, taggedParameters, new SqueezeParserInfo(itemDelimiter, handler));
        }

        /**
         * A command to the server where items in the response are delimited by id: tags.
         *
         * @param cmd The command to send to the server.
         * @param taggedParameters The keys for any tagged parameters to send.
         * @param handler The handler used to construct new model objects from the response.
         */
        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                ListHandler<? extends Item> handler) {
            this(HANDLER_LIST_GLOBAL, cmd, taggedParameters, new SqueezeParserInfo(handler));
        }

        public String toString() {
            return "{ cmd:'" + cmd + "', list:" + handlerList + ", player specific:" + playerSpecific + ", prefixed:" + prefixed + " }";
        }

    }

    final ExtendedQueryFormatCmd[] extQueryFormatCmds = initializeExtQueryFormatCmds();

    final Map<String, ExtendedQueryFormatCmd> extQueryFormatCmdMap
            = initializeExtQueryFormatCmdMap();

    private ExtendedQueryFormatCmd[] initializeExtQueryFormatCmds() {
        List<ExtendedQueryFormatCmd> list = new ArrayList<>();

        list.add(
                new ExtendedQueryFormatCmd(
                        "players",
                        new HashSet<>(Arrays.asList("playerprefs", "charset")),
                        "playerindex",
                        new BaseListHandler<Player>() {}
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC,
                        "alarms",
                        new HashSet<>(Arrays.asList("filter", "dow")),
                        new SqueezeParserInfo(new BaseListHandler<Alarm>(){})
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "artists",
                        new HashSet<>(
                                Arrays.asList("search", "genre_id", "album_id", "tags", "charset")),
                        new ArtistListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "albums",
                        new HashSet<>(
                                Arrays.asList("search", "genre_id", "artist_id", "track_id", "year",
                                        "compilation", "sort", "tags", "charset")),
                        new AlbumListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "years",
                        new HashSet<>(Arrays.asList("charset")),
                        "year",
                        new BaseListHandler<Year>(){}
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "genres",
                        new HashSet<>(
                                Arrays.asList("search", "artist_id", "album_id", "track_id", "year",
                                        "tags", "charset")),
                        new GenreListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "musicfolder",
                        new HashSet<>(Arrays.asList("folder_id", "url", "tags", "charset")),
                        new MusicFolderListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "songs",
                        new HashSet<>(
                                Arrays.asList("genre_id", "artist_id", "album_id", "year", "search",
                                        "tags", "sort", "charset")),
                        new SongListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "playlists",
                        new HashSet<>(Arrays.asList("search", "tags", "charset")),
                        new BaseListHandler<Playlist>(){})
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "playlists tracks",
                        new HashSet<>(Arrays.asList("playlist_id", "tags", "charset")),
                        "playlist index",
                        new SongListHandler())
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "alarm playlists",
                        new HashSet<String>(),
                        "category",
                        new BaseListHandler<AlarmPlaylist>(){})
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_GLOBAL,
                        "search",
                        new HashSet<>(Arrays.asList("term", "charset")),
                        new SqueezeParserInfo("genres_count", new GenreListHandler(), "genre_id"),
                        new SqueezeParserInfo("albums_count", new AlbumListHandler(), "album_id"),
                        new SqueezeParserInfo("contributors_count", new ArtistListHandler()
                                , "contributor_id"),
                        new SqueezeParserInfo("tracks_count", new SongListHandler(), "track_id")
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_PLAYER_SPECIFIC,
                        "status",
                        new HashSet<>(Arrays.asList("tags", "charset", "subscribe")),
                        new SqueezeParserInfo("playlist_tracks", new SongListHandler(),
                                "playlist index")
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "radios",
                        new HashSet<>(Arrays.asList("sort", "charset")),
                        new PluginListHandler(),
                        "cmd", "name", "type", "icon", "weight"
                )

        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "apps",
                        new HashSet<>(Arrays.asList("sort", "charset")),
                        new PluginListHandler(),
                        "cmd", "name", "type", "icon", "weight"
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC,
                        "items",
                        new HashSet<>(
                                Arrays.asList("item_id", "search", "want_url", "charset")),
                        new SqueezeParserInfo(new PluginItemListHandler()))
        );

        return list.toArray(new ExtendedQueryFormatCmd[list.size()]);
    }

    private Map<String, ExtendedQueryFormatCmd> initializeExtQueryFormatCmdMap() {
        Map<String, ExtendedQueryFormatCmd> map = new HashMap<>();
        for (ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            map.put(cmd.cmd, cmd);
        }
        return map;
    }

    CliClient(@NonNull EventBus eventBus) {
        super(eventBus);
    }

    // Call through to mConnectionState implementation for the moment.
    public void disconnect(boolean loginFailed) {
        currentConnectionGeneration.incrementAndGet();
        mConnectionState.disconnect(loginFailed);
        Socket socket = socketRef.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        socketRef.set(null);
        socketWriter.set(null);
        httpPort.set(null);
        mPlayers.clear();
    }

    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private int mCorrelationId = 0;

    @Override
    public synchronized void sendCommandImmediately(String... commands) {
        if (commands.length == 0) {
            return;
        }
        PrintWriter writer = socketWriter.get();
        if (writer == null) {
            return;
        }

        String formattedCommands = mNewlineJoiner.join(commands);
        Log.v(TAG, "SEND: " + formattedCommands);

        // Make sure that username/password do not make it to Crashlytics.
        if (commands[0].startsWith("login ")) {
            Util.crashlyticsSetString("lastCommands", "login [username] [password]");
        } else {
            Util.crashlyticsSetString("lastCommands", formattedCommands);
        }

        writer.println(formattedCommands);
        writer.flush();
    }

    @Override
    public void sendCommand(final String command) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            sendCommandImmediately(command);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sendCommandImmediately(command);
                }
            });
        }
    }

    public void sendPlayerCommand(final Player player, final String command) {
        sendCommand(Util.encode(player.getId()) + " " + command);
    }

    /**
     * Keeps track of asynchronous request waiting for a reply
     * <p>
     * When a request is made, the callback is put this list, along with a
     * unique correlation id.
     * <p>
     * When the reply comes the callback is called, and the request is removed from this list.
     * <p>
     * When the client hosting callbacks goes away, all requests with callbacks hosted by it, is
     * removed from this list.
     * <p>
     * If a reply with with matching entry is this list comes in, it is discarded.
     */
    private final Map<Integer, IServiceItemListCallback> mPendingRequests
            = new ConcurrentHashMap<>();

    public void cancelClientRequests(Object client) {
        for (Map.Entry<Integer, IServiceItemListCallback> entry : mPendingRequests.entrySet()) {
            if (entry.getValue().getClient() == client) {
                Log.i(TAG, "cancel request: [" + entry.getKey() + ";" + entry.getValue() +"]");
                mPendingRequests.remove(entry.getKey());
            }
        }
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p>
     * Items are returned to the caller via the specified callback.
     * <p>
     * See {@link #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)} for details.
     *
     * @param playerId Id of the current player or null
     * @param cmd Identifies the type of items
     * @param start First item to return
     * @param pageSize No of items to return
     * @param parameters Item specific parameters for the request
     * @see #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)
     */
    private void internalRequestItems(String playerId, String cmd, int start, int pageSize, List<String> parameters, IServiceItemListCallback callback) {
        mPendingRequests.put(mCorrelationId, callback);
        final StringBuilder sb = new StringBuilder(cmd + " " + start + " " + pageSize);
        if (playerId != null) {
            sb.insert(0, Util.encode(playerId) + " ");
        }
        if (parameters != null) {
            for (String parameter : parameters) {
                sb.append(" ").append(Util.encode(parameter));
            }
        }
        sb.append(" correlationid:");
        sb.append(mCorrelationId++);
        sendCommand(sb.toString());
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p>
     * Items are requested in chunks of <code>R.integer.PageSize</code>, and returned
     * to the caller via the specified callback.
     * <p>
     * If start is zero, this will order one item, to quickly learn the number of items
     * from the server. When the server response with this item it is transferred to the
     * caller. The remaining items in the first page are then ordered, and transferred
     * to the caller when they arrive.
     * <p>
     * If start is < 0, it means the caller wants the entire list. They are ordered in
     * pages, and transferred to the caller as they arrive.
     * <p>
     * Otherwise request a page of items starting from start.
     * <p>
     * See {@link #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)} for details.
     *
     * @param playerId Id of the current player or null
     * @param cmd Identifies the type of items
     * @param start First item to return
     * @param parameters Item specific parameters for the request
     * @see #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)
     */
    private void internalRequestItems(String playerId, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        boolean full_list = (start < 0);

        if (full_list) {
            if (parameters == null)
                parameters = new ArrayList<>();
            parameters.add("full_list:1");
        }

        internalRequestItems(playerId, cmd, (full_list ? 0 : start), (start == 0 ? 1 : mPageSize), parameters, callback);
    }

    void requestItems(Player player, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        internalRequestItems(player.getId(), cmd, start, parameters, callback);
    }

    public void requestItems(String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        internalRequestItems(null, cmd, start, parameters, callback);
    }

    public void requestItems(String cmd, int start, IServiceItemListCallback callback) {
        requestItems(cmd, start, null, callback);
    }

    void requestItems(String cmd, int start, int pageSize, List<String> parameters, IServiceItemListCallback callback) {
        internalRequestItems(null, cmd, start, pageSize, parameters, callback);
    }

    public void requestItems(String cmd, int start, int pageSize, IServiceItemListCallback callback) {
        requestItems(cmd, start, pageSize, null, callback);
    }

    public void requestPlayerItems(@Nullable Player player, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        if (player == null) {
            return;
        }
        requestItems(player, cmd, start, parameters, callback);
    }

    /**
     * Data for {@link CliClient#parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)}
     *
     * @author kaa
     */
    private static class SqueezeParserInfo {

        private final Set<String> columns;

        private final String count_id;

        private final ListHandler<? extends Item> handler;

        /**
         * @param countId The label for the tag which contains the total number of results, normally
         * "count".
         * @param handler Callback to receive the parsed data.
         * @param columns If one column is specified, it is the item delimiter as defined for each
         *                extended query format command in the SqueezeServer CLI documentation.
         *                Multiple columns is supported to workaround of a bug in recent server
         *                versions.
         */
        public SqueezeParserInfo(String countId, ListHandler<? extends Item> handler, String... columns) {
            count_id = countId;
            this.columns = new HashSet<>(Arrays.asList(columns));
            this.handler = handler;
        }

        public SqueezeParserInfo(String itemDelimiter, ListHandler<? extends Item> handler) {
            this("count", handler, itemDelimiter);
        }

        public SqueezeParserInfo(ListHandler<? extends Item> handler, String... columns) {
            this("count", handler, columns);
        }

        public SqueezeParserInfo(ListHandler<? extends Item> handler) {
            this("id", handler);
        }

        public boolean isComplete(Map<String, String> record) {
            for (String column : columns) {
                if (!record.containsKey(column)) return false;
            }
            return true;
        }
    }

    /**
     * Generic method to parse replies for queries in extended query format
     * <p>
     * This is the control center for asynchronous and paging receiving of data from SqueezeServer.
     * <p>
     * Transfer of each data type are started by an asynchronous request by one of the public method
     * in this module. This method will forward the data using the supplied {@link ListHandler}, and
     * and order the next page if necessary, repeating the current query parameters.
     * <p>
     * Activities should just initiate the request, and supply a callback to receive a page of
     * data.
     *
     * @param cmd Describes of the CLI command
     * @param tokens List of tokens with value or key:value.
     */
    void parseSqueezerList(ExtendedQueryFormatCmd cmd, List<String> tokens) {
        Log.v(TAG, "Parsing list, cmd: " +cmd + ", tokens: " + tokens);

        final int ofs = mSpaceSplitPattern.split(cmd.cmd).length + (cmd.playerSpecific ? 1 : 0) + (cmd.prefixed ? 1 : 0);
        int actionsCount = 0;
        final String playerid = (cmd.playerSpecific ? tokens.get(0) + " " : "");
        final String prefix = (cmd.prefixed ? tokens.get(cmd.playerSpecific ? 1 : 0) + " " : "");
        final int start = Util.parseDecimalIntOrZero(tokens.get(ofs));
        final int itemsPerResponse = Util.parseDecimalIntOrZero(tokens.get(ofs + 1));

        int correlationId = 0;
        boolean rescan = false;
        boolean full_list = false;
        final Map<String, String> taggedParameters = new HashMap<>();
        final Map<String, String> parameters = new HashMap<>();
        final Set<String> countIdSet = new HashSet<>();
        final Map<String, SqueezeParserInfo> itemDelimeterMap = new HashMap<>();
        final Map<String, Integer> counts = new HashMap<>();
        final Map<String, String> record = new HashMap<>();

        for (SqueezeParserInfo parserInfo : cmd.parserInfos) {
            parserInfo.handler.clear();
            countIdSet.add(parserInfo.count_id);
            for (String column : parserInfo.columns) itemDelimeterMap.put(column, parserInfo);
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

            if ("rescan".equals(key)) {
                rescan = (Util.parseDecimalIntOrZero(value) == 1);
            } else if ("full_list".equals(key)) {
                full_list = (Util.parseDecimalIntOrZero(value) == 1);
                taggedParameters.put(key, token);
            } else if ("correlationid".equals(key)) {
                correlationId = Util.parseDecimalIntOrZero(value);
                taggedParameters.put(key, token);
            } else if ("actions".equals(key)) {
                // Apparently squeezer returns some commands which are
                // included in the count of the current request
                actionsCount++;
            }
            if (countIdSet.contains(key)) {
                counts.put(key, Util.parseDecimalIntOrZero(value));
            } else {
                SqueezeParserInfo newParserInfo = itemDelimeterMap.get(key);
                if (newParserInfo != null && parserInfo != null && parserInfo.isComplete(record)) {
                    parserInfo.handler.add(record);
                    Log.v(TAG, "record=" + record);
                    record.clear();
                }
                if (newParserInfo != null) parserInfo = newParserInfo;
                if (parserInfo != null) {
                    record.put(key, value);
                } else if (cmd.taggedParameters.contains(key)) {
                    taggedParameters.put(key, token);
                } else {
                    parameters.put(key, value);
                }
            }
        }

        if (parserInfo != null && !record.isEmpty()) {
            parserInfo.handler.add(record);
            Log.v(TAG, "record=" + record);
        }

        // Process the lists for all the registered handlers
        int end = start + itemsPerResponse;
        int max = 0;
        IServiceItemListCallback callback = mPendingRequests.get(correlationId);
        for (SqueezeParserInfo parser : cmd.parserInfos) {
            Integer count = counts.get(parser.count_id);
            int countValue = (count == null ? 0 : count);
            if (count != null || start == 0) {
                if (callback != null) {
                    callback.onItemsReceived(countValue - actionsCount, start, parameters, parser.handler.getItems(), parser.handler.getDataType());
                }
                if (countValue > max) {
                    max = countValue;
                }
            }
        }

        // If the client is still around check if we need to order more items,
        // otherwise were done, so remove the callback
        if (callback != null) {
            if ((full_list || end % mPageSize != 0) && end < max) {
                int count = (end + mPageSize > max ? max - end : full_list ? mPageSize : mPageSize - itemsPerResponse);
                StringBuilder cmdline = new StringBuilder();
                cmdline.append(playerid);
                cmdline.append(prefix);
                cmdline.append(cmd.cmd);
                cmdline.append(" ");
                cmdline.append(end);
                cmdline.append(" ");
                cmdline.append(count);
                for (String parameter : taggedParameters.values()) {
                    cmdline.append(" ").append(parameter);
                }
                sendCommandImmediately(cmdline.toString());
            } else
                mPendingRequests.remove(correlationId);
        }
    }

    private class GenreListHandler extends BaseListHandler<Genre> {}

    private class ArtistListHandler extends BaseListHandler<Artist> {}

    /**
     * Handler that adds <code>artwork_url</code> tags to items.
     */
    private class AlbumListHandler extends BaseListHandler<Album> {
        @Override
        public void add(Map<String, String> record) {
            addArtworkUrlTag(record);
            super.add(record);
        }
    }

    /**
     * Handler that adds <code>download_url</code> tags to items.
     */
    private class MusicFolderListHandler extends BaseListHandler<MusicFolderItem> {
        @Override
        public void add(Map<String, String> record) {
            addDownloadUrlTag(record);
            super.add(record);
        }
    }

    /**
     * Handler that adds <code>artwork_url</code> and <code>download_url</code> tags to items.
     */
    private class SongListHandler extends BaseListHandler<Song> {
        @Override
        public void add(Map<String, String> record) {
            addArtworkUrlTag(record);
            addDownloadUrlTag(record);
            super.add(record);
        }
    }

    private class PluginListHandler extends BaseListHandler<Plugin> {
        @Override
        public void add(Map<String, String> record) {
            fixImageTag("icon", record);
            super.add(record);
        }
    }

    private class PluginItemListHandler extends BaseListHandler<PluginItem> {
        @Override
        public void add(Map<String, String> record) {
            fixImageTag("image", record);
            super.add(record);
        }
    }

    /**
     * Make sure the icon/image tag is an absolute URL.
     *
     * @param record The record to modify.
     */
    private void fixImageTag(String imageTag, Map<String, String> record) {
        String image = record.get(imageTag);
        if (image == null) {
            return;
        }

        if (Uri.parse(image).isAbsolute()) {
            return;
        }

        record.put(imageTag, mUrlPrefix + (image.startsWith("/") ? image : "/" + image));
    }

    // Shims around ConnectionState methods.
    @Override
    public void startConnect(final SqueezeService service,
                             final String host, final int cliPort, int httpPort,
                             final String userName, final String password) {
        Log.i(TAG, "Connecting to: " + userName + "@" + host + ":" + cliPort + "," + httpPort);
        final String cleanHostPort = host + ":" + cliPort;

        currentHost.set(host);
        this.cliPort.set(cliPort);
        this.httpPort.set(null);  // not known until later, after connect.
        this.userName.set(userName);
        this.password.set(password);

        // Start the off-thread connect.
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Ensuring service is disconnected");
                service.disconnect();
                Socket socket = new Socket();
                try {
                    Log.d(TAG, "Connecting to: " + cleanHostPort);
                    mConnectionState.setConnectionState(ConnectionState.CONNECTION_STARTED);
                    socket.connect(new InetSocketAddress(host, cliPort),
                            4000 /* ms timeout */);
                    socketRef.set(socket);
                    Log.d(TAG, "Connected to: " + cleanHostPort);
                    socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                    mConnectionState.setConnectionState(ConnectionState.CONNECTION_COMPLETED);
                    startListeningThread(mExecutor);
                    onCliPortConnectionEstablished(userName, password);
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(userName, password.toCharArray());
                        }
                    });
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket timeout connecting to: " + cleanHostPort);
                    mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
                } catch (IOException e) {
                    Log.e(TAG, "IOException connecting to: " + cleanHostPort);
                    mConnectionState.setConnectionState(ConnectionState.CONNECTION_FAILED);
                }
            }

        });
    }

    private interface CmdHandler {
        void handle(List<String> tokens);
    }

    private final Map<String, CmdHandler> globalHandlers = initializeGlobalHandlers();

    private final Map<String, CmdHandler> prefixedHandlers = initializePrefixedHandlers();

    /**
     * Command handlers that are specific to a given player. The first token passed to any
     * handler is always the player ID.
     */
    private final Map<String, CmdHandler> playerSpecificHandlers
            = initializePlayerSpecificHandlers();

    private final Map<String, CmdHandler> globalPlayerSpecificHandlers
            = initializeGlobalPlayerSpecificHandlers();

    private final Map<String, CmdHandler> prefixedPlayerSpecificHandlers
            = initializePrefixedPlayerSpecificHandlers();

    private Map<String, CmdHandler> initializeGlobalHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HANDLER_LIST_GLOBAL) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }
        handlers.put("playlists", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                if ("delete".equals(tokens.get(1))) {
                    ;
                } else if ("edit".equals(tokens.get(1))) {
                    ;
                } else if ("new".equals(tokens.get(1))) {
                    HashMap<String, String> tokenMap = parseTokens(tokens);
                    if (tokenMap.get("overwritten_playlist_id") != null) {
                        mEventBus.post(new PlaylistCreateFailed(Squeezer.getContext().getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                tokenMap.get("name"))));
                    }
                } else if ("rename".equals(tokens.get(1))) {
                    HashMap<String, String> tokenMap = parseTokens(tokens);
                    if (tokenMap.get("dry_run") != null) {
                        if (tokenMap.get("overwritten_playlist_id") != null) {
                            mEventBus.post(new PlaylistRenameFailed(Squeezer.getContext().getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                    tokenMap.get("newname"))));
                        } else {
                            sendCommandImmediately(
                                    "playlists rename playlist_id:" + tokenMap.get("playlist_id")
                                            + " newname:" + Util.encode(tokenMap.get("newname")));
                        }
                    }
                } else if ("tracks".equals(tokens.get(1))) {
                    parseSqueezerList(extQueryFormatCmdMap.get("playlists tracks"), tokens);
                } else {
                    parseSqueezerList(extQueryFormatCmdMap.get("playlists"), tokens);
                }
            }
        });
        handlers.put("alarm", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                if ("playlists".equals(tokens.get(1))) {
                    parseSqueezerList(extQueryFormatCmdMap.get("alarm playlists"), tokens);
                }
            }
        });
        handlers.put("login", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Authenticated: " + tokens);
                onAuthenticated();
            }
        });
        handlers.put("pref", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Preference received: " + tokens);
                if ("httpport".equals(tokens.get(1)) && tokens.size() >= 3) {
                    httpPort.set(Integer.parseInt(tokens.get(2)));
                }
                if ("jivealbumsort".equals(tokens.get(1)) && tokens.size() >= 3) {
                    mConnectionState.setPreferedAlbumSort(tokens.get(2));
                }
                if ("mediadirs".equals(tokens.get(1)) && tokens.size() >= 3) {
                    mConnectionState.setMediaDirs(Util.decode(tokens.get(2)));
                }
            }
        });
        handlers.put("can", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Capability received: " + tokens);
                if ("favorites".equals(tokens.get(1)) && tokens.size() >= 4) {
                    mConnectionState.setCanFavorites(Util.parseDecimalIntOrZero(tokens.get(3)) == 1);
                }
                if ("musicfolder".equals(tokens.get(1)) && tokens.size() >= 3) {
                    mConnectionState
                            .setCanMusicfolder(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }
                if ("myapps".equals(tokens.get(1)) && tokens.size() >= 4) {
                    mConnectionState.setCanMyApps(Util.parseDecimalIntOrZero(tokens.get(3)) == 1);
                }
                if ("randomplay".equals(tokens.get(1)) && tokens.size() >= 3) {
                    mConnectionState
                            .setCanRandomplay(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }
            }
        });
        handlers.put("getstring", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                int maxOrdinal = 0;
                Map<String, String> tokenMap = parseTokens(tokens);
                for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
                    if (entry.getValue() != null) {
                        ServerString serverString = ServerString.valueOf(entry.getKey());
                        serverString.setLocalizedString(entry.getValue());
                        if (serverString.ordinal() > maxOrdinal) {
                            maxOrdinal = serverString.ordinal();
                        }
                    }
                }

                // Fetch the next strings until the list is completely translated
                if (maxOrdinal < ServerString.values().length - 1) {
                    sendCommandImmediately(
                            "getstring " + ServerString.values()[maxOrdinal + 1].name());
                }
            }
        });
        handlers.put("version", new CmdHandler() {
            /**
             * Seeing the <code>version</code> result indicates that the
             * handshake has completed (see
             * {@link CliClient#onCliPortConnectionEstablished(String, String)}),
             * post a {@link HandshakeComplete} event.
             */
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Version received: " + tokens);
                mUrlPrefix = "http://" + getCurrentHost() + ":" + getHttpPort();
                String version = tokens.get(1);
                mConnectionState.setServerVersion(version);
                Util.crashlyticsSetString("server_version", version);
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializePrefixedHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HANDLER_LIST_PREFIXED) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        return handlers;
    }

    /**
     * Initialise handlers for player-specific commands.
     * <p>
     * All commands processed by these handlers start with the player ID.
     *
     * @return
     */
    private Map<String, CmdHandler> initializePlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HANDLER_LIST_PLAYER_SPECIFIC) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }
        handlers.put("play", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "play registered");
                updatePlayStatus(Util.decode(tokens.get(0)), PlayerState.PLAY_STATE_PLAY);
            }
        });
        handlers.put("stop", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "stop registered");
                updatePlayStatus(Util.decode(tokens.get(0)), PlayerState.PLAY_STATE_STOP);
            }
        });
        handlers.put("pause", new CmdHandler() {
            /**
             * <code>&lt;playerid> pause &lt;0|1|></code>
             * @param tokens
             */
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "pause registered: " + tokens);
                updatePlayStatus(Util.decode(tokens.get(0)), parsePause(tokens.size() >= 3 ? tokens.get(2) : null));
            }
        });
        handlers.put("playlist", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                parsePlaylistNotification(tokens);
            }
        });
        handlers.put("playerpref", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Player preference received: " + tokens);
                if (tokens.size() == 4) {
                    Player player = mPlayers.get(Util.decode(tokens.get(0)));
                    if (player == null) {
                        return;
                    }

                    String pref = Util.decode(tokens.get(2));
                    if (Player.Pref.VALID_PLAYER_PREFS.contains(pref)) {
                        //noinspection WrongConstant
                        mEventBus.post(new PlayerPrefReceived(player, pref,
                                Util.decode(tokens.get(3))));
                    }
                }
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializeGlobalPlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            if (cmd.handlerList == HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        // &lt;playerid> client &lt;new|disconnect|reconnect>
        handlers.put("client", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "client received: " + tokens);
                // Something has happened to the player list, we just fetch the full list again.
                //
                // Reasons to do this:
                //
                // Issuing a "<playerid> status" request will not return the same information that
                // "players" does, missing "model", "displaytype", "isplayer", "displaytype", and
                // "canpoweroff" information.

                fetchPlayers();
            }
        });
        handlers.put("status", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                if (tokens.size() >= 3 && "-".equals(tokens.get(2))) {
                    Player player = mPlayers.get(Util.decode(tokens.get(0)));

                    // XXX: Can we ever see a status for a player we don't know about?
                    // XXX: Maybe the better thing to do is to add it.
                    if (player == null)
                        return;

                    HashMap<String, String> tokenMap = parseTokens(tokens);
                    parseStatus(player, null, tokenMap);
                } else {
                    parseSqueezerList(extQueryFormatCmdMap.get("status"), tokens);
                }
            }
        });
        handlers.put("prefset", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "Prefset received: " + tokens);
                if (tokens.size() == 5 && tokens.get(2).equals("server")) {
                    String playerId = Util.decode(tokens.get(0));
                    Player player = mPlayers.get(playerId);
                    if (player == null) {
                        return;
                    }

                    if (tokens.get(3).equals("volume")) {
                        updatePlayerVolume(playerId, Util.parseDecimalIntOrZero(tokens.get(4)));
                    }

                    @Player.Pref.Name String pref = tokens.get(3);
                    if (Player.Pref.VALID_PLAYER_PREFS.contains(pref)) {
                        mEventBus.post(new PlayerPrefReceived(player, pref, Util.decode(tokens.get(4))));
                    }
                }
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializePrefixedPlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        return handlers;
    }

    public void onLineReceived(String serverLine) {
        Log.v(TAG, "RECV: " + serverLine);

        // Make sure that username/password do not make it to Crashlytics.
        if (serverLine.startsWith("login ")) {
            Util.crashlyticsSetString("lastReceivedLine", "login [username] [password]");
        } else {
            Util.crashlyticsSetString("lastReceivedLine", serverLine);
        }

        List<String> tokens = Arrays.asList(mSpaceSplitPattern.split(serverLine));
        if (tokens.size() < 2) {
            return;
        }

        CmdHandler handler;
        if ((handler = globalHandlers.get(tokens.get(0))) != null) {
            handler.handle(tokens);
            return;
        }
        if ((handler = prefixedHandlers.get(tokens.get(1))) != null) {
            handler.handle(tokens);
            return;
        }
        if ((handler = globalPlayerSpecificHandlers.get(tokens.get(1))) != null) {
            handler.handle(tokens);
            return;
        }

        // Player-specific commands
        if ((handler = playerSpecificHandlers.get(tokens.get(1))) != null) {
            handler.handle(tokens);
            return;
        }
        if (tokens.size() > 2
                && (handler = prefixedPlayerSpecificHandlers.get(tokens.get(2))) != null) {
            handler.handle(tokens);
        }
    }

    private HashMap<String, String> parseTokens(List<String> tokens) {
        HashMap<String, String> tokenMap = new HashMap<>();
        String[] kv;
        for (String token : tokens) {
            kv = parseToken(token);
            if (kv.length == 0)
                continue;

            tokenMap.put(kv[0], kv[1]);
        }
        return tokenMap;
    }

    /**
     * Parse a token in to a key-value pair.  The value is optional.
     * <p>
     * The token is assumed to be URL encoded, with the key and value separated by ':' (encoded
     * as '%3A').
     *
     * @param token The string to decode.
     * @return An array -- empty if token is null or empty, otherwise with two elements. The first
     * is the key, the second, which may be null, is the value. The elements are decoded.
     */
    private String[] parseToken(@Nullable String token) {
        String key, value;

        if (token == null || token.length() == 0) {
            return new String[]{};
        }

        int colonPos = token.indexOf("%3A");
        if (colonPos == -1) {
            key = Util.decode(token);
            value = null;
        } else {
            key = Util.decode(token.substring(0, colonPos));
            value = Util.decode(token.substring(colonPos + 3));
        }

        return new String[]{key, value};
    }

    private @PlayerState.PlayState String parsePause(String explicitPause) {
        if ("0".equals(explicitPause)) {
            return PlayerState.PLAY_STATE_PLAY;
            //updatePlayStatus(PlayerState.PlayStatus.play);
        } else if ("1".equals(explicitPause)) {
            return PlayerState.PLAY_STATE_PAUSE;
            //updatePlayStatus(PlayerState.PlayStatus.pause);
        }
        //updateAllPlayerSubscriptionStates();

        // XXX: This is probably not correct. Log and return something else?
        return PlayerState.PLAY_STATE_PAUSE;
    }

    private void parsePlaylistNotification(List<String> tokens) {
        Log.v(TAG, "Playlist notification received: " + tokens);
        String notification = tokens.get(2);
        if ("newsong".equals(notification)) {
            sendCommand(tokens.get(0) + " status - 1 tags:" + SqueezeService.SONGTAGS);
        } else if ("addtracks".equals(notification)) {
            mEventBus.postSticky(new PlaylistTracksAdded());
        } else if ("delete".equals(notification)) {
            mEventBus.postSticky(new PlaylistTracksDeleted());
        }

        // Ignore "play", "stop", "pause" playlist notifications that come through here,
        // as they come through every time a track changes, causing the notification to
        // briefly disappear and re-appear. The top level "play", "stop", and "pause"
        // messages don't have this problem.
    }

    private void updatePlayerVolume(String playerId, int newVolume) {
        Player player = mPlayers.get(playerId);
        if (player == null) {
            return;
        }
        player.getPlayerState().setCurrentVolume(newVolume);
        mEventBus.post(new PlayerVolume(newVolume, player));
    }

    private void updatePlayStatus(@NonNull String playerId, String playStatus) {
        Player player = mPlayers.get(playerId);

        if (player == null) {
            return;
        }

        updatePlayStatus(player, playStatus);
    }

    /**
     * Handshake with the SqueezeServer, learn some of its supported features, and start listening
     * for asynchronous updates of server state.
     *
     * Note: Authentication may not actually have completed at this point. The server has
     * responded to the "login" request, but if the username/password pair was incorrect it
     * has (probably) not yet disconnected the socket. See
     * {@link ListeningThread#run()} for the code
     * that determines whether authentication succeeded.
     */
    private void onAuthenticated() {
        fetchPlayers();
        sendCommandImmediately(
                "listen 1", // subscribe to all server notifications
                "can musicfolder ?", // learn music folder browsing support
                "can randomplay ?", // learn random play function functionality
                "can favorites items ?", // learn support for "Favorites" plugin
                "can myapps items ?", // learn support for "MyApps" plugin
                "pref httpport ?", // learn the HTTP port (needed for images)
                "pref jivealbumsort ?", // learn the preferred album sort order
                "pref mediadirs ?", // learn the base path(s) of the server music library

                // Fetch the version number. This must be the last thing
                // fetched, as seeing the result triggers the
                // "handshake is complete" logic elsewhere.
                "version ?"
        );
    }

    /**
     * Queries for all players known by the server.
     * </p>
     * Posts a PlayersChanged message if the list of players has changed.
     */
    private void fetchPlayers() {
        requestItems("players", -1, new IServiceItemListCallback<Player>() {
            private final HashMap<String, Player> players = new HashMap<>();

            @Override
            public void onItemsReceived(int count, int start, Map<String, String> parameters,
                                        List<Player> items, Class<Player> dataType) {
                for (Player player : items) {
                    players.put(player.getId(), player);
                }

                // If all players have been received then determine the new active player.
                if (start + items.size() >= count) {
                    if (players.equals(mPlayers)) {
                        return;
                    }

                    mPlayers.clear();
                    mPlayers.putAll(players);

                    // XXX: postSticky?
                    mEventBus.postSticky(new PlayersChanged(mPlayers));
                }
            }

            @Override
            public Object getClient() {
                return CliClient.this;
            }
        });
    }

    private int getHttpPort() {
        return httpPort.get();
    }

    private String getCurrentHost() {
        return currentHost.get();
    }

    public String[] getMediaDirs() {
        return mConnectionState.getMediaDirs();
    }

    public String getPreferredAlbumSort() {
        return mConnectionState.getPreferredAlbumSort();
    }



    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    private final AtomicReference<Socket> socketRef = new AtomicReference<>();

    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<>();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<>();

    private final AtomicReference<Integer> httpPort = new AtomicReference<>();

    private final AtomicReference<Integer> cliPort = new AtomicReference<>();

    private final AtomicReference<String> userName = new AtomicReference<>();

    private final AtomicReference<String> password = new AtomicReference<>();

    void startListeningThread(@NonNull Executor executor) {
        Thread listeningThread = new ListeningThread(executor, this, socketRef.get(),
                currentConnectionGeneration.incrementAndGet());
        listeningThread.start();
    }

    private class ListeningThread extends Thread {
        @NonNull private final Executor mExecutor;

        private final Socket socket;

        private final SlimClient client;

        private final int generationNumber;

        private ListeningThread(@NonNull Executor executor, SlimClient client, Socket socket, int generationNumber) {
            mExecutor = executor;
            this.client = client;
            this.socket = socket;
            this.generationNumber = generationNumber;
        }

        @Override
        public void run() {
            Log.d(TAG, "Listening thread started");

            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                Log.v(TAG, "IOException while creating BufferedReader: " + e);
                client.disconnect(false);
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
                        client.disconnect(exception == null);
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
                if (mConnectionState.isLoginStarted() && !inputLine.startsWith("login ")) {
                    mConnectionState.setConnectionState(ConnectionState.LOGIN_COMPLETED);
                }
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        client.onLineReceived(inputLine);
                    }
                });
            }
        }
    }

    /**
     * Authenticate on the SqueezeServer.
     * <p>
     * The server does
     * <pre>
     * login user wrongpassword
     * login user ******
     * (Connection terminated)
     * </pre>
     * instead of as documented
     * <pre>
     * login user wrongpassword
     * (Connection terminated)
     * </pre>
     * therefore a disconnect when handshake (the next step after authentication) is not completed,
     * is considered an authentication failure.
     */
    private void onCliPortConnectionEstablished(final String userName, final String password) {
        mConnectionState.setConnectionState(ConnectionState.LOGIN_STARTED);
        sendCommandImmediately("login " + Util.encode(userName) + " " + Util.encode(password));
    }

}
