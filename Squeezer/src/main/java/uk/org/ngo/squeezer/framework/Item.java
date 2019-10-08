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

package uk.org.ngo.squeezer.framework;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.Plugin;

/**
 * Base class for SqueezeServer data. Specializations must implement all the necessary boilerplate
 * code. This is okay for now, because we only have few data types.
 *
 * @author Kurt Aaholst
 */
public abstract class Item implements Parcelable {
    private String id;
    @NonNull private String name;
    @NonNull private Uri icon;
    private String node;
    private int weight;

    public Action.NextWindow nextWindow;
    public Input input;
    public String inputValue;
    public Window window;
    public boolean doAction;
    public Action goAction;
    public Action playAction;
    public Action addAction;
    public Action insertAction;
    public Action moreAction;
    public List<Plugin> subItems;
    public boolean showBigArtwork;

    public Item() {
        name = "";
        icon = Uri.EMPTY;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public Item setName(@NonNull String name) {
        this.name = name;
        return this;
    }

    /**
     * @return Relative URL path to an icon for this radio or music service, for example
     * "plugins/Picks/html/images/icon.png"
     */
    @NonNull
    public Uri getIcon() {
        return icon;
    }

    /**
     * @return Whether the song has artwork associated with it.
     */
    public boolean hasArtwork() {
        return ! (getIcon().equals(Uri.EMPTY));
    }

    /**
     * @return Icon resource for this plugin if it is embedded in the Squeezer app, or an empty icon.
     */
    public int getIconResource() {
        if ("myMusic".equals(id)) {
            return R.drawable.ic_my_music;
        }
        if ("radio".equals(id)) {
            return R.drawable.ic_internet_radio;
        }
        if ("radios".equals(id)) {
            return R.drawable.ic_internet_radio;
        }
        if ("favorites".equals(id)) {
            return R.drawable.ic_favorites;
        }
        if ("globalSearch".equals(id)) {
            return R.drawable.ic_search;
        }
        if ("homeSearchRecent".equals(id)) {
            return R.drawable.ic_search;
        }
        if ("myApps".equals(id)) {
            return R.drawable.ic_my_apps;
        }
        if ("opmlmyapps".equals(id)) {
            return R.drawable.ic_my_apps;
        }
        if ("opmlappgallery".equals(id)) {
            return R.drawable.ic_app_gallery;
        }
        if ("playerpower".equals(id)) {
            return R.drawable.ic_power;
        }
        if ("myMusicArtists".equals(id)) {
            return R.drawable.ic_artists;
        }
        if ("myMusicAlbums".equals(id)) {
            return R.drawable.ic_albums;
        }
        if ("myMusicGenres".equals(id)) {
            return R.drawable.ic_genres;
        }
        if ("myMusicYears".equals(id)) {
            return R.drawable.ic_years;
        }
        if ("myMusicNewMusic".equals(id)) {
            return R.drawable.ic_new_music;
        }
        if ("myMusicPlaylists".equals(id)) {
            return R.drawable.ic_playlists;
        }
        if (id != null && id.startsWith("myMusicSearch")) {
            return R.drawable.ic_search;
        }
        if ("myMusicMusicFolder".equals(id)) {
            return R.drawable.ic_music_folder;
        }
        if ("randomplay".equals(id)) {
            return R.drawable.ic_random;
        }
        if ("opmlselectVirtualLibrary".equals(id)) {
            return R.drawable.ic_ml_other_library;
        }
        if ("opmlselectRemoteLibrary".equals(id)) {
            return R.drawable.ic_my_music;
        }
        return R.drawable.icon_pending_artwork;
    }


    public String getNode() {
        return node;
    }

    public int getWeight() {
        return weight;
    }


    public boolean isSelectable() {
        return (goAction != null || hasSubItems()|| node != null);
    }

    public boolean hasContextMenu() {
        return (playAction != null || addAction != null || insertAction != null || moreAction != null);
    }


    public Item(Map<String, Object> record) {
        setId(getString(record, record.containsKey("cmd") ? "cmd" : "id"));
        name = getStringOrEmpty(record, record.containsKey("name") ? "name" : "text");
        icon = getImageUrl(record, record.containsKey("icon-id") ? "icon-id" : "icon");
        node = getString(record, "node");
        weight = getInt(record, "weight");
        Map<String, Object> baseRecord = getRecord(record, "base");
        Map<String, Object> baseActions = (baseRecord != null ? getRecord(baseRecord, "actions") : null);
        Map<String, Object> baseWindow = (baseRecord != null ? getRecord(baseRecord, "window") : null);
        Map<String, Object> actionsRecord = getRecord(record, "actions");
        nextWindow = Action.NextWindow.fromString(getString(record, "nextWindow"));
        input = extractInput(getRecord(record, "input"));
        window = extractWindow(getRecord(record, "window"), baseWindow);

        // do takes precedence over go
        goAction = extractAction("do", baseActions, actionsRecord, record, baseRecord);
        doAction = (goAction != null);
        if (goAction == null) {
            goAction = extractAction("go", baseActions, actionsRecord, record, baseRecord);
        }

        playAction = extractAction("play", baseActions, actionsRecord, record, baseRecord);
        addAction = extractAction("add", baseActions, actionsRecord, record, baseRecord);
        insertAction = extractAction("add-hold", baseActions, actionsRecord, record, baseRecord);
        moreAction = extractAction("more", baseActions, actionsRecord, record, baseRecord);
        if (moreAction != null) {
            moreAction.action.params.put("xmlBrowseInterimCM", 1);
        }
        subItems = extractSubItems((Object[]) record.get("item_loop"));
        showBigArtwork = record.containsKey("showBigArtwork");
    }

    public Item(Parcel source) {
        setId(source.readString());
        name = source.readString();
        icon = Uri.parse(source.readString());
        node = source.readString();
        weight = source.readInt();
        nextWindow = Action.NextWindow.fromString(source.readString());
        input = Input.readFromParcel(source);
        window = Window.readFromParcel(source);
        goAction = source.readParcelable(Item.class.getClassLoader());
        playAction = source.readParcelable(Item.class.getClassLoader());
        addAction = source.readParcelable(Item.class.getClassLoader());
        insertAction = source.readParcelable(Item.class.getClassLoader());
        moreAction = source.readParcelable(Item.class.getClassLoader());
        subItems = source.createTypedArrayList(Plugin.CREATOR);
        doAction = (source.readByte() != 0);
        showBigArtwork = (source.readByte() != 0);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
        dest.writeString(icon.toString());
        dest.writeString(node);
        dest.writeInt(weight);
        dest.writeString(nextWindow == null ? null : nextWindow.toString());
        Input.writeToParcel(dest, input);
        Window.writeToParcel(dest, window);
        dest.writeParcelable(goAction, flags);
        dest.writeParcelable(playAction, flags);
        dest.writeParcelable(addAction, flags);
        dest.writeParcelable(insertAction, flags);
        dest.writeParcelable(moreAction, flags);
        dest.writeTypedList(subItems);
        dest.writeByte((byte) (doAction ? 1 : 0));
        dest.writeByte((byte) (showBigArtwork ? 1 : 0));
    }


    public boolean hasInput() {
        return (input != null);
    }

    public boolean isInputReady() {
        return !TextUtils.isEmpty(inputValue);
    }

    public boolean hasSubItems() {
        return (subItems != null);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return (getId() != null ? getId().hashCode() : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (o.getClass() != getClass()) {
            // There is no guarantee that SqueezeServer items have globally unique IDs.
            return false;
        }

        // Both might be empty items. For example a Song initialised
        // with an empty token map, because no song is currently playing.
        if (getId() == null && ((Item) o).getId() == null) {
            return true;
        }

        return getId() != null && getId().equals(((Item) o).getId());
    }

    protected String toStringOpen() {
        return getClass().getSimpleName() + " { id: " + getId()
                + ", name: " + getName()
                + ", node: " + node
                + ", weight: " + getWeight()
                + ", go: " + goAction
                + ", play: " + playAction
                + ", add: " + addAction
                + ", insert: " + insertAction
                + ", more: " + moreAction
                + ", window: " + window;

    }

    @Override
    public String toString() {
        return toStringOpen() + " }";
    }


    private Map<String, Object> getRecord(Map<String, Object> record, String recordName) {
        return Util.getRecord(record, recordName);
    }

    protected int getInt(Map<String, Object> record, String fieldName) {
        return Util.getInt(record, fieldName);
    }

    protected int getInt(Map<String, Object> record, String fieldName, int defaultValue) {
        return Util.getInt(record, fieldName, defaultValue);
    }

    protected static String getString(Map<String, Object> record, String fieldName) {
        return Util.getString(record, fieldName);
    }

    @NonNull
    protected String getStringOrEmpty(Map<String, Object> record, String fieldName) {
        return Util.getStringOrEmpty(record, fieldName);
    }

    @NonNull
    private static Uri getImageUrl(Map<String, Object> record, String fieldName) {
        return Util.getImageUrl(record, fieldName);
    }

    public static Window extractWindow(Map<String, Object> itemWindow, Map<String, Object> baseWindow) {
        if (itemWindow == null && baseWindow == null) return null;

        Map<String, Object> params = new HashMap<>();
        if (baseWindow != null) params.putAll(baseWindow);
        if (itemWindow != null) params.putAll(itemWindow);

        Window window = new Window();
        window.windowId = getString(params, "windowId");
        window.text = getString(params, "text");
        window.textarea = getString(params, "textarea");
        window.textareaToken = getString(params, "textAreaToken");
        window.help = getString(params, "help");
        window.icon = getString(params, params.containsKey("icon") ? "icon" : "icon-id");
        window.titleStyle = getString(params, "titleStyle");
        window.menuStyle = getString(params, "menuStyle");
        window.windowStyle = getString(params, "windowStyle");

        return window;
    }

    private Input extractInput(Map<String, Object> record) {
        if (record == null) return null;

        Input input = new Input();
        input.len = getInt(record, "len");
        input.softbutton1 = getString(record, "softbutton1");
        input.softbutton2 = getString(record, "softbutton2");
        input.inputStyle = getString(record, "inputStyle");
        input.title = getString(record, "title");
        input.allowedChars = getString(record, "allowedChars");
        Map<String, Object> helpRecord = getRecord(record, "help");
        if (helpRecord != null) {
            input.help = new HelpText();
            input.help.text = getString(helpRecord, "text");
            input.help.token = getString(helpRecord, "token");
        }
        return input;
    }

    private Action extractAction(String actionName, Map<String, Object> baseActions, Map<String, Object> itemActions, Map<String, Object> record, Map<String, Object> baseRecord) {
        Map<String, Object> actionsRecord = null;
        Map<String, Object> itemParams = null;

        Object itemAction = (itemActions != null ? itemActions.get(actionName) : null);
        if (itemAction instanceof Map) {
            actionsRecord = (Map<String, Object>) itemAction;
        }
        if (actionsRecord == null && baseActions != null) {
            Map<String, Object> baseAction = getRecord(baseActions, actionName);
            if (baseAction != null) {
                String itemsParams = (String) baseAction.get("itemsParams");
                if (itemsParams != null) {
                    itemParams = getRecord(record, itemsParams);
                    if (itemParams != null) {
                        actionsRecord = baseAction;
                    }
                }
            }
        }
        if (actionsRecord == null) return null;

        Action actionHolder = new Action();
        Action.JsonAction action = actionHolder.action = new Action.JsonAction();

        action.nextWindow = Action.NextWindow.fromString(getString(actionsRecord, "nextWindow"));
        if (action.nextWindow == null) action.nextWindow = nextWindow;
        if (action.nextWindow == null && baseRecord != null) action.nextWindow = Action.NextWindow.fromString(getString(baseRecord, "nextWindow"));

        action.cmd = Util.getStringArray((Object[]) actionsRecord.get("cmd"));
        action.params = new HashMap<>();
        Map<String, Object> params = getRecord(actionsRecord, "params");
        if (params != null) {
            action.params.putAll(params);
        }
        if (itemParams != null) {
            action.params.putAll(itemParams);
        }
        action.params.put("useContextMenu", "1");
        actionHolder.initInputParam();

        return actionHolder;
    }

    private List<Plugin> extractSubItems(Object[] item_loop) {
        if (item_loop != null) {
            List<Plugin> items = new ArrayList<>();
            for (Object item_d : item_loop) {
                Map<String, Object> record = (Map<String, Object>) item_d;
                items.add(new Plugin(record));
            }
            return items;
        }

        return null;
    }

}
