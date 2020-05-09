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

import androidx.annotation.StringRes;

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.Window;


public class Plugin extends Item {
    public static final Plugin HOME = new Plugin("home", null, R.string.HOME, 1, Window.WindowStyle.HOME_MENU);
    public static final Plugin CURRENT_PLAYLIST = new Plugin("status", null, R.string.menu_item_playlist, 1, Window.WindowStyle.PLAY_LIST);
    public static final Plugin EXTRAS = new Plugin("extras", "home", R.string.EXTRAS, 50, Window.WindowStyle.HOME_MENU);
    public static final Plugin SETTINGS = new Plugin("settings", "home", R.string.SETTINGS, 1005, Window.WindowStyle.HOME_MENU);
    public static final Plugin ADVANCED_SETTINGS = new Plugin("advancedSettings", "settings", R.string.ADVANCED_SETTINGS, 105, Window.WindowStyle.TEXT_ONLY);

    public Plugin(Map<String, Object> record) {
        super(record);
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
    }

    private Plugin(String id, String node, @StringRes int text, int weight, Window.WindowStyle windowStyle) {
        this(record(id, node, text, weight));
        window = new Window();
        window.windowStyle = windowStyle;
    }

    private static Map<String, Object> record(String id, String node, @StringRes int text, int weight) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("node", node);
        record.put("name", Squeezer.getContext().getString(text));
        record.put("weight", weight);

        return record;
    }
}
