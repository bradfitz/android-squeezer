/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public class Util {
    private Util() {}

    public static String nonNullString(AtomicReference<String> ref) {
        String string = ref.get();
        return string == null ? "" : string;
    }

    public static int getAtomicInteger(AtomicReference<Integer> ref, int defaultValue) {
    	Integer integer = ref.get();
    	return integer == null ? 0 : integer;
    }


    /**
     * Update target, if it's different from newValue.
     * @param <T>
     * @param target
     * @param newValue
     * @return true if target is updated. Otherwise return false.
     */
    public static <T> boolean atomicReferenceUpdated(AtomicReference<T> target, T newValue) {
        T currentValue = target.get();
        if (currentValue == null && newValue == null)
            return false;
        if (currentValue == null || !currentValue.equals(newValue)) {
            target.set(newValue);
            return true;
        }
        return false;
    }

    public static int parseDecimalInt(String value, int defaultValue) {
    	if (value == null)
   			return defaultValue;
        int decimalPoint = value.indexOf('.');
        if (decimalPoint != -1) value = value.substring(0, decimalPoint);
        if (value.length() == 0) return defaultValue;
        try {
            int intValue = Integer.parseInt(value);
            return intValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseDecimalIntOrZero(String value) {
    	return parseDecimalInt(value, 0);
    }

    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    public synchronized static String makeTimeString(long secs) {
        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        return sFormatter.format("%2$d:%5$02d", timeArgs).toString();
    }

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String decode(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static View getSpinnerItemView(Activity activity, View convertView, String label) {
        TextView view;
        view = (TextView) (convertView != null
                && TextView.class.isAssignableFrom(convertView.getClass())
                ? convertView
                : activity.getLayoutInflater().inflate(R.layout.spinner_item, null));
        view.setText(label);
        return view;
    }
}
