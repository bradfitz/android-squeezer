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

/**
 * Implements the <help> of the LMS SqueezePlay interface.
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cinput_fields.3E
 */
public class HelpText {
    public String text;
    public String token;

    public static HelpText readFromParcel(Parcel source) {
        if (source.readInt() == 0) return null;

        HelpText helpText = new HelpText();
        helpText.text = source.readString();
        helpText.token = source.readString();

        return helpText;
    }

    public static void writeToParcel(Parcel dest, HelpText helpText) {
        dest.writeInt(helpText == null ? 0 : 1);
        if (helpText == null) return;

        dest.writeString(helpText.text);
        dest.writeString(helpText.token);
    }
}
