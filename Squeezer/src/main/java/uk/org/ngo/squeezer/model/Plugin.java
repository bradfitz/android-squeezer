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

package uk.org.ngo.squeezer.model;

import android.os.Parcel;

import java.util.Arrays;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.Item;


public class Plugin extends Item {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public Plugin setName(String name) {
        this.name = name;
        return this;
    }

    private String icon;

    /**
     * @return Relative URL path to an icon for this radio or music service, for example
     * "plugins/Picks/html/images/icon.png"
     */
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    private int iconResource;

    /**
     * @return Icon resource for this plugin if it is embedded in the Squeezer app, or null.
     */
    public int getIconResource() {
        return iconResource;
    }

    private int weight;

    public int getWeight() {
        return weight;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean hasInput() {
        return (input != null);
    }

    public boolean isSelectable() {
        return (isSelectAction() || nextWindow != null);
    }

    public boolean isSelectAction() {
        return (goAction != null && !isGoActionPlayAction());
    }

    public boolean isPlayable() {
        return (playAction() != null);
    }

    public Action playAction() {
        if (playAction != null) {
            return playAction;
        }
        return (isGoActionPlayAction() ? goAction : null);
    }

    public boolean hasContextMenu() {
        return (playAction() != null || addAction != null || insertAction != null || moreAction != null);
    }

    public boolean hasSlimContextMenu() {
        return (moreAction != null && moreAction.isContextMenu());
    }

    /** We preferably won't play when item is selected, so attempt to avoid that */
    private boolean isGoActionPlayAction() {
        if (goAction == null) {
            return false;
        }
        if (playAction != null) {
            // goAction and playAction performs the same action
            return sameAction(goAction.action, playAction.action);
        } else {
            // goAction plays
            return "playlistcontrol".equals(goAction.action.cmd[0]) ||
                    (goAction.action.nextWindow != null && Action.NextWindowEnum.nowPlaying == goAction.action.nextWindow.nextWindow);
        }
    }

    private boolean sameAction(Action.JsonAction action1, Action.JsonAction action2) {
        if (!Arrays.equals(action1.cmd, action2.cmd)) return false;
        return action1.params.equals(action2.params);
    }

    public Plugin(Map<String, Object> record) {
        super(record);

        setId(getString(record, record.containsKey("cmd") ? "cmd" : "id"));
        name = getString(record, record.containsKey("name") ? "name" : "text");
        type = getString(record, "type");
        icon = getString(record, record.containsKey("icon") ? "icon" : "icon-id");
        weight = getInt(record, "weight");
    }

    public static final Creator<Plugin> CREATOR = new Creator<Plugin>() {
        @Override
        public Plugin[] newArray(int size) {
            return new Plugin[size];
        }

        @Override
        public Plugin createFromParcel(Parcel source) {
            return new Plugin(source);
        }
    };

    private Plugin(Parcel source) {
        super(source);
        name = source.readString();
        type = source.readString();
        icon = source.readString();
        iconResource = source.readInt();
        weight = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(icon);
        dest.writeInt(iconResource);
        dest.writeInt(weight);
    }

    @Override
    public String toStringOpen() {
        return super.toStringOpen()
                + ", type: " + getType()
                + ", weight: " + getWeight()
                + ", go: " + goAction
                + ", play: " + playAction
                + ", add: " + addAction
                + ", insert: " + insertAction
                + ", more: " + moreAction
                + ", window: " + window;
    }

}
