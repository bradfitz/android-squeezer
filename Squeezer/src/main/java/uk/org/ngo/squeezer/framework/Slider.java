/*
 * Copyright (c) 2020 Kurt Aaholst <kaaholst@gmail.com>
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

public class Slider implements Parcelable {
    public int min;
    public int max;
    public int adjust;
    public int initial;
    public String sliderIcons;
    public String help;

    public Slider() {
    }

    protected Slider(Parcel in) {
        min = in.readInt();
        max = in.readInt();
        adjust = in.readInt();
        initial = in.readInt();
        sliderIcons = in.readString();
        help = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(min);
        dest.writeInt(max);
        dest.writeInt(adjust);
        dest.writeInt(initial);
        dest.writeString(sliderIcons);
        dest.writeString(help);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Slider> CREATOR = new Creator<Slider>() {
        @Override
        public Slider createFromParcel(Parcel in) {
            return new Slider(in);
        }

        @Override
        public Slider[] newArray(int size) {
            return new Slider[size];
        }
    };
}
