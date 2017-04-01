/*
 * Copyright (C) 2012 The Android Open Source Project
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

package uk.org.ngo.squeezer.download;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import uk.org.ngo.squeezer.Preferences;

/**
 * This class holds options for download storage.
 */
public class DownloadStorage {

    private final Preferences prefs;
    private final Context context;

    public DownloadStorage(Context context) {
        this.context = context;
        prefs = new Preferences(context);
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public File getDownloadDir() throws IOException {
        if (prefs.isDownloadUseSdCard()) {
            return getRemovableMediaStorage();
        }
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    }


    /**
     * @return True if removable music storage (like an SD card) is available, false otherwise.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean isPublicMediaStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            File publicMusicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            return Environment.isExternalStorageRemovable(publicMusicDirectory);
        }
        return false;
    }

    /**
     * @return True if removable music storage (like an SD card) is available, false otherwise.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean hasRemovableMediaStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            for (File dir : context.getExternalMediaDirs())
                if (dir != null && Environment.isExternalStorageRemovable(dir))
                    return true;
        return false;
    }

    /**
     * @return The first music directory which is on a removable storage.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private File getRemovableMediaStorage() throws IOException {
        for (File dir : context.getExternalMediaDirs()) {
            if (dir != null && Environment.isExternalStorageRemovable(dir))
                return new File(dir, Environment.DIRECTORY_MUSIC);
        }
        throw new IOException("A removable media directory was expected, but none was found");
    }

}
