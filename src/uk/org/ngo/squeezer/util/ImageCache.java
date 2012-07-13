/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import org.acra.ErrorReporter;

import uk.org.ngo.squeezer.Squeezer;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.util.LruCache;

import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Editor;
import com.jakewharton.DiskLruCache.Snapshot;

/**
 * A (singleton) 2 level LRU cache implementation. The first level is in-memory,
 * the second is on-disk.
 * <p>
 * The on-disk cache is persistent across invocations of the application, the
 * in-memory is cleared DELAY_BEFORE_PURGE ms after the last access.
 */
public class ImageCache {
    @SuppressWarnings("unused")
    private static final String TAG = "ImageCache";

    /** Singleton ImageCache instance */
    private static ImageCache instance;

    /** In-memory cache */
    private static LruCache<String, Bitmap> sMemoryCache;

    /** How many milliseconds of inactivity to wait before purging the cache. */
    private static final int DELAY_BEFORE_PURGE = 10 * 1000;

    /** Handler for "purge the cache" messages */
    private final Handler purgeHandler = new Handler();

    /** Disk cache */
    private static DiskLruCache sDiskCache;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 10L; // 10MB
    private static final String DISK_CACHE_SUBDIR = "artwork";

    /** Flag to temporarily pause disk access */
    private boolean mPauseDiskAccess = false;

    /** Private constructor to enforce singleton. */
    private ImageCache() {
        Context context = Squeezer.getContext();

        int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass();

        // Size cache to 1/8th of the application's available memory (MiB)
        int memoryCacheSize = 1024 * 1024 * memClass / 8;

        sMemoryCache = new LruCache<String, Bitmap>(memoryCacheSize) {
            /**
             * Measure cache by the size of bitmaps in it, not the count.
             * 
             * @see android.support.v4.util.LruCache#sizeOf(java.lang.Object,
             *      java.lang.Object)
             */
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // .getByteCount() not available in API 7.
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };

        // XXX: Deal with the exceptions
        try {
            final File cachePath =
                    Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED ?
                            new File(Environment.getExternalStorageDirectory(),
                                    "Android/data/uk.org.ngo.squeezer/cache/" + DISK_CACHE_SUBDIR) :
                            new File(context.getCacheDir(), DISK_CACHE_SUBDIR);

            sDiskCache = DiskLruCache.open(cachePath,
                    Squeezer.getContext().getPackageManager()
                            .getPackageInfo(Squeezer.getContext().getPackageName(), 0).versionCode,
                    1, DISK_CACHE_SIZE);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            ErrorReporter.getInstance().handleException(e);
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            ErrorReporter.getInstance().handleException(e);
            e.printStackTrace();
        }
    }

    /**
     * Returns the singleton cache instance, creating it if necessary.
     * 
     * @return
     */
    synchronized public static ImageCache getInstance() {
        if (instance == null) {
            instance = new ImageCache();
        }

        return instance;
    }

    public void setPauseDiskAccess(boolean state) {
        mPauseDiskAccess = state;
    }

    /**
     * Retrieves a bitmap from the memory cache only, with no disk access on
     * cache miss.
     * 
     * @param url
     * @return
     */
    public Bitmap getFast(String url) {
        String key = URLEncoder.encode(url);
        return sMemoryCache.get(key);
    }

    /**
     * Retrieves a bitmap from the cache. If the item is not found in the cache
     * it's retrieved and inserted.
     * <p>
     * Returns the bitmap, or null if it couldn't be retrieved for any reason.
     * 
     * @param url
     * @return
     */
    public Bitmap get(String url) {
        String key = URLEncoder.encode(url);

        Bitmap bitmap = sMemoryCache.get(key);
        if (bitmap != null) {
            return bitmap;
        }

        Snapshot snapshot;
        try {
            snapshot = sDiskCache.get(key);
            if (snapshot != null) {
                while (mPauseDiskAccess) {
                }
                bitmap = BitmapFactory.decodeStream(snapshot.getInputStream(0), null, null);
                sMemoryCache.put(key, bitmap);
                return bitmap;
            }
        } catch (IOException e) {
            return null;
        }

        return null;
    }

    /**
     * Writes the bitmap to the cache, using url as the key.
     * 
     * @param url
     * @param bitmap
     */
    public void put(String url, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        // TODO: Check to see if the item already exists in the cache.
        String key = URLEncoder.encode(url);
        sMemoryCache.put(key, bitmap);

        // TODO: Spin the disk write out in to a different thread.
        Editor editor = null;
        try {
            editor = sDiskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeBitmapToEditor(bitmap, editor)) {
                sDiskCache.flush();
                editor.commit();
            } else {
                editor.abort();
            }
        } catch (IOException e) {
            if (editor != null) {
                try {
                    editor.abort();
                } catch (IOException ignored) {

                }
            }
        }
    }

    private boolean writeBitmapToEditor(Bitmap bitmap, Editor editor) throws IOException {
        OutputStream out = null;

        try {
            out = new BufferedOutputStream(editor.newOutputStream(0));
            return bitmap.compress(CompressFormat.PNG, 0, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Clears the timer that purges the cache, and sets a new one.
     */
    public void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }

    /**
     * Clears the memory cache.
     */
    private final Runnable purger = new Runnable() {
        public void run() {
            sMemoryCache.evictAll();
        }
    };
}
