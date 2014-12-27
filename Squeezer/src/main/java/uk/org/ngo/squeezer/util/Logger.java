/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.util;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.atomic.AtomicBoolean;

import uk.org.ngo.squeezer.BuildConfig;

/**
 * Encapsulates the use of an error backend, and ensures that reports are only sent log in debug
 * builds, and ensures consistent reporting.
 */
public class Logger {

    private static AtomicBoolean useErrorBackend = new AtomicBoolean();

    /**
     * Prepare the app for use with an error backend.<br/>
     * <b>Note<br/></b>
     * Will only use a backend for release builds
     * <p/>
     * Call this at app startup
     * @param context
     */
    public static void setup(Context context) {
        if (!BuildConfig.DEBUG) {
            useErrorBackend.set(true);
            Crashlytics.start(context);
        }
    }

    /** Associate the supplied string with the data for the next report */
    public static void log(String message) {
        if (useErrorBackend.get()) {
            Crashlytics.log(message);
        }
    }

    /**
     * Associate the supplied string with the data for the next report
     * <p/>
     * Also write to logCat using {@link Log#println(int, String, String)}
     * at {@link Log#WARN} priority
     */
    public static void log(String tag, String message) {
        if (useErrorBackend.get()) {
            Crashlytics.log(Log.WARN, tag, message);
        }
    }

    /**
     * Logs and send the error to a backend
     * <p/>
     * See {@link Log#w(String, String)} for an explanation of the arguments
     */
    public static void logError(String tag, String message, String... log) {
        StringBuilder sb = new StringBuilder(message);
        for (String line : log)
            sb.append("\n\t").append(line);
        Log.w(tag, sb.toString());
        logException(new RuntimeException(tag + ": " + sb.toString()));
    }

    /**
     * Logs and send the exception to a backend
     * <p/>
     * See {@link Log#w(String, String, Throwable)} for an explanation of the arguments
     */
    public static void logException(String tag, String message, Throwable throwable) {
        Log.w(tag, message, throwable);
        logException(throwable);
    }

    /**
     * Send the exception to a backend
     * @param throwable An exception to log
     */
    public static void logException(Throwable throwable) {
        if (useErrorBackend.get()) {
            Crashlytics.logException(throwable);
        }
    }
}
