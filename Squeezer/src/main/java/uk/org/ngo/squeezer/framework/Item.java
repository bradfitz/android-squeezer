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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.Util;

/**
 * Base class for SqueezeServer data. Specializations must implement all the necessary boilerplate
 * code. This is okay for now, because we only have few data types.
 *
 * @author Kurt Aaholst
 */
public abstract class Item implements Parcelable {
    private static final String TAG = Item.class.getSimpleName();

    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    abstract public String getName();

    public Item() {
    }

    public Window window;
    public Input input;
    public Action goAction;
    public Action playAction;
    public Action addAction;
    public Action insertAction;
    public Action moreAction;

    public Item(Map<String, Object> record) {
        Map<String, Object> baseActions = null;
        Map<String, Object> baseRecord = (Map<String, Object>) record.get("base");
        if (baseRecord != null) {
            baseActions = (Map<String, Object>) baseRecord.get("actions");
        }
        Map<String, Object> actionsRecord = (Map<String, Object>) record.get("actions");
        input = extractInput((Map<String, Object>) record.get("input"));
        goAction = extractAction("go", baseActions, actionsRecord, record, baseRecord);
        playAction = extractAction("play", baseActions, actionsRecord, record, baseRecord);
        addAction = extractAction("add", baseActions, actionsRecord, record, baseRecord);
        insertAction = extractAction("add-hold", baseActions, actionsRecord, record, baseRecord);
        moreAction = extractAction("more", baseActions, actionsRecord, record, baseRecord);
        if (moreAction != null) {
            moreAction.action.params.put("xmlBrowseInterimCM", 1);
        }

        // We preferably won't play when item is selected, so attempt to aviod that
        if (goAction != null) {
            if (playAction != null) {
                if (sameAction(goAction.action, playAction.action)) {
                    // Remove identical goAction
                    goAction = null;
                }
            } else {
                if ("playlistcontrol".equals(goAction.action.cmd[0]) ||
                        (goAction.action.nextWindow != null && Action.NextWindowEnum.nowPlaying == goAction.action.nextWindow.nextWindow)) {
                    // Switch to playAction
                    playAction = goAction;
                    goAction = null;
                }
            }
        }
    }

    public Item(Parcel source) {
        setId(source.readString());
        window = Window.readFromParcel(source);
        input = Input.readFromParcel(source);
        goAction = source.readParcelable(Item.class.getClassLoader());
        playAction = source.readParcelable(Item.class.getClassLoader());
        addAction = source.readParcelable(Item.class.getClassLoader());
        insertAction = source.readParcelable(Item.class.getClassLoader());
        moreAction = source.readParcelable(Item.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        Window.writeToParcel(dest, window);
        Input.writeToParcel(dest, input);
        dest.writeParcelable(goAction, flags);
        dest.writeParcelable(playAction, flags);
        dest.writeParcelable(addAction, flags);
        dest.writeParcelable(insertAction, flags);
        dest.writeParcelable(moreAction, flags);
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
        return getClass().getSimpleName() + " { id: " + getId() + ", name: " + getName();
    }

    @Override
    public String toString() {
        return toStringOpen() + " }";
    }


    protected int getInt(Map<String, Object> record, String fieldName) {
        return Util.getInt(record, fieldName);
    }

    protected int getInt(Map<String, Object> record, String fieldName, int defaultValue) {
        return Util.getInt(record, fieldName, defaultValue);
    }


    protected String getString(Map<String, Object> record, String fieldName) {
        return Util.getString(record, fieldName);
    }

    @NonNull
    protected String getStringOrEmpty(Map<String, Object> record, String fieldName) {
        return Util.getStringOrEmpty(record, fieldName);
    }


    private boolean sameAction(Action.JsonAction action1, Action.JsonAction action2) {
        if (!Arrays.equals(action1.cmd, action2.cmd)) return false;
        return action1.params.equals(action2.params);
    }

    public boolean isAction(Action action1, Action action2) {
        if (action2 == null || action2.action == null) {
            return false;
        }
        if (!Arrays.equals(action1.action.cmd, action2.action.cmd)) return false;
        return action1.action.params.get("item_id").equals(action2.action.params.get("item_id"));
    }

    private Input extractInput(Map<String, Object> inputRecord) {
        if (inputRecord == null) return null;

        Input input = new Input();
        input.len = getInt(inputRecord, "len");
        input.softbutton1 = getString(inputRecord, "softbutton1");
        input.softbutton2 = getString(inputRecord, "softbutton2");
        input.inputStyle = getString(inputRecord, "inputStyle");
        input.allowedChars = getString(inputRecord, "allowedChars");
        Map<String, Object> helpRecord = (Map<String, Object>) inputRecord.get("help");
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
            Map<String, Object> baseAction = (Map<String, Object>) baseActions.get(actionName);
            if (baseAction != null) {
                String itemsParams = (String) baseAction.get("itemsParams");
                if (itemsParams != null) {
                    itemParams = (Map<String, Object>) record.get(itemsParams);
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
        if (action.nextWindow == null) action.nextWindow = Action.NextWindow.fromString(getString(record, "nextWindow"));
        if (action.nextWindow == null && baseRecord != null) action.nextWindow = Action.NextWindow.fromString(getString(baseRecord, "nextWindow"));

        action.cmd = Util.getStringArray((Object[]) actionsRecord.get("cmd"));
        action.params = new HashMap<>();
        Map<String, Object> params = (Map<String, Object>) actionsRecord.get("params");
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

}
