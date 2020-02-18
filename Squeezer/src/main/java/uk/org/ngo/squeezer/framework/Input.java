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
 * Implements <input_fields> of the LMS SqueezePlay interface.
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cinput_fields.3E
 */
public class Input {
    public int len;
    public String allowedChars;
    public String inputStyle;
    public String title;
    public String initialText;
    public HelpText help;
    public String softbutton1;
    public String softbutton2;

    public static Input readFromParcel(Parcel source) {
        if (source.readInt() == 0) return null;

        Input input = new Input();
        input.len = source.readInt();
        input.allowedChars = source.readString();
        input.inputStyle = source.readString();
        input.title = source.readString();
        input.initialText = source.readString();
        input.help = HelpText.readFromParcel(source);
        input.softbutton1 = source.readString();
        input.softbutton2 = source.readString();

        return input;
    }

    public static void writeToParcel(Parcel dest, Input input) {
        dest.writeInt(input == null ? 0 : 1);
        if (input == null) return;

        dest.writeInt(input.len);
        dest.writeString(input.allowedChars);
        dest.writeString(input.inputStyle);
        dest.writeString(input.title);
        dest.writeString(input.initialText);
        HelpText.writeToParcel(dest, input.help);
        dest.writeString(input.softbutton1);
        dest.writeString(input.softbutton2);
    }
}