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

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class Util {

    /** {@link java.util.regex.Pattern} that splits strings on colon. */
    private static final Pattern mColonSplitPattern = Pattern.compile(":");

    private Util() {
    }


    /**
     * Update target, if it's different from newValue.
     *
     * @return true if target is updated. Otherwise return false.
     */
    public static <T> boolean atomicReferenceUpdated(AtomicReference<T> target, T newValue) {
        T currentValue = target.get();
        if (currentValue == null && newValue == null) {
            return false;
        }
        if (currentValue == null || !currentValue.equals(newValue)) {
            target.set(newValue);
            return true;
        }
        return false;
    }

    public static double parseDouble(String value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value.length() == 0) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseDecimalInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        int decimalPoint = value.indexOf('.');
        if (decimalPoint != -1) {
            value = value.substring(0, decimalPoint);
        }
        if (value.length() == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseDecimalIntOrZero(String value) {
        return parseDecimalInt(value, 0);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getRecord(Map<String, Object> record, String recordName) {
        return (Map<String, Object>) record.get(recordName);
    }

    public static double getDouble(Map<String, Object> record, String fieldName) {
        return getDouble(record, fieldName, 0);
    }

    public static double getDouble(Map<String, Object> record, String fieldName, double defaultValue) {
        return getDouble(record.get(fieldName), defaultValue);
    }

    public static double getDouble(Object value, double defaultValue) {
        return (value instanceof Number) ? ((Number)value).doubleValue() : parseDouble((String)value, defaultValue);
    }

    public static int getInt(Map<String, Object> record, String fieldName) {
        return getInt(record, fieldName, 0);
    }

    public static int getInt(Map<String, Object> record, String fieldName, int defaultValue) {
        return getInt(record.get(fieldName), defaultValue);
    }

    public static int getInt(Object value, int defaultValue) {
        return (value instanceof Number) ? ((Number)value).intValue() : parseDecimalInt((String)value, defaultValue);
    }

    public static String getString(Map<String, Object> record, String fieldName) {
        return getString(record.get(fieldName), null);
    }

    public static String getString(Map<String, Object> record, String fieldName, String defaultValue) {
        return getString(record.get(fieldName), defaultValue);
    }

    @NonNull
    public static String getStringOrEmpty(Map<String, Object> record, String fieldName) {
        return getStringOrEmpty(record.get(fieldName));
    }

    @NonNull
    public static String getStringOrEmpty(Object value) {
        return getString(value, "");
    }

    public static String getString(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        return (value instanceof String) ? (String)value : value.toString();
    }

    public static String[] getStringArray(Object[] objects) {
        String[] result = new String[objects == null ? 0 : objects.length];
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                result[i] = getString(objects[i], null);
            }
        }
        return result;
    }

    public static Map<String, Object > mapify(String[] tokens) {
        Map<String, Object> tokenMap = new HashMap<>();
        for (String token : tokens) {
            String[] split = mColonSplitPattern.split(token, 2);
            tokenMap.put(split[0], split.length > 1 ? split[1] : null);
        }
        return tokenMap;
    }

    /** Make sure the icon/image tag is an absolute URL. */
    private static final Pattern HEX_PATTERN = Pattern.compile("^\\p{XDigit}+$");
    @NonNull
    private static Uri getImageUrl(String urlPrefix, String imageId) {
        if (imageId != null) {
            if (HEX_PATTERN.matcher(imageId).matches()) {
                // if the iconId is a hex digit, this is a coverid or remote track id(a negative id)
                imageId = "/music/" + imageId + "/cover";
            }

            // Make sure the url is absolute
            if (!Uri.parse(imageId).isAbsolute()) {
                imageId = urlPrefix + (imageId.startsWith("/") ? imageId : "/" + imageId);
            }
        }
        return Uri.parse(imageId != null ? imageId : "");
    }

    @NonNull
    public static Uri getImageUrl(Map<String, Object> record, String fieldName) {
        return getImageUrl(getString(record, "urlPrefix"), getString(record, fieldName));
    }

    private static final StringBuilder sFormatBuilder = new StringBuilder();

    private static final Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    private static final Object[] sTimeArgs = new Object[5];

    /**
     * Formats an elapsed time in the form "M:SS" or "H:MM:SS" for display.
     * <p>
     * Like {@link android.text.format.DateUtils#formatElapsedTime(long)} but without the leading
     * zeroes if the number of minutes is < 10.
     *
     * @param elapsedSeconds the elapsed time, in seconds.
     */
    public synchronized static String formatElapsedTime(long elapsedSeconds) {
        calculateTimeArgs(elapsedSeconds);
        sFormatBuilder.setLength(0);
        return sFormatter.format("%2$d:%5$02d", sTimeArgs).toString();
    }

    private static void calculateTimeArgs(long elapsedSeconds) {
        sTimeArgs[0] = elapsedSeconds / 3600;
        sTimeArgs[1] = elapsedSeconds / 60;
        sTimeArgs[2] = (elapsedSeconds / 60) % 60;
        sTimeArgs[3] = elapsedSeconds;
        sTimeArgs[4] = elapsedSeconds % 60;
    }

    /**
     * Returns {@code true} if the arguments are equal to each other
     * and {@code false} otherwise.
     * Consequently, if both arguments are {@code null}, {@code true}
     * is returned and if exactly one argument is {@code null}, {@code
     * false} is returned.  Otherwise, equality is determined by using
     * the {@link Object#equals equals} method of the first
     * argument.
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    /**
     * @return a view suitable for use as a spinner view.
     */
    public static View getSpinnerItemView(Context context, View convertView, ViewGroup parent,
                                          String label) {
        return getSpinnerView(context, convertView, parent, label,
                android.R.layout.simple_spinner_item);
    }

    public static View getActionBarSpinnerItemView(Context context, View convertView,
                                                   ViewGroup parent, String label) {
        return getSpinnerView(context, convertView, parent, label,
                android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
    }

    private static View getSpinnerView(Context context, View convertView, ViewGroup parent,
                                       String label, int layout) {
        TextView view;
        view = (TextView) (convertView != null
                && TextView.class.isAssignableFrom(convertView.getClass())
                ? convertView
                : ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                        layout, parent, false));
        view.setText(label);
        return view;
    }

    /** Helper to set alpha value for a view, since View.setAlpha is API level 11 */
    public static View setAlpha(View view, float alpha) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(alpha, alpha);
        alphaAnimation.setDuration(0);
        alphaAnimation.setFillAfter(true);
        view.startAnimation(alphaAnimation);

        return view;
    }

    /** @return True if Crashlytics is supported in this build. */
    public static boolean supportCrashlytics() {
        // Don't include in debug builds
        if (BuildConfig.DEBUG) {
            return false;
        }

        // Don't include in <= API 7 (only works on API 8 and above).
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
            return false;
        }

        return true;
    }

    /**
     * Calls {@link Crashlytics#setString(String, String)} if Crashlytics is
     * enabled in this build, otherwise does nothing.
     */
    public static void crashlyticsSetString(String key, String value) {
        if (supportCrashlytics()) {
            Crashlytics.setString(key, value);
        }
    }

    /**
     * Calls {@link Crashlytics#setLong(String, long)} if Crashlytics is
     * enabled in this build, otherwise does nothing.
     */
    public static void crashlyticsSetLong(String key, long value) {
        if (supportCrashlytics()) {
            Crashlytics.setLong(key, value);
        }
    }

    /**
     * Calls {@link Crashlytics#log(String)} if Crashlytics is enabled in
     * this build, otherwise log to .
     */
    public static void crashlyticsLog(String msg) {
        if (supportCrashlytics()) {
            Crashlytics.log(msg);
        } else {
            Log.i("Util.crashlyticsLog", msg);
        }
    }

    /**
     * Calls {@link Crashlytics#log(int, String, String)} if Crashlytics is
     * enabled in this build, otherwise does nothing.
     */
    public static void crashlyticsLog(int priority, String tag, String msg) {
        if (supportCrashlytics()) {
            Crashlytics.log(priority, tag, msg);
        } else {
            Log.println(priority, tag, msg);
        }
    }

    /**
     * Calls {@link Crashlytics#logException(Throwable)} if Crashlytics is
     * enabled in this build, otherwise does nothing.
     */
    public static void crashlyticsLogException(java.lang.Throwable throwable) {
        if (supportCrashlytics()) {
            Crashlytics.logException(throwable);
        } else {
            Log.i("Util.crashlyticsLog", "", throwable);
        }
    }

    @NonNull
    public static String getBaseName(String fileName) {
        String name = new File(fileName).getName();
        int pos = name.lastIndexOf(".");
        return (pos > 0) ? name.substring(0, pos) : name;
    }

    public static void moveFile(File sourceFile, File destinationFile) throws IOException {
        File destFolder = destinationFile.getParentFile();
        if (!destFolder.exists()) {
            if (!destFolder.mkdirs()) {
                throw new IOException("Cant create folder for '" + destinationFile + "'");
            }
        }
        if (!sourceFile.renameTo(destinationFile)) {
            // We could not rename. This may be because source and destination are on different
            // mount points, so we attempt to copy and delete instead.
            FileChannel sourceChannel = null;
            FileChannel destinationChannel = null;
            try {
                sourceChannel = new FileInputStream(sourceFile).getChannel();
                destinationChannel = new FileOutputStream(destinationFile).getChannel();
                // Transfer the file in chunks to avoid out of memory issues
                final long blockSize = Math.min(268435456, sourceChannel.size());
                long position = 0;
                while (destinationChannel.transferFrom(sourceChannel, position, blockSize) > 0) {
                    position += blockSize;
                }
            } finally {
                if (sourceChannel != null)
                    sourceChannel.close();
                if (destinationChannel != null)
                    destinationChannel.close();
            }
            if (!sourceFile.delete()) {
                throw new IOException("failed to delete " + sourceFile);
            }
        }
    }
}
