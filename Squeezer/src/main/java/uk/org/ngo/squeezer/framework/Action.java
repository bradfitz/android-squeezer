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

import com.google.common.base.Joiner;

import java.util.Arrays;
import java.util.Map;

import uk.org.ngo.squeezer.Util;

/**
 * Implements <code>action_fields</code> of the LMS SqueezePlay interface.
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cactions_fields.3E
 */
public class Action {
    private static final String INPUT_PLACEHOLDER = "__TAGGEDINPUT__";
    private static final Joiner joiner = Joiner.on(" ");

    public String urlCommand;
    public JsonAction action;
    public String inputParam;

    public void initInputParam() {
        if (action.params.containsValue(INPUT_PLACEHOLDER)) {
            for (Map.Entry<String, Object> entry : action.params.entrySet()) {
                if ("__TAGGEDINPUT__".equals(entry.getValue())) {
                    inputParam = entry.getKey();
                    break;
                }
            }
        }
    }

    public boolean isSearchReady() {
        return (inputParam != null && !INPUT_PLACEHOLDER.equals(_getInputValue()));
    }

    public String getInputValue() {
        return (INPUT_PLACEHOLDER.equals(_getInputValue()) ? "" : _getInputValue());
    }

    private String _getInputValue() {
        return (inputParam != null ? (String) action.params.get(inputParam) : null);
    }

    public static Action readFromParcel(Parcel source) {
        if (source.readInt() == 0) return null;

        Action action = new Action();
        action.urlCommand = source.readString();
        if (action.urlCommand == null) {
            action.action = new JsonAction();
            action.action.cmd = source.createStringArray();
            action.action.params = Util.mapify(source.createStringArray());
            action.action.nextWindow = NextWindow.fromString(source.readString());
            action.inputParam = source.readString();
        }

        return action;
    }

    public static void writeToParcel(Parcel dest, Action action) {
        dest.writeInt(action == null ? 0 : 1);
        if (action == null) return;

        dest.writeString(action.urlCommand);
        if (action.urlCommand == null) {
            dest.writeStringArray(action.action.cmd);
            String[] tokens = new String[action.action.params.size()];
            int i = 0;
            for (Map.Entry entry : action.action.params.entrySet()) {
                tokens[i++] = entry.getKey() + ":" + entry.getValue();
            }
            dest.writeStringArray(tokens);
            dest.writeString(action.action.nextWindow == null ? null : action.action.nextWindow.toString());
            dest.writeString(action.inputParam);
        }
    }

    @Override
    public String toString() {
        return "Action{" +
                "urlCommand='" + urlCommand + '\'' +
                ", action=" + action +
                ", inputParam='" + inputParam + '\'' +
                '}';
    }

    /**
     * Action which can be sent to the server.
     * <p>
     * It is either received from the server or constructed from the CLI specification
     */
    public static class JsonAction {
        /** Array of command terms, f.e. ['playlist', 'jump'] */
        public String[] cmd;

        /** Hash of parameters, f.e. {sort = new}. Passed to the server in the form "key:value", f.e. 'sort:new'. */
        public Map<String, Object> params;

        /** If a nextWindow param is given at the json command level, it takes precedence over a nextWindow param at the item level,
         * which in turn takes precendence over a nextWindow param at the base level.
         * See <item_fields> section for more detail on this parameter. */
        public NextWindow nextWindow;

        public String cmd() {
            return joiner.join(cmd);
        }

        @Override
        public String toString() {
            return "JsonAction{" +
                    "cmd=" + Arrays.toString(cmd) +
                    ", params=" + params +
                    ", nextWindow=" + nextWindow +
                    '}';
        }
    }

    public static class NextWindow {
        public NextWindowEnum nextWindow;
        public String windowId;

        public static NextWindow fromString(String s) {
            return (s == null ? null : new NextWindow(s));
        }

        private NextWindow(String nextWindow) {
            try {
                this.nextWindow = NextWindowEnum.valueOf(nextWindow);
            } catch (IllegalArgumentException e) {
                this.nextWindow = NextWindowEnum.windowId;
                windowId = nextWindow;
            }
        }

        @Override
        public String toString() {
            return (nextWindow == NextWindowEnum.windowId ? windowId : nextWindow.name());
        }
    }

    public enum NextWindowEnum {
        nowPlaying, // push to the Now Playing browse window
        playlist, // push to the current playlist window
        home, // push to the top level "home" window
        parent, // push back to the previous window in the stack and refresh that window with the json that created it
        parentNoRefresh, // same as parent but do not refresh the window
        grandparent, // push back two windows in the stack
        refresh, // stay on this window, but resend the cli command that was used to construct it and refresh the window with the freshly returned data
        refreshOrigin, // push to the previous window in the stack, but resend the cli command that was used to construct it and refresh the window with the freshly returned data
        windowId, // (7.4+)any other value of a window that is present on the window stack and has a "windowId" in it's window fields. Search the window stack backwards until a window with this windowId is found and pop all windows above it.
    }

}