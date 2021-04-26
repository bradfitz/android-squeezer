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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.model.Action;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;

public interface ISqueezeService {
    /**
     * @return the EventBus the activity posts events to.
     */
    @NonNull EventBus getEventBus();

    // Instructing the service to connect to the SqueezeCenter server:
    // hostPort is the port of the CLI interface.
    void startConnect();
    void disconnect();
    boolean isConnected();
    boolean isConnectInProgress();

    /** Initiate the flow to register the controller with the server */
    void register(IServiceItemListCallback<JiveItem> callback);

    // For the SettingsActivity to notify the Service that a setting changed.
    void preferenceChanged(String key);

    // Call this to change the player we are controlling
    void setActivePlayer(@NonNull Player player);

    // Returns the player we are currently controlling
    @Nullable
    Player getActivePlayer();

    /**
     * @return players that the server knows about (irrespective of power, connection, or
     * other status).
     */
    Collection<Player> getPlayers();

    // XXX: Delete, now that PlayerState is tracked in the player?
    PlayerState getActivePlayerState();

    // Player control
    void togglePower(Player player);
    void playerRename(Player player, String newName);
    void sleep(Player player, int duration);
    void playerPref(@Player.Pref.Name String playerPref);
    void playerPref(@Player.Pref.Name String playerPref, String value);
    void playerPref(Player player, @Player.Pref.Name String playerPref, String value);

    /**
     * Synchronises the slave player to the player with masterId.
     *
     * @param player the player to sync.
     * @param masterId ID of the player to sync to.
     */
    void syncPlayerToPlayer(@NonNull Player player, @NonNull String masterId);

    /**
     * Removes the player with playerId from any sync groups.
     *
     * @param player the player to be removed from sync groups.
     */
    void unsyncPlayer(@NonNull Player player);

    ////////////////////
    // Depends on active player:

    String getServerVersion() throws SqueezeService.HandshakeNotCompleteException;
    boolean togglePausePlay();
    boolean togglePausePlay(Player player);
    boolean play();
    boolean pause();
    boolean stop();
    boolean nextTrack();
    boolean nextTrack(Player player);
    boolean previousTrack();
    boolean previousTrack(Player player);
    boolean toggleShuffle();
    boolean toggleRepeat();
    boolean playlistIndex(int index);
    boolean playlistRemove(int index);
    boolean playlistMove(int fromIndex, int toIndex);
    boolean playlistClear();
    boolean playlistSave(String name);
    boolean button(Player player, IRButton button);

    boolean setSecondsElapsed(int seconds);

    PlayerState getPlayerState();
    String getCurrentPlaylist();

    /**
     * Sets the volume to the absolute volume in newVolume, which will be clamped to the
     * interval [0, 100].
     */
    void adjustVolumeTo(Player player, int newVolume);
    void adjustVolumeTo(int newVolume);
    void adjustVolumeBy(int delta);

    /** Cancel any pending callbacks for client */
    void cancelItemListRequests(Object client);

    /** Alarm list */
    void alarms(int start, IServiceItemListCallback<Alarm> callback);

    /** Alarm playlists */
    void alarmPlaylists(IServiceItemListCallback<AlarmPlaylist> callback);

    /** Alarm maintenance */
    void alarmAdd(int time);
    void alarmDelete(String id);
    void alarmSetTime(String id, int time);
    void alarmAddDay(String id, int day);
    void alarmRemoveDay(String id, int day);
    void alarmEnable(String id, boolean enabled);
    void alarmRepeat(String id, boolean repeat);
    void alarmSetPlaylist(String id, AlarmPlaylist playlist);


    // Plugins (Radios/Apps (music services)/Favorites)
    void pluginItems(int start, String cmd, IServiceItemListCallback<JiveItem>  callback) throws SqueezeService.HandshakeNotCompleteException;

    /**
     * Start an asynchronous fetch of the squeezeservers generic menu items.
     * <p>
     * See http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#Go_Do.2C_On_and_Off_actions"
     *
     * @param start Offset of the first item to fetch. Paging parameters are added automatically.
     * @param item Current SBS item with the <code>action</code>, and which may contain parameters for the action.
     * @param action <code>go</code> action from SBS. "go" refers to a command that opens a new window (i.e. returns results to browse)
     * @param callback This will be called as the items arrive.
     * @throws SqueezeService.HandshakeNotCompleteException if this is called before handshake is complete
     */
    void pluginItems(int start, JiveItem item, Action action, IServiceItemListCallback<JiveItem> callback) throws SqueezeService.HandshakeNotCompleteException;

    /**
     * Start an asynchronous fetch of the squeezeservers generic menu items with no paging nor extra parameters.
     * <p>
     * See http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#Go_Do.2C_On_and_Off_actions"
     *
     * @param action <code>go</code> action from SBS. "go" refers to a command that opens a new window (i.e. returns results to browse)
     * @param callback This will be called as the items arrive.
     * @throws SqueezeService.HandshakeNotCompleteException if this is called before handshake is complete
     */
    void pluginItems(Action action, IServiceItemListCallback<JiveItem> callback) throws SqueezeService.HandshakeNotCompleteException;

    /**
     * Perform the supplied SBS <code>do</code> <code>action</code> using parameters in <code>item</code>.
     * <p>
     * See http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#Go_Do.2C_On_and_Off_actions"
     *
     * @param item Current SBS item with the <code>action</code>, and which may contain parameters for the action.
     * @param action <code>do</code> action from SBS. "do" refers to an action to perform that does not return browsable data.
     */
    void action(JiveItem item, Action action);

    /**
     * Perform the supplied SBS <code>do</code> <code>action</code>
     * <p>
     * See http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#Go_Do.2C_On_and_Off_actions"
     *
     * @param action <code>do</code> action from SBS. "do" refers to an action to perform that does not return browsable data.
     */
    void action(Action.JsonAction action);

    /**
     * Find the specified player
     * @param playerId id of the player to find
     * @return
     */
    Player getPlayer(String playerId) throws PlayerNotFoundException;
    /**
     * Initiate download of songs for the supplied item.
     *
     * @param item Song or item with songs to download
     */
    void downloadItem(JiveItem item) throws SqueezeService.HandshakeNotCompleteException;

}
