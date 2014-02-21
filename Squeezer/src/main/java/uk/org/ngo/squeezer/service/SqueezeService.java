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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceCurrentPlaylistCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlaylistMaintenanceCallback;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.itemlist.dialog.SongViewDialog;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.PlayerState.PlayStatus;
import uk.org.ngo.squeezer.model.PlayerState.RepeatStatus;
import uk.org.ngo.squeezer.model.PlayerState.ShuffleStatus;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.util.Scrobble;


public class SqueezeService extends Service {

    private static final String TAG = "SqueezeService";

    private static final int PLAYBACKSERVICE_STATUS = 1;

    private static final String ALBUMTAGS = "alyj";

    /**
     * Information that will be requested about songs.
     * <p/>
     * a: artist name<br/>
     * C: compilation (1 if true, missing otherwise)<br/>
     * d: duration, in seconds<br/>
     * e: album ID<br/>
     * j: coverart (1 if available, missing otherwise)<br/>
     * J: artwork_track_id (if available, missing otherwise)<br/>
     * K: URL to remote artwork<br/>
     * l: album name<br/>
     * s: artist id<br/>
     * t: tracknum, if known<br/>
     * x: 1, if this is a remote track<br/>
     * y: song year
     */
    // This should probably be a field in Song.
    private static final String SONGTAGS = "aCdejJKlstxy";

    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    Thread mainThread;

    private boolean mHandshakeComplete = false;

    final ServiceCallbackList<IServiceCallback> mServiceCallbacks
            = new ServiceCallbackList<IServiceCallback>();

    final ServiceCallbackList<IServiceCurrentPlaylistCallback> mCurrentPlaylistCallbacks
            = new ServiceCallbackList<IServiceCurrentPlaylistCallback>();

    final ServiceCallbackList<IServiceMusicChangedCallback> mMusicChangedCallbacks
            = new ServiceCallbackList<IServiceMusicChangedCallback>();

    final ServiceCallbackList<IServiceHandshakeCallback> mHandshakeCallbacks
            = new ServiceCallbackList<IServiceHandshakeCallback>();

    final AtomicReference<IServicePlaylistMaintenanceCallback> playlistMaintenanceCallback
            = new AtomicReference<IServicePlaylistMaintenanceCallback>();

    final ServiceCallbackList<IServiceVolumeCallback> mVolumeCallbacks
            = new ServiceCallbackList<IServiceVolumeCallback>();

    ConnectionState connectionState = new ConnectionState();

    PlayerState playerState = new PlayerState();

    CliClient cli = new CliClient(this);

    /**
     * Is scrobbling enabled?
     */
    private boolean scrobblingEnabled;

    /**
     * Was scrobbling enabled?
     */
    private boolean scrobblingPreviouslyEnabled;

    boolean debugLogging; // Enable this if you are debugging something

    boolean mUpdateOngoingNotification;

    int mFadeInSecs;

    @Override
    public void onCreate() {
        super.onCreate();

        // Get the main thread
        mainThread = Thread.currentThread();

        // Clear leftover notification in case this service previously got killed while playing
        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
        connectionState
                .setWifiLock(((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(
                        WifiManager.WIFI_MODE_FULL, "Squeezer_WifiLock"));

        getPreferences();

        cli.initialize();
    }

    private void getPreferences() {
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        scrobblingEnabled = preferences.getBoolean(Preferences.KEY_SCROBBLE_ENABLED, false);
        mFadeInSecs = preferences.getInt(Preferences.KEY_FADE_IN_SECS, 0);
        mUpdateOngoingNotification = preferences
                .getBoolean(Preferences.KEY_NOTIFY_OF_CONNECTION, false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (IBinder) squeezeService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    void disconnect() {
        disconnect(false);
    }

    void disconnect(boolean isServerDisconnect) {
        connectionState.disconnect(this, isServerDisconnect && !mHandshakeComplete);
        mHandshakeComplete = false;
        clearOngoingNotification();
        playerState = new PlayerState();
    }


    private interface CmdHandler {

        public void handle(List<String> tokens);
    }

    private Map<String, CmdHandler> initializeGlobalHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (!(cmd.playerSpecific || cmd.prefixed)) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
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
                        IServicePlaylistMaintenanceCallback callback = playlistMaintenanceCallback
                                .get();
                        if (callback != null) {
                            callback.onCreateFailed(getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                        tokenMap.get("name")));
                        }
                    }
                } else if ("rename".equals(tokens.get(1))) {
                    HashMap<String, String> tokenMap = parseTokens(tokens);
                    if (tokenMap.get("dry_run") != null) {
                        if (tokenMap.get("overwritten_playlist_id") != null) {
                            IServicePlaylistMaintenanceCallback callback
                                    = playlistMaintenanceCallback.get();
                            if (callback != null) {
                                callback.onRenameFailed(
                                        getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                                tokenMap.get("newname")));
                            }
                        } else {
                            cli.sendCommandImmediately(
                                    "playlists rename playlist_id:" + tokenMap.get("playlist_id")
                                            + " newname:" + Util.encode(tokenMap.get("newname")));
                        }
                    }
                } else if ("tracks".equals(tokens.get(1))) {
                    cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("playlists tracks"), tokens);
                } else {
                    cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("playlists"), tokens);
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
                    connectionState.setHttpPort(Integer.parseInt(tokens.get(2)));
                }
                if ("jivealbumsort".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState.setPreferedAlbumSort(tokens.get(2));
                }
            }
        });
        handlers.put("can", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Capability received: " + tokens);
                if ("musicfolder".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState
                            .setCanMusicfolder(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }

                if ("randomplay".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState
                            .setCanRandomplay(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }
            }
        });
        handlers.put("getstring", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                int maxOrdinal = 0;
                Map<String, String> tokenMap = parseTokens(tokens);
                for (Entry<String, String> entry : tokenMap.entrySet()) {
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
                    cli.sendCommandImmediately(
                            "getstring " + ServerString.values()[maxOrdinal + 1].name());
                }
            }
        });
        handlers.put("version", new CmdHandler() {
            /**
             * Seeing the <code>version</code> result indicates that the
             * handshake has completed (see
             * {@link SqueezeService#onCliPortConnectionEstablished(String, String)}), call any handshake
             * callbacks that have been registered.
             */
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Version received: " + tokens);
                mHandshakeComplete = true;
                strings();

                for (IServiceHandshakeCallback callback : mHandshakeCallbacks) {
                    callback.onHandshakeCompleted();
                }
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializePrefixedHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.prefixed && !cmd.playerSpecific) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        return handlers;
    }

    private Map<String, CmdHandler> initializePlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.playerSpecific && !cmd.prefixed) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }
        handlers.put("prefset", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "Prefset received: " + tokens);
                if (tokens.size() > 4 && tokens.get(2).equals("server") && tokens.get(3)
                        .equals("volume")) {
                    String newVolume = tokens.get(4);
                    updatePlayerVolume(Util.parseDecimalIntOrZero(newVolume));
                }
            }
        });
        handlers.put("play", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "play registered");
                updatePlayStatus(PlayerState.PlayStatus.play);
            }
        });
        handlers.put("stop", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "stop registered");
                updatePlayStatus(PlayerState.PlayStatus.stop);
            }
        });
        handlers.put("pause", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "pause registered: " + tokens);
                parsePause(tokens.size() >= 3 ? tokens.get(2) : null);
            }
        });
        handlers.put("status", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                if (tokens.size() >= 3 && "-".equals(tokens.get(2))) {
                    parseStatusLine(tokens);
                } else {
                    cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("status"), tokens);
                }
            }
        });
        handlers.put("playlist", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                parsePlaylistNotification(tokens);
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializePrefixedPlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.playerSpecific && cmd.prefixed) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        return handlers;
    }

    private final Map<String, CmdHandler> globalHandlers = initializeGlobalHandlers();

    private final Map<String, CmdHandler> prefixedHandlers = initializePrefixedHandlers();

    private final Map<String, CmdHandler> playerSpecificHandlers
            = initializePlayerSpecificHandlers();

    private final Map<String, CmdHandler> prefixedPlayerSpecificHandlers
            = initializePrefixedPlayerSpecificHandlers();

    void onLineReceived(String serverLine) {
        if (debugLogging) {
            Log.v(TAG, "LINE: " + serverLine);
        }
        List<String> tokens = Arrays.asList(serverLine.split(" "));
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

        // Player-specific commands follow.  But we ignore all that aren't for our
        // active player.
        String activePlayerId = (connectionState.getActivePlayer() != null ? connectionState
                .getActivePlayer().getId() : null);
        if (activePlayerId == null || activePlayerId.length() == 0 ||
                !Util.decode(tokens.get(0)).equals(activePlayerId)) {
            // Different player that we're not interested in.
            // (yet? maybe later.)
            return;
        }
        if ((handler = playerSpecificHandlers.get(tokens.get(1))) != null) {
            handler.handle(tokens);
            return;
        }
        if (tokens.size() > 2
                && (handler = prefixedPlayerSpecificHandlers.get(tokens.get(2))) != null) {
            handler.handle(tokens);
        }
    }

    private void updatePlayerVolume(int newVolume) {
        playerState.setCurrentVolume(newVolume);
        Player player = connectionState.getActivePlayer();
        for (IServiceVolumeCallback callback : mVolumeCallbacks) {
            callback.onVolumeChanged(newVolume, player);
        }
    }

    private void updateTimes(int secondsIn, int secondsTotal) {
        playerState.setCurrentSongDuration(secondsTotal);
        if (playerState.getCurrentTimeSecond() != secondsIn) {
            playerState.setCurrentTimeSecond(secondsIn);
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onTimeInSongChange(secondsIn, secondsTotal);
            }
        }
    }

    private void parsePlaylistNotification(List<String> tokens) {
        Log.v(TAG, "Playlist notification received: " + tokens);
        String notification = tokens.get(2);
        if ("newsong".equals(notification)) {
            // When we don't subscribe to the current players status, we rely
            // on playlist notifications and order song details here.
            // TODO keep track of subscribe status
            cli.sendPlayerCommand("status - 1 tags:" + SONGTAGS);
        } else if ("play".equals(notification)) {
            updatePlayStatus(PlayerState.PlayStatus.play);
        } else if ("stop".equals(notification)) {
            updatePlayStatus(PlayerState.PlayStatus.stop);
        } else if ("pause".equals(notification)) {
            parsePause(tokens.size() >= 4 ? tokens.get(3) : null);
        } else if ("addtracks".equals(notification)) {
            for (IServiceCurrentPlaylistCallback callback : mCurrentPlaylistCallbacks) {
                callback.onAddTracks(playerState);
            }
        } else if ("delete".equals(notification)) {
            for (IServiceCurrentPlaylistCallback callback : mCurrentPlaylistCallbacks) {
                callback.onDelete(playerState, Integer.parseInt(tokens.get(3)));
            }
        }
    }

    private void parsePause(String explicitPause) {
        if ("0".equals(explicitPause)) {
            updatePlayStatus(PlayerState.PlayStatus.play);
        } else if ("1".equals(explicitPause)) {
            updatePlayStatus(PlayerState.PlayStatus.pause);
        }
    }

    private HashMap<String, String> parseTokens(List<String> tokens) {
        HashMap<String, String> tokenMap = new HashMap<String, String>();
        String key, value;
        for (String token : tokens) {
            if (token == null || token.length() == 0) {
                continue;
            }
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
                key = Util.decode(token);
                value = null;
            } else {
                key = Util.decode(token.substring(0, colonPos));
                value = Util.decode(token.substring(colonPos + 3));
            }
            tokenMap.put(key, value);
        }
        return tokenMap;
    }

    private void parseStatusLine(List<String> tokens) {
        HashMap<String, String> tokenMap = parseTokens(tokens);

        updatePowerStatus(Util.parseDecimalIntOrZero(tokenMap.get("power")) == 1);
        updatePlayStatus(tokenMap.get("mode"));
        updateShuffleStatus(tokenMap.get("playlist shuffle"));
        updateRepeatStatus(tokenMap.get("playlist repeat"));

        playerState.setCurrentPlaylist(tokenMap.get("playlist_name"));
        playerState.setCurrentPlaylistIndex(
                Util.parseDecimalIntOrZero(tokenMap.get("playlist_cur_index")));
        updateCurrentSong(new Song(tokenMap));

        updateTimes(Util.parseDecimalIntOrZero(tokenMap.get("time")),
                Util.parseDecimalIntOrZero(tokenMap.get("duration")));
    }

    void changeActivePlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }

        Log.v(TAG, "Active player now: " + newPlayer);
        final String playerId = newPlayer.getId();
        String oldPlayerId = (connectionState.getActivePlayer() != null ? connectionState
                .getActivePlayer().getId() : null);
        boolean changed = false;
        if (oldPlayerId == null || !oldPlayerId.equals(playerId)) {
            if (oldPlayerId != null) {
                // Unsubscribe from the old player's status.
                cli.sendCommand(Util.encode(oldPlayerId) + " status - 1 subscribe:-");
            }

            connectionState.setActivePlayer(newPlayer);
            playerState = new PlayerState();
            changed = true;
        }

        // Start an async fetch of its status.
        cli.sendPlayerCommand("status - 1 tags:" + SONGTAGS);

        if (changed) {
            updatePlayerSubscriptionState();

            // NOTE: this involves a write and can block (sqlite lookup via binder call), so
            // should be done off-thread, so we can process service requests & send our callback
            // as quickly as possible.
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Saving " + Preferences.KEY_LASTPLAYER + "=" + playerId);
                    final SharedPreferences preferences = getSharedPreferences(Preferences.NAME,
                            MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(Preferences.KEY_LASTPLAYER, playerId);
                    editor.commit();
                }
            });
        }

        for (IServiceCallback callback : mServiceCallbacks) {
            callback.onPlayerChanged(newPlayer);
        }
    }


    private void updatePlayerSubscriptionState() {
        // Subscribe or unsubscribe to the player's realtime status updates
        // depending on whether we have an Activity or some sort of client
        // that cares about second-to-second updates.
        if (mServiceCallbacks.count() > 0) {
            cli.sendPlayerCommand("status - 1 subscribe:1 tags:" + SONGTAGS);
        } else {
            cli.sendPlayerCommand("status - 1 subscribe:-");
        }
    }

    /**
     * Authenticate on the SqueezeServer.
     * <p/>
     * The server does
     * <pre>
     * login user wrongpassword
     * login user ******
     * (Connection terminted)
     * </pre>
     * instead of as documented
     * <pre>
     * login user wrongpassword
     * (Connection terminted)
     * </pre>
     * therefore a disconnect when handshake (the next step after authentication) is not completed,
     * is considered an authentication failure.
     */
    void onCliPortConnectionEstablished(final String userName, final String password) {
        cli.sendCommandImmediately("login " + Util.encode(userName) + " " + Util.encode(password));
    }

    /**
     * Handshake with the SqueezeServer, learn some of its supported features, and start listening
     * for asynchronous updates of server state.
     */
    private void onAuthenticated() {
        // initiate an async player fetch
        cli.requestItems("players", 0, new IServiceItemListCallback<Player>() {
            @Override
            public void onItemsReceived(int count, int start, Map<String, String> parameters, List<Player> items, Class<Player> dataType) {
                final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
                final String lastConnectedPlayer = preferences.getString(Preferences.KEY_LASTPLAYER, null);
                Player defaultPlayer = null;
                Log.v(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);
                for (Player player : items) {
                    if (defaultPlayer == null || player.getId().equals(lastConnectedPlayer)) {
                        defaultPlayer = player;
                    }
                }
                if (defaultPlayer != null) {
                    changeActivePlayer(defaultPlayer);
                }
            }

            @Override
            public Object getClient() {
                return SqueezeService.this;
            }
        });

        cli.sendCommandImmediately(
                "listen 1", // subscribe to all server notifications
                "can musicfolder ?", // learn music folder browsing support
                "can randomplay ?",   // learn random play function functionality
                "pref httpport ?", // learn the HTTP port (needed for images)
                "pref jivealbumsort ?", // learn the preferred album sort order

                // Fetch the version number. This must be the last thing
                // fetched, as seeing the result triggers the
                // "handshake is complete" logic elsewhere.
                "version ?"
        );
    }

    /* Start an asynchronous fetch of the squeezeservers localized strings */
    private void strings() {
        cli.sendCommandImmediately("getstring " + ServerString.values()[0].name());
    }

    private void updatePlayStatus(String token) {
        if (token != null)
            try {
                updatePlayStatus(PlayStatus.valueOf(token));
            } catch (IllegalArgumentException e) {
                // Server sent us an unknown status, we skip the update
            }
    }

    private void updatePlayStatus(PlayStatus playStatus) {
        if (playerState.getPlayStatus() != playStatus) {
            playerState.setPlayStatus(playStatus);
            //TODO when do we want to keep the wiFi lock ?
            connectionState.updateWifiLock(playerState.isPlaying());
            updateOngoingNotification();
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onPlayStatusChanged(playStatus.name());
            }
        }
    }

    private void updateShuffleStatus(String token) {
        if (token != null) {
            ShuffleStatus shuffleStatus = ShuffleStatus.valueOf(Util.parseDecimalIntOrZero(token));
            if (shuffleStatus != playerState.getShuffleStatus()) {
                boolean wasUnknown = playerState.getShuffleStatus() == null;
                playerState.setShuffleStatus(shuffleStatus);
                onShuffleStatusChanged(wasUnknown, shuffleStatus);
            }
        }
    }

    private void onShuffleStatusChanged(boolean initial, ShuffleStatus shuffleStatus) {
        for (IServiceCallback callback : mServiceCallbacks) {
            callback.onShuffleStatusChanged(initial, shuffleStatus.getId());
        }
    }

    private void updateRepeatStatus(String token) {
        if (token != null) {
            RepeatStatus repeatStatus = RepeatStatus.valueOf(Util.parseDecimalIntOrZero(token));
            if (repeatStatus != playerState.getRepeatStatus()) {
                boolean wasUnknown = playerState.getRepeatStatus() == null;
                playerState.setRepeatStatus(repeatStatus);
                onRepeatStatusChanged(wasUnknown, repeatStatus);
            }
        }
    }

    private void onRepeatStatusChanged(boolean wasUnknown, RepeatStatus repeatStatus) {
        for (IServiceCallback callback : mServiceCallbacks) {
            callback.onRepeatStatusChanged(wasUnknown, repeatStatus.getId());
        }
    }

    private void updateOngoingNotification() {
        boolean playing = playerState.isPlaying();
        String songName = playerState.getCurrentSongName();
        String playerName = connectionState.getActivePlayer() != null ? connectionState
                .getActivePlayer().getName() : "squeezer";

        // Update scrobble state, if either we're currently scrobbling, or we
        // were (to catch the case where we started scrobbling a song, and the
        // user went in to settings to disable scrobbling).
        if (scrobblingEnabled || scrobblingPreviouslyEnabled) {
            scrobblingPreviouslyEnabled = scrobblingEnabled;
            Song s = playerState.getCurrentSong();

            if (s != null) {
                Log.v(TAG, "Scrobbling, playing is: " + playing);
                Intent i = new Intent();

                if (Scrobble.haveScrobbleDroid()) {
                    // http://code.google.com/p/scrobbledroid/wiki/DeveloperAPI
                    i.setAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
                    i.putExtra("playing", playing);
                    i.putExtra("track", songName);
                    i.putExtra("album", s.getAlbum());
                    i.putExtra("artist", s.getArtist());
                    i.putExtra("secs", playerState.getCurrentSongDuration());
                    i.putExtra("source", "P");
                } else if (Scrobble.haveSls()) {
                    // http://code.google.com/p/a-simple-lastfm-scrobbler/wiki/Developers
                    i.setAction("com.adam.aslfms.notify.playstatechanged");
                    i.putExtra("state", playing ? 0 : 2);
                    i.putExtra("app-name", getText(R.string.app_name));
                    i.putExtra("app-package", "uk.org.ngo.squeezer");
                    i.putExtra("track", songName);
                    i.putExtra("album", s.getAlbum());
                    i.putExtra("artist", s.getArtist());
                    i.putExtra("duration", playerState.getCurrentSongDuration());
                    i.putExtra("source", "P");
                }
                sendBroadcast(i);
            }
        }

        if (!playing) {
            if (!mUpdateOngoingNotification) {
                clearOngoingNotification();
                return;
            }
        }

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification status = new Notification();
        //status.contentView = views;
        Intent showNowPlaying = new Intent(this, NowPlayingActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, showNowPlaying, 0);
        if (playing) {
            status.setLatestEventInfo(this,
                    getString(R.string.notification_playing_text, playerName), songName, pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
        } else {
            status.setLatestEventInfo(this,
                    getString(R.string.notification_connected_text, playerName), "-", pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.ic_launcher;
        }
        nm.notify(PLAYBACKSERVICE_STATUS, status);
    }

    private void updateCurrentSong(Song song) {
        Song currentSong = playerState.getCurrentSong();
        if ((song == null ? (currentSong != null) : !song.equals(currentSong))) {
            Log.d(TAG, "updateCurrentSong: " + song);
            playerState.setCurrentSong(song);
            updateOngoingNotification();
            for (IServiceMusicChangedCallback callback : mMusicChangedCallbacks) {
                callback.onMusicChanged(playerState);
            }
        }
    }

    private void clearOngoingNotification() {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
    }

    private void updatePowerStatus(boolean powerStatus) {
        if (powerStatus != playerState.isPoweredOn()) {
            playerState.setPoweredOn(powerStatus);
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onPowerStatusChanged(squeezeService.canPowerOn(),
                        squeezeService.canPowerOff());
            }
        }
    }


    private final ISqueezeService squeezeService = new SqueezeServiceBinder();
    private class SqueezeServiceBinder extends Binder implements ISqueezeService {

        @Override
        public void registerCallback(IServiceCallback callback) {
            mServiceCallbacks.register(callback);
            updatePlayerSubscriptionState();
        }

        @Override
        public void unregisterCallback(IServiceCallback callback) {
            mServiceCallbacks.unregister(callback);
            updatePlayerSubscriptionState();
        }

        @Override
        public void registerCurrentPlaylistCallback(IServiceCurrentPlaylistCallback callback) {
            mCurrentPlaylistCallbacks.register(callback);
        }

        @Override
        public void unregisterCurrentPlaylistCallback(IServiceCurrentPlaylistCallback callback) {
            mCurrentPlaylistCallbacks.unregister(callback);
        }

        @Override
        public void registerMusicChangedCallback(IServiceMusicChangedCallback callback) {
            mMusicChangedCallbacks.register(callback);
        }

        @Override
        public void unregisterMusicChangedCallback(IServiceMusicChangedCallback callback) {
            mMusicChangedCallbacks.unregister(callback);
        }

        @Override
        public void registerHandshakeCallback(IServiceHandshakeCallback callback) {
            mHandshakeCallbacks.register(callback);

            // Call onHandshakeCompleted() immediately if handshaking is done.
            if (mHandshakeComplete) {
                callback.onHandshakeCompleted();
            }
        }

        @Override
        public void unregisterHandshakeCallback(IServiceHandshakeCallback callback) {
            mHandshakeCallbacks.unregister(callback);
        }

        @Override
        public void registerVolumeCallback(IServiceVolumeCallback callback) {
            mVolumeCallbacks.register(callback);
        }

        @Override
        public void unregisterVolumeCallback(IServiceVolumeCallback callback) {
            mVolumeCallbacks.unregister(callback);
        }

        @Override
        public void adjustVolumeTo(int newVolume) {
            cli.sendPlayerCommand("mixer volume " + Math.min(100, Math.max(0, newVolume)));
        }

        @Override
        public void adjustVolumeBy(int delta) {
            if (delta > 0) {
                cli.sendPlayerCommand("mixer volume %2B" + delta);
            } else if (delta < 0) {
                cli.sendPlayerCommand("mixer volume " + delta);
            }
        }

        @Override
        public boolean isConnected() {
            return connectionState.isConnected();
        }

        @Override
        public boolean isConnectInProgress() {
            return connectionState.isConnectInProgress();
        }

        @Override
        public void startConnect(String hostPort, String userName, String password) {
            connectionState
                    .startConnect(SqueezeService.this, executor, hostPort, userName, password);
        }

        @Override
        public void disconnect() {
            if (!isConnected()) {
                return;
            }
            SqueezeService.this.disconnect();
        }

        @Override
        public boolean powerOn() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("power 1");
            return true;
        }

        @Override
        public boolean powerOff() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("power 0");
            return true;
        }

        @Override
        public boolean canPowerOn() {
            return canPower() && !playerState.isPoweredOn();
        }

        @Override
        public boolean canPowerOff() {
            return canPower() && playerState.isPoweredOn();
        }

        private boolean canPower() {
            Player player = connectionState.getActivePlayer();
            return connectionState.isConnected() && player != null && player.isCanpoweroff();
        }

        /**
         * Determines whether the Squeezeserver supports the
         * <code>musicfolders</code> command.
         *
         * @return <code>true</code> if it does, <code>false</code> otherwise.
         */
        @Override
        public boolean canMusicfolder() {
            return connectionState.canMusicfolder();
        }

        @Override
        public boolean canRandomplay() {
            return connectionState.canRandomplay();
        }

        @Override
        public String preferredAlbumSort() {
            return connectionState.getPreferredAlbumSort();
        }

        private String fadeInSecs() {
            return mFadeInSecs > 0 ? " " + mFadeInSecs : "";
        }

        @Override
        public boolean togglePausePlay() {
            if (!isConnected()) {
                return false;
            }

            PlayerState.PlayStatus playStatus = playerState.getPlayStatus();

            // May be null (e.g., connected to a server with no connected
            // players. TODO: Handle this better, since it's not obvious in the
            // UI.
            if (playStatus == null) {
                return false;
            }

            switch (playStatus) {
                case play:
                    // NOTE: we never send ambiguous "pause" toggle commands (without the '1')
                    // because then we'd get confused when they came back in to us, not being
                    // able to differentiate ours coming back on the listen channel vs. those
                    // of those idiots at the dinner party messing around.
                    cli.sendPlayerCommand("pause 1");
                    break;
                case stop:
                    cli.sendPlayerCommand("play" + fadeInSecs());
                    break;
                case pause:
                    cli.sendPlayerCommand("pause 0" + fadeInSecs());
                    break;
            }
            return true;
        }

        @Override
        public boolean play() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("play" + fadeInSecs());
            return true;
        }

        @Override
        public boolean stop() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("stop");
            return true;
        }

        @Override
        public boolean nextTrack() {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            cli.sendPlayerCommand("button jump_fwd");
            return true;
        }

        @Override
        public boolean previousTrack() {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            cli.sendPlayerCommand("button jump_rew");
            return true;
        }

        @Override
        public boolean toggleShuffle() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist shuffle");
            return true;
        }

        @Override
        public boolean toggleRepeat() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist repeat");
            return true;
        }

        @Override
        public boolean playlistControl(String cmd, String tag, String itemId) {
            if (!isConnected()) {
                return false;
            }

            cli.sendPlayerCommand("playlistcontrol cmd:" + cmd + " " + tag + ":" + itemId);
            return true;
        }

        @Override
        public boolean randomPlay(String type) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("randomplay " + type);
            return true;
        }

        /**
         * Start playing the song in the current playlist at the given index.
         *
         * @param index the index to jump to
         */
        @Override
        public boolean playlistIndex(int index) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist index " + index + fadeInSecs());
            return true;
        }

        @Override
        public boolean playlistRemove(int index) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist delete " + index);
            return true;
        }

        @Override
        public boolean playlistMove(int fromIndex, int toIndex) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist move " + fromIndex + " " + toIndex);
            return true;
        }

        @Override
        public boolean playlistClear() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist clear");
            return true;
        }

        @Override
        public boolean playlistSave(String name) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist save " + Util.encode(name));
            return true;
        }

        @Override
        public boolean pluginPlaylistControl(Plugin plugin, String cmd, String itemId) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand(plugin.getId() + " playlist " + cmd + " item_id:" + itemId);
            return true;

        }

        private boolean isPlaying() {
            return playerState.isPlaying();
        }

        @Override
        public void setActivePlayer(final Player player) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    changeActivePlayer(player);
                }
            });
        }

        @Override
        public Player getActivePlayer() {
            return connectionState.getActivePlayer();
        }

        @Override
        public PlayerState getPlayerState() {
            return playerState;
        }

        @Override
        public String getCurrentPlaylist() {
            return playerState.getCurrentPlaylist();
        }

        @Override
        public String getAlbumArtUrl(String artworkTrackId) {
            return getAbsoluteUrl(artworkTrackIdUrl(artworkTrackId));
        }

        private String artworkTrackIdUrl(String artworkTrackId) {
            return "/music/" + artworkTrackId + "/cover.jpg";
        }

        /**
         * Returns a URL to download a song.
         *
         * @param songId the song ID
         * @return The URL (as a string)
         */
        @Override
        public String getSongDownloadUrl(String songId) {
            return getAbsoluteUrl(songDownloadUrl(songId));
        }

        private String songDownloadUrl(String songId) {
            return "/music/" + songId + "/download";
        }

        @Override
        public String getIconUrl(String icon) {
            return getAbsoluteUrl('/' + icon);
        }

        private String getAbsoluteUrl(String relativeUrl) {
            Integer port = connectionState.getHttpPort();
            if (port == null || port == 0) {
                return "";
            }
            return "http://" + connectionState.getCurrentHost() + ":" + port + relativeUrl;
        }

        @Override
        public boolean setSecondsElapsed(int seconds) {
            if (!isConnected()) {
                return false;
            }
            if (seconds < 0) {
                return false;
            }

            cli.sendPlayerCommand("time " + seconds);

            return true;
        }

        @Override
        public void preferenceChanged(String key) {
            Log.i(TAG, "Preference changed: " + key);
            if (Preferences.KEY_NOTIFY_OF_CONNECTION.equals(key)) {
                updateOngoingNotification();
                return;
            }

            // If the server address changed then disconnect.
            if (Preferences.KEY_SERVERADDR.equals(key)) {
                disconnect();
                return;
            }

            getPreferences();
        }


        /**
         * Clean up callbacks for client
         * @param client hosting callbacks to remove
         */
        @Override
        public void cancelItemListRequests(Object client) {
            cli.cancelClientRequests(client);
        }

        /* Start an async fetch of the SqueezeboxServer's players */
        @Override
        public void players(int start, IServiceItemListCallback<Player> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("players", start, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's albums, which are matching the given parameters */
        @Override
        public void albums(int start, String sortOrder, String searchString,
                Artist artist, Year year, Genre genre, Song song, IServiceItemListCallback<Album> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            parameters.add("tags:" + ALBUMTAGS);
            parameters.add("sort:" + sortOrder);
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            if (artist != null) {
                parameters.add("artist_id:" + artist.getId());
            }
            if (year != null) {
                parameters.add("year:" + year.getId());
            }
            if (genre != null) {
                parameters.add("genre_id:" + genre.getId());
            }
            if (song != null) {
                parameters.add("track_id:" + song.getId());
            }
            cli.requestItems("albums", start, parameters, callback);
        }


        /* Start an async fetch of the SqueezeboxServer's artists */
        @Override
        public void artists(int start, String searchString, Album album,
                Genre genre, IServiceItemListCallback<Artist> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            if (album != null) {
                parameters.add("album_id:" + album.getId());
            }
            if (genre != null) {
                parameters.add("genre_id:" + genre.getId());
            }
            cli.requestItems("artists", start, parameters, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's years */
        @Override
        public void years(int start, IServiceItemListCallback<Year> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("years", start, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's genres */
        @Override
        public void genres(int start, String searchString, IServiceItemListCallback<Genre> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            cli.requestItems("genres", start, parameters, callback);
        }

        /**
         * Starts an async fetch of the contents of a SqueezerboxServer's music
         * folders in the given folderId.
         * <p>
         * folderId may be null, in which case the contents of the root music
         * folder are returned.
         * <p>
         * Results are returned through the given callback.
         *
         * @param start Where in the list of folders to start.
         * @param folderId The folder to view.
         * @param callback Results will be returned through this
         */
        @Override
        public void musicFolders(int start, String folderId, IServiceItemListCallback<MusicFolderItem> callback) {
            if (!isConnected()) {
                return;
            }

            List<String> parameters = new ArrayList<String>();

            if (folderId != null) {
                parameters.add("folder_id:" + folderId);
            }

            cli.requestItems("musicfolder", start, parameters, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's songs */
        @Override
        public void songs(int start, String sortOrder, String searchString, Album album,
                Artist artist, Year year, Genre genre, IServiceItemListCallback<Song> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            parameters.add("tags:" + SONGTAGS);
            parameters.add("sort:" + sortOrder);
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            if (album != null) {
                parameters.add("album_id:" + album.getId());
            }
            if (artist != null) {
                parameters.add("artist_id:" + artist.getId());
            }
            if (year != null) {
                parameters.add("year:" + year.getId());
            }
            if (genre != null) {
                parameters.add("genre_id:" + genre.getId());
            }
            cli.requestItems("songs", start, parameters, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's current playlist */
        @Override
        public void currentPlaylist(int start, IServiceItemListCallback<Song> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestPlayerItems("status", start, Arrays.asList("tags:" + SONGTAGS), callback);
        }

        /* Start an async fetch of the songs of the supplied playlist */
        @Override
        public void playlistSongs(int start, Playlist playlist, IServiceItemListCallback<Song> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("playlists tracks", start,
                    Arrays.asList("playlist_id:" + playlist.getId(), "tags:" + SONGTAGS), callback);
        }

        /* Start an async fetch of the SqueezeboxServer's playlists */
        @Override
        public void playlists(int start, IServiceItemListCallback<Playlist> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("playlists", start, callback);
        }

        @Override
        public void registerPlaylistMaintenanceCallback(
                IServicePlaylistMaintenanceCallback callback) {
            Log.v(TAG, "PlaylistMaintenanceCallback attached.");
            playlistMaintenanceCallback.set(callback);
        }

        @Override
        public void unregisterPlaylistMaintenanceCallback(
                IServicePlaylistMaintenanceCallback callback) {
            Log.v(TAG, "PlaylistMaintenanceCallback detached.");
            playlistMaintenanceCallback.compareAndSet(callback, null);
        }

        @Override
        public boolean playlistsDelete(Playlist playlist) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists delete playlist_id:" + playlist.getId());
            return true;
        }

        @Override
        public boolean playlistsMove(Playlist playlist, int index, int toindex) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists edit cmd:move playlist_id:" + playlist.getId()
                    + " index:" + index + " toindex:" + toindex);
            return true;
        }

        @Override
        public boolean playlistsNew(String name) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists new name:" + Util.encode(name));
            return true;
        }

        @Override
        public boolean playlistsRemove(Playlist playlist, int index) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists edit cmd:delete playlist_id:" + playlist.getId() + " index:"
                    + index);
            return true;
        }

        @Override
        public boolean playlistsRename(Playlist playlist, String newname) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand(
                    "playlists rename playlist_id:" + playlist.getId() + " dry_run:1 newname:"
                            + Util.encode(newname));
            return true;
        }

        /* Start an asynchronous search of the SqueezeboxServer's library */
        @Override
        public void search(int start, String searchString, IServiceItemListCallback itemListCallback) {
            if (!isConnected()) {
                return;
            }

            AlbumViewDialog.AlbumsSortOrder albumSortOrder = AlbumViewDialog.AlbumsSortOrder
                    .valueOf(
                            preferredAlbumSort());

            artists(start, searchString, null, null, itemListCallback);
            albums(start, albumSortOrder.name().replace("__", ""), searchString, null, null, null,
                    null, itemListCallback);
            genres(start, searchString, itemListCallback);
            songs(start, SongViewDialog.SongsSortOrder.title.name(), searchString, null,
                    null, null, null, itemListCallback);
        }

        /* Start an asynchronous fetch of the squeezeservers radio type plugins */
        @Override
        public void radios(int start, IServiceItemListCallback<Plugin> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("radios", start, callback);
        }

        /* Start an asynchronous fetch of the squeezeservers radio application plugins */
        @Override
        public void apps(int start, IServiceItemListCallback<Plugin> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("apps", start, callback);
        }


        /* Start an asynchronous fetch of the squeezeservers items of the given type */
        @Override
        public void pluginItems(int start, Plugin plugin, PluginItem parent, String search, IServiceItemListCallback<PluginItem> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            if (parent != null) {
                parameters.add("item_id:" + parent.getId());
            }
            if (search != null && search.length() > 0) {
                parameters.add("search:" + search);
            }
            cli.requestPlayerItems(plugin.getId() + " items", start, parameters, callback);
        }
    }

}
