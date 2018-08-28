/*
 * Copyright (c) 2018 Kurt Aaholst <kaaholst@gmail.com>
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

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.Util;

/**
 * Represents base fields as defined in:
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cbase_fields.3E
 *
 * @author Kurt Aaholst
 */
public class BaseItem implements Parcelable {

    public Window window;
    public Action goAction;
    public Action playAction;
    public Action addAction;
    public Action insertAction;
    public Action moreAction;

    public BaseItem(Map<String, Object> record) {
        window = extractWindow(getRecord(record, "window"));
        Map<String, Object> actionsRecord = getRecord(record, "actions");
        goAction = extractAction("go", null, actionsRecord, record, null);
        playAction = extractAction("play", null, actionsRecord, record, null);
        addAction = extractAction("add", null, actionsRecord, record, null);
        insertAction = extractAction("add-hold", null, actionsRecord, record, null);
        moreAction = extractAction("more", null, actionsRecord, record, null);
        if (moreAction != null) {
            moreAction.action.params.put("xmlBrowseInterimCM", 1);
        }
    }

    public BaseItem(Parcel source) {
        window = Window.readFromParcel(source);
        goAction = source.readParcelable(BaseItem.class.getClassLoader());
        playAction = source.readParcelable(BaseItem.class.getClassLoader());
        addAction = source.readParcelable(BaseItem.class.getClassLoader());
        insertAction = source.readParcelable(BaseItem.class.getClassLoader());
        moreAction = source.readParcelable(BaseItem.class.getClassLoader());
    }

    public static final Creator<BaseItem> CREATOR = new Creator<BaseItem>() {
        @Override
        public BaseItem createFromParcel(Parcel in) {
            return new BaseItem(in);
        }

        @Override
        public BaseItem[] newArray(int size) {
            return new BaseItem[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Window.writeToParcel(dest, window);
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
    public String toString() {
        return "BaseItem{" +
                "window=" + window +
                ", goAction=" + goAction +
                ", playAction=" + playAction +
                ", addAction=" + addAction +
                ", insertAction=" + insertAction +
                ", moreAction=" + moreAction +
                '}';
    }

    private Map<String, Object> getRecord(Map<String, Object> record, String recordName) {
        return (Map<String, Object>) record.get(recordName);
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


    private Window extractWindow(Map<String, Object> record) {
        if (record == null) return null;

        Window window = new Window();
        window.windowId = getString(record, "windowId");
        window.text = getString(record, "text");
        window.textarea = getString(record, "textarea");
        window.textareaToken = getString(record, "textAreaToken");
        window.help = getString(record, "help");
        window.icon = getString(record, record.containsKey("icon") ? "icon" : "icon-id");
        window.menuStyle = getString(record, "menuStyle");
        window.titleStyle = getString(record, "titleStyle");

        return window;
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
        if (action.nextWindow == null) action.nextWindow = Action.NextWindow.fromString(getString(record, "nextWindow"));
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

}
