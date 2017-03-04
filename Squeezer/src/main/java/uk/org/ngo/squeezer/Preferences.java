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

package uk.org.ngo.squeezer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import uk.org.ngo.squeezer.download.DownloadFilenameStructure;
import uk.org.ngo.squeezer.download.DownloadPathStructure;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.action.PlayableItemAction;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.itemlist.dialog.SongViewDialog;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Song;

public final class Preferences {

    public static final String NAME = "Squeezer";

    // e.g. "10.0.0.81:9090"
    public static final String KEY_SERVER_ADDRESS = "squeezer.serveraddr";

    // Optional Squeezebox Server name
    private static final String KEY_SERVER_NAME = "squeezer.server_name";

    // Optional Squeezebox Server user name
    private static final String KEY_USERNAME = "squeezer.username";

    // Optional Squeezebox Server password
    private static final String KEY_PASSWORD = "squeezer.password";

    // The playerId that we were last connected to. e.g. "00:04:20:17:04:7f"
    public static final String KEY_LAST_PLAYER = "squeezer.lastplayer";

    // Do we automatically try and connect on WiFi availability?
    public static final String KEY_AUTO_CONNECT = "squeezer.autoconnect";

    // Do we keep the notification going at top, even when we're not connected?
    // Deprecated, retained for compatibility when upgrading.
    public static final String KEY_NOTIFY_OF_CONNECTION = "squeezer.notifyofconnection";

    // Type of notification to show.
    public static final String KEY_NOTIFICATION_TYPE = "squeezer.notification_type";

    @StringDef({NOTIFICATION_TYPE_NONE, NOTIFICATION_TYPE_PLAYING, NOTIFICATION_TYPE_ALWAYS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NotificationType {}
    public static final String NOTIFICATION_TYPE_NONE = "none";
    public static final String NOTIFICATION_TYPE_PLAYING = "playing";
    public static final String NOTIFICATION_TYPE_ALWAYS = "always";

    // Do we scrobble track information?
    // Deprecated, retained for compatibility when upgrading. Was an int, of
    // either 0 == No scrobbling, 1 == use ScrobbleDroid API, 2 == use SLS API
    public static final String KEY_SCROBBLE = "squeezer.scrobble";

    // Do we scrobble track information (if a scrobble service is available)?
    //
    // Type of underlying preference is bool / CheckBox
    public static final String KEY_SCROBBLE_ENABLED = "squeezer.scrobble.enabled";

    // Do we send anonymous usage statistics?
    public static final String KEY_ANALYTICS_ENABLED = "squeezer.analytics.enabled";

    // Fade-in period? (0 = disable fade-in)
    public static final String KEY_FADE_IN_SECS = "squeezer.fadeInSecs";

    // What do to when an album is selected in the list view
    protected static final String KEY_ON_SELECT_ALBUM_ACTION = "squeezer.action.onselect.album";

    // What do to when a song is selected in the list view
    protected static final String KEY_ON_SELECT_SONG_ACTION = "squeezer.action.onselect.song";

    // Preferred album list layout.
    private static final String KEY_ALBUM_LIST_LAYOUT = "squeezer.album.list.layout";

    // Preferred song list layout.
    private static final String KEY_SONG_LIST_LAYOUT = "squeezer.song.list.layout";

    // Start SqueezePlayer automatically if installed.
    public static final String KEY_SQUEEZEPLAYER_ENABLED = "squeezer.squeezeplayer.enabled";

    // Preferred UI theme.
    static final String KEY_ON_THEME_SELECT_ACTION = "squeezer.theme";

    // Download category
    static final String KEY_DOWNLOAD_CATEGORY = "squeezer.download.category";

    // Download folder
    static final String KEY_DOWNLOAD_USE_SERVER_PATH = "squeezer.download.use_server_path";

    // Download path structure
    static final String KEY_DOWNLOAD_PATH_STRUCTURE = "squeezer.download.path_structure";

    // Download filename structure
    static final String KEY_DOWNLOAD_FILENAME_STRUCTURE = "squeezer.download.filename_structure";

    // Use SD-card (getExternalMediaDirs)
    static final String KEY_DOWNLOAD_USE_SD_CARD_SCREEN = "squeezer.download.use_sd_card.screen";
    static final String KEY_DOWNLOAD_USE_SD_CARD = "squeezer.download.use_sd_card";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public Preferences(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
    }

    public Preferences(Context context, SharedPreferences sharedPreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
    }

    private String getStringPreference(String preference, String defaultValue) {
        final String pref = sharedPreferences.getString(preference, null);
        if (pref == null || pref.length() == 0) {
            return defaultValue;
        }
        return pref;
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public ServerAddress getServerAddress() {
        ServerAddress serverAddress = new ServerAddress();

        WifiManager mWifiManager = (WifiManager) context
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        serverAddress.bssId = (connectionInfo != null ? connectionInfo.getBSSID() : null);
        if (serverAddress.bssId != null)
            serverAddress.address = getStringPreference(KEY_SERVER_ADDRESS + "_" + serverAddress.bssId, null);
        if (serverAddress.address == null)
            serverAddress.address = getStringPreference(KEY_SERVER_ADDRESS, null);

        return serverAddress;
    }

    public static class ServerAddress {
        public String bssId;
        public String address; // <host name or ip>:<port>

        @Override
        public String toString() {
            return (bssId != null ? bssId + "_ " : "") + address + "_";
        }
    }

    public ServerAddress saveServerAddress(String address) {
        WifiManager mWifiManager = (WifiManager) context
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        String bssId = (connectionInfo != null ? connectionInfo.getBSSID() : null);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(bssId != null ? KEY_SERVER_ADDRESS + "_" + bssId : KEY_SERVER_ADDRESS, address);
        editor.commit();

        ServerAddress serverAddress = new ServerAddress();
        serverAddress.bssId = bssId;
        serverAddress.address = address;

        return serverAddress;
    }

    public String getServerName() {
        return getServerName(getServerAddress());
    }

    public String getServerName(ServerAddress serverAddress) {
        String serverName = getStringPreference(serverAddress + KEY_SERVER_NAME, null);
        return serverName != null ? serverName : serverAddress.address;
    }

    public void saveServerName(ServerAddress serverAddress, String serverName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(serverAddress + KEY_SERVER_NAME, serverName);
        editor.commit();
    }

    public String getUserName(ServerAddress serverAddress) {
        return getUserName(serverAddress, null);
    }

    public String getUserName(ServerAddress serverAddress, String defaultValue) {
        return getStringPreference(serverAddress + KEY_USERNAME, defaultValue);
    }

    public String getPassword(ServerAddress serverAddress) {
        return getPassword(serverAddress, null);
    }

    public String getPassword(ServerAddress serverAddress, String defaultValue) {
        return getStringPreference(serverAddress + KEY_PASSWORD, defaultValue);
    }

    public void saveUserCredentials(ServerAddress serverAddress, String userName, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(serverAddress + KEY_USERNAME, userName);
        editor.putString(serverAddress + KEY_PASSWORD, password);
        editor.commit();
    }

    public String getTheme() {
        return getStringPreference(KEY_ON_THEME_SELECT_ACTION, null);
    }

    public boolean isAutoConnect() {
        return sharedPreferences.getBoolean(KEY_AUTO_CONNECT, true);
    }

    public boolean controlSqueezePlayer() {
        return sharedPreferences.getBoolean(KEY_SQUEEZEPLAYER_ENABLED, true);
    }

    public PlayableItemAction.Type getOnItemSelectAction(Class<? extends PlaylistItem> clazz) {
        final String actionName = sharedPreferences.getString(getOnSelectItemActionKey(clazz), PlayableItemAction.Type.NONE.name());
        try {
            return PlayableItemAction.Type.valueOf(actionName);
        } catch (IllegalArgumentException e) {
            return PlayableItemAction.Type.NONE;
        }
    }

    public void setOnSelectItemAction(Class<? extends PlaylistItem> clazz, PlayableItemAction.Type action) {
        String key = getOnSelectItemActionKey(clazz);

        if (action != null) {
            sharedPreferences.edit().putString(key, action.name()).commit();
        } else {
            sharedPreferences.edit().remove(key).commit();
        }
    }

    private String getOnSelectItemActionKey(Class<? extends PlaylistItem> clazz) {
        String key = null;
        if (clazz == Song.class) key = KEY_ON_SELECT_SONG_ACTION; else
        if (clazz == MusicFolderItem.class) key = KEY_ON_SELECT_SONG_ACTION; else
        if (clazz == Album.class) key = KEY_ON_SELECT_ALBUM_ACTION;
        if (key == null) {
            throw new IllegalArgumentException("Default action for class '" + clazz + " is not supported");
        }
        return key;
    }

    public AlbumViewDialog.AlbumListLayout getAlbumListLayout() {
        String listLayoutString = sharedPreferences.getString(Preferences.KEY_ALBUM_LIST_LAYOUT, null);
        if (listLayoutString == null) {
            int screenSize = context.getResources().getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK;
            return (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                    ? AlbumViewDialog.AlbumListLayout.grid : AlbumViewDialog.AlbumListLayout.list;
        } else {
            return AlbumViewDialog.AlbumListLayout.valueOf(listLayoutString);
        }
    }

    public void setAlbumListLayout(AlbumViewDialog.AlbumListLayout albumListLayout) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Preferences.KEY_ALBUM_LIST_LAYOUT, albumListLayout.name());
        editor.commit();
    }

    public SongViewDialog.SongListLayout getSongListLayout() {
        String listLayoutString = sharedPreferences.getString(Preferences.KEY_SONG_LIST_LAYOUT, null);
        if (listLayoutString != null) {
            return SongViewDialog.SongListLayout.valueOf(listLayoutString);
        }
        return SongViewDialog.SongListLayout.list;
    }

    public void setSongListLayout(SongViewDialog.SongListLayout songListLayout) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Preferences.KEY_SONG_LIST_LAYOUT, songListLayout.name());
        editor.commit();
    }

    public boolean isDownloadUseServerPath() {
        return sharedPreferences.getBoolean(KEY_DOWNLOAD_USE_SERVER_PATH, true);
    }

    public DownloadPathStructure getDownloadPathStructure() {
        final String string = sharedPreferences.getString(KEY_DOWNLOAD_PATH_STRUCTURE, null);
        return (string == null ? DownloadPathStructure.ARTIST_ALBUM: DownloadPathStructure.valueOf(string));
    }

    public DownloadFilenameStructure getDownloadFilenameStructure() {
        final String string = sharedPreferences.getString(KEY_DOWNLOAD_FILENAME_STRUCTURE, null);
        return (string == null ? DownloadFilenameStructure.NUMBER_TITLE: DownloadFilenameStructure.valueOf(string));
    }

    public boolean isDownloadUseSdCard() {
        return sharedPreferences.getBoolean(KEY_DOWNLOAD_USE_SD_CARD, false);
    }
}
