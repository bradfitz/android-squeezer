/*
 * Copyright (c) 2017 Kurt Aaholst <kaaholst@gmail.com>
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
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Map;

import uk.org.ngo.squeezer.Util;

/**
 * Implements <action_fields> of the LMS SqueezePlay interface.
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cactions_fields.3E
 */
public class Action {
    private static final String INPUT_PLACEHOLDER = "__TAGGEDINPUT__";

    public String name;
    public String urlCommand;
    public JsonAction action;

    public void initInputParam() {
        if (action.params.containsValue(INPUT_PLACEHOLDER)) {
            for (Map.Entry<String, Object> entry : action.params.entrySet()) {
                if ("__TAGGEDINPUT__".equals(entry.getValue())) {
                    action.inputParam = entry.getKey();
                    break;
                }
            }
        }
    }

    public boolean isSearchReady() {
        return (action.inputParam != null && !INPUT_PLACEHOLDER.equals(_getInputValue()));
    }

    public String getInputValue() {
        return (INPUT_PLACEHOLDER.equals(_getInputValue()) ? "" : _getInputValue());
    }

    private String _getInputValue() {
        return (action.inputParam != null ? (String) action.params.get(action.inputParam) : null);
    }

    public static Action readFromParcel(Parcel source) {
        if (source.readInt() == 0) return null;

        Action action = new Action();
        action.name = source.readString();
        action.urlCommand = source.readString();
        if (action.urlCommand == null) {
            action.action = new JsonAction();
            action.action.action = source.readString();
            action.action.cmd = source.createStringArray();
            action.action.inputParam = source.readString();
            action.action.params = Util.mapify(source.createStringArray());
        }

        return action;
    }

    public static void writeToParcel(Parcel dest, Action action) {
        dest.writeInt(action == null ? 0 : 1);
        if (action == null) return;

        dest.writeString(action.name);
        dest.writeString(action.urlCommand);
        if (action.urlCommand == null) {
            dest.writeString(action.action.action);
            dest.writeStringArray(action.action.cmd);
            dest.writeString(action.action.inputParam);
            String[] tokens = new String[action.action.params.size()];
            int i = 0;
            for (Map.Entry entry : action.action.params.entrySet()) {
                tokens[i++] = entry.getKey() + ":" + entry.getValue();
            }
            dest.writeStringArray(tokens);
        }
    }

    public static class JsonAction {
        public String action;
        public String[] cmd;
        public String inputParam;
        public Map<String, Object> params;

        @Override
        public String toString() {
            return "JsonAction{" +
                    "action='" + action + '\'' +
                    ", cmd=" + Arrays.toString(cmd) +
                    ", params=" + params +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Action{" +
                "name='" + name + '\'' +
                ", urlCommand='" + urlCommand + '\'' +
                ", action=" + action +
                '}';
    }
}