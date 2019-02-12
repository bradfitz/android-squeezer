/*
 * Copyright (c) 2019 Kurt Aaholst <kaaholst@gmail.com>
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

import com.google.common.base.Joiner;

import java.util.Map;

import uk.org.ngo.squeezer.Util;

/** Holds data for a display status message */
public class DisplayMessage {
    /** tells SP what style of popup to use. Valid types are 'popupplay', 'icon', 'song', 'mixed', and 'popupalbum'. In 7.6, 'alertWindow' has been added (see next section) */
    public final String type;

    /** duration in seconds to display the showBriefly popup. Defaults to 3 seconds. In 7.6, a duration of -1 will create a popup that doesn't go away until dismissed. */
    public final int duration;

    /** used for specific styles to be used in popup windows, e.g. adding a + badge when adding a favorite. */
    public final String style;

    /** The message to show. */
    public final String text;

    public DisplayMessage(Map<String, Object> display) {
        type = Util.getString(display, "type");
        duration = Util.getInt(display, "type");
        style = Util.getString(display, "type");
        Object[] texts = (Object[]) display.get("text");
        text = Joiner.on('\n').join(texts).replaceAll("\\\\n", "\n");
    }

    @Override
    public String toString() {
        return "DisplayMessage{" +
                "type='" + type + '\'' +
                ", duration=" + duration +
                ", style='" + style + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
