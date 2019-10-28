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

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;


public class Plugin extends Item {
    public static final Plugin SETTINGS = new Plugin("settings", "home", "SETTINGS", 1005);
    public static final Plugin ADVANCED_SETTINGS = new Plugin("advancedSettings", "settings", "ADVANCED_SETTINGS", 105, "text_only");

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

    private Plugin(String id, String node, String text, int weight) {
        this(record(id, node, text, weight));

    }

    private Plugin(String id, String node, String text, int weight, String windowStyle) {
        this(record(id, node, text, weight, windowStyle));

    }

    private static Map<String, Object> record(String id, String node, String text, int weight) {
        return record(id, node, text, weight, null);
    }

    private static Map<String, Object> record(String id, String node, String text, int weight, String windowStyle) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        record.put("node", node);
        record.put("name", text);
        record.put("weight", weight);
        if (windowStyle != null) {
            Map<String, Object> window = new HashMap<>();
            window.put("windowStyle", windowStyle);
            record.put("window", window);
        }
        return record;
    }
}
