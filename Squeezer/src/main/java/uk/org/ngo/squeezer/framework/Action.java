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

import java.util.Arrays;
import java.util.Map;

import uk.org.ngo.squeezer.Util;

/**
 * Implements <code>action_fields</code> of the LMS SqueezePlay interface.
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cactions_fields.3E
 */
public class Action {
    private static final String INPUT_PLACEHOLDER = "__TAGGEDINPUT__";
    public String urlCommand;
    public NextWindow nextWindow;
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
        action.urlCommand = source.readString();
        action.nextWindow = NextWindow.fromString(source.readString());
        if (action.urlCommand == null) {
            action.action = new JsonAction();
            action.action.cmd = source.createStringArray();
            action.action.inputParam = source.readString();
            action.action.params = Util.mapify(source.createStringArray());
        }

        return action;
    }

    public static void writeToParcel(Parcel dest, Action action) {
        dest.writeInt(action == null ? 0 : 1);
        if (action == null) return;

        dest.writeString(action.urlCommand);
        dest.writeString(action.nextWindow == null ? null : action.nextWindow.toString());
        if (action.urlCommand == null) {
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

    @Override
    public String toString() {
        return "Action{" +
                "urlCommand='" + urlCommand + '\'' +
                ", action=" + action +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Action action1 = (Action) o;

        if (urlCommand != null ? !urlCommand.equals(action1.urlCommand) : action1.urlCommand != null)
            return false;
        if (nextWindow != null ? !nextWindow.equals(action1.nextWindow) : action1.nextWindow != null)
            return false;
        return action != null ? action.equals(action1.action) : action1.action == null;

    }

    @Override
    public int hashCode() {
        int result = urlCommand != null ? urlCommand.hashCode() : 0;
        result = 31 * result + (nextWindow != null ? nextWindow.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    /**
     * Action which can be sent to the server.
     * <p>
     * It is either received from the server or constructed from the CLI specification
     */
    public static class JsonAction {
        /** Array of command terms, f.e. ['playlist', 'jump'] */
        public String[] cmd;
        public String inputParam;
        /** Hash of parameters, f.e. {sort = new}. Passed to the server in the form "key:value", f.e. 'sort:new'. */
        public Map<String, Object> params;

        @Override
        public String toString() {
            return "JsonAction{" +
                    "cmd=" + Arrays.toString(cmd) +
                    ", params=" + params +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonAction that = (JsonAction) o;

            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(cmd, that.cmd)) return false;
            if (inputParam != null ? !inputParam.equals(that.inputParam) : that.inputParam != null)
                return false;
            return params != null ? params.equals(that.params) : that.params == null;

        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(cmd);
            result = 31 * result + (inputParam != null ? inputParam.hashCode() : 0);
            result = 31 * result + (params != null ? params.hashCode() : 0);
            return result;
        }
    }

    public static class NextWindow {
        public NextWindowEnum nextWindow;
        public String windowId;

        public static NextWindow fromString(String s) {
            return (s == null ? null : new NextWindow(s));
        }

        public NextWindow(String nextWindow) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NextWindow that = (NextWindow) o;

            if (nextWindow != that.nextWindow) return false;
            return windowId != null ? windowId.equals(that.windowId) : that.windowId == null;

        }

        @Override
        public int hashCode() {
            int result = nextWindow != null ? nextWindow.hashCode() : 0;
            result = 31 * result + (windowId != null ? windowId.hashCode() : 0);
            return result;
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