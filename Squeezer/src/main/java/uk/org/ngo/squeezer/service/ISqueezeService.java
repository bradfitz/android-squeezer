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

import android.os.RemoteException;

import uk.org.ngo.squeezer.IServiceCallback;
import uk.org.ngo.squeezer.IServiceMusicChangedCallback;
import uk.org.ngo.squeezer.IServiceHandshakeCallback;
import uk.org.ngo.squeezer.IServiceVolumeCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlayerListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceAlbumListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceArtistListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceCurrentPlaylistCallback;
import uk.org.ngo.squeezer.itemlist.IServiceYearListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceGenreListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceMusicFolderListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceSongListCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlaylistsCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlaylistMaintenanceCallback;
import uk.org.ngo.squeezer.itemlist.IServicePluginListCallback;
import uk.org.ngo.squeezer.itemlist.IServicePluginItemListCallback;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;

public interface ISqueezeService {
    // For the activity to get callbacks on interesting events
    void registerCallback(IServiceCallback callback);
    void unregisterCallback(IServiceCallback callback);

    // For the activity to get callback when the current playlist is modified
    void registerCurrentPlaylistCallback(IServiceCurrentPlaylistCallback callback);
    void unregisterCurrentPlaylistCallback(IServiceCurrentPlaylistCallback callback);

    // For the activity to get callback when music changes
    void registerMusicChangedCallback(IServiceMusicChangedCallback callback);
    void unregisterMusicChangedCallback(IServiceMusicChangedCallback callback);

    // For the activity to get callback when handshake completes
    void registerHandshakeCallback(IServiceHandshakeCallback callback);
    void unregisterHandshakeCallback(IServiceHandshakeCallback callback);

    // For the activity to get callback when the volume changes.
    void registerVolumeCallback(IServiceVolumeCallback callback);
    void unregisterVolumeCallback(IServiceVolumeCallback callback);

    // Instructing the service to connect to the SqueezeCenter server:
    // hostPort is the port of the CLI interface.
    void startConnect(String hostPort, String userName, String password);
    void disconnect();
    boolean isConnected();
    boolean isConnectInProgress();

    // For the SettingsActivity to notify the Service that a setting changed.
    void preferenceChanged(String key);

    // Call this to change the player we are controlling
    void setActivePlayer(Player player);

    // Returns the player we are currently controlling
    Player getActivePlayer();

    ////////////////////
    // Depends on active player:

    boolean canPowerOn();
    boolean canPowerOff();
    boolean powerOn();
    boolean powerOff();
    boolean canMusicfolder();
    boolean canRandomplay();
    String preferredAlbumSort();
    boolean togglePausePlay();
    boolean play();
    boolean stop();
    boolean nextTrack();
    boolean previousTrack();
    boolean toggleShuffle();
    boolean toggleRepeat();
    boolean playlistControl(String cmd, String className, String id);
    boolean randomPlay(String type);
    boolean playlistIndex(int index);
    boolean playlistRemove(int index);
    boolean playlistMove(int fromIndex, int toIndex);
    boolean playlistClear();
    boolean playlistSave(String name);
    boolean pluginPlaylistControl(Plugin plugin, String cmd, String id);

    boolean setSecondsElapsed(int seconds);

    PlayerState getPlayerState();
    String getCurrentPlaylist();
    String getAlbumArtUrl(String artworkTrackId);
    String getIconUrl(String icon);

    String getSongDownloadUrl(String songTrackId);

    /**
     * Sets the volume to the absolute volume in newVolume, which will be clamped to the
     * interval [0, 100].
     *
     * @param newVolume
     */
    void adjustVolumeTo(int newVolume);
    void adjustVolumeBy(int delta);

    // Player list
    boolean players(int start);
    void registerPlayerListCallback(IServicePlayerListCallback callback);
    void unregisterPlayerListCallback(IServicePlayerListCallback callback);

    // Album list
    /**
     * Starts an asynchronous fetch of album data from the server.  Any
     * callback registered with {@link #registerAlbumListCallback(IServiceAlbumListCallback)} will
     * be called when the data is fetched.
     *
     * @param start
     * @param sortOrder
     * @param searchString
     * @param artist
     * @param year
     * @param genre
     */
    boolean albums(int start, String sortOrder, String searchString, Artist artist, Year year, Genre genre, Song song);
    void registerAlbumListCallback(IServiceAlbumListCallback callback);
    void unregisterAlbumListCallback(IServiceAlbumListCallback callback);

    // Artist list
    boolean artists(int start, String searchString, Album album, Genre genre);
    void registerArtistListCallback(IServiceArtistListCallback callback);
    void unregisterArtistListCallback(IServiceArtistListCallback callback);

    // Year list
    boolean years(int start);
    void registerYearListCallback(IServiceYearListCallback callback);
    void unregisterYearListCallback(IServiceYearListCallback callback);

    // Genre list
    boolean genres(int start, String searchString);
    void registerGenreListCallback(IServiceGenreListCallback callback);
    void unregisterGenreListCallback(IServiceGenreListCallback callback);

    // MusicFolder list
    boolean musicFolders(int start, String folderId);
    void registerMusicFolderListCallback(IServiceMusicFolderListCallback callback);
    void unregisterMusicFolderListCallback(IServiceMusicFolderListCallback callback);

    // Song list
    boolean songs(int start, String sortOrder, String searchString, Album album, Artist artist, Year year, Genre genre);
    boolean currentPlaylist(int start);
    boolean playlistSongs(int start, Playlist playlist);
    void registerSongListCallback(IServiceSongListCallback callback);
    void unregisterSongListCallback(IServiceSongListCallback callback);

    // Playlists
    boolean playlists(int start);
    void registerPlaylistsCallback(IServicePlaylistsCallback callback);
    void unregisterPlaylistsCallback(IServicePlaylistsCallback callback);

    // Named playlist maintenance
    void registerPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback);
    void unregisterPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback);
    boolean playlistsNew(String name);
    boolean playlistsRename(Playlist playlist, String newname);
    boolean playlistsDelete(Playlist playlist);
    boolean playlistsMove(Playlist playlist, int index, int toindex);
    boolean playlistsRemove(Playlist playlist, int index);

    // Search
    boolean search(int start, String searchString);

    // Radios/plugins
    boolean radios(int start);
    boolean apps(int start);
    void registerPluginListCallback(IServicePluginListCallback callback);
    void unregisterPluginListCallback(IServicePluginListCallback callback);

    boolean pluginItems(int start, Plugin plugin, PluginItem parent, String search);
    void registerPluginItemListCallback(IServicePluginItemListCallback callback);
    void unregisterPluginItemListCallback(IServicePluginItemListCallback callback);
}
