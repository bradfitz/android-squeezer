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

package uk.org.ngo.squeezer.framework;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.framework.ImageDownloader.BitmapDownloaderTask;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * This helper class downloads an image from the Internet and binds it the
 * provided ImageView.
 * <p>
 * Multiple calls with the same URL and different ImageViews will result in the
 * ImageViews sharing the image -- the image is only fetched once.
 * <p>
 * Multiple calls with different URLs and the same ImageView (e.g., if the
 * ImageView is being recycled in a ListAdapter) will result in the ImageView
 * being bound to the last URL provided.
 * <p>
 * A local cache of downloaded images is maintained internally to improve
 * performance.
 */
public class ImageDownloader {
    private static final String LOG_TAG = "ImageDownloader";

    /** Cache size, 1MiB. TODO: Size based on available memory. */
    private static final int mMemoryCacheSize = 1 * 1024 * 1024;

    /** A memory cache of bitmaps, sized to mMemoryCacheSize bytes. */
    private static final LruCache<String, Bitmap> mMemoryCache = new LruCache<String, Bitmap>(
            mMemoryCacheSize) {
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

    /**
     * Maps a URL to a set of one or more ImageViews that must be updated when
     * the image at the URL has been downloaded.
     */
    private static final Hashtable<String, HashSet<WeakReference<ImageView>>> sDownloadsInFlight = new Hashtable<String, HashSet<WeakReference<ImageView>>>();

    /**
     * Downloads the specified image from the Internet and binds it to the
     * provided {@link ImageView}. The binding is immediate if the image is
     * found in the cache and will be done asynchronously otherwise. A null
     * bitmap will be associated to the ImageView if an error occurs.
     *
     * @param url the URL of the image to download
     * @param imageView the ImageView to bind the downloaded image to
     */
    public void downloadUrlToImageView(String url, ImageView imageView) {
        // State sanity: url is guaranteed to never be null in
        // DownloadedDrawable and cache keys.
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }

        resetPurgeTimer();
        Bitmap bitmap = mMemoryCache.get(url);

        if (bitmap == null) {
            // Associate the URL with the image.
            imageView.setImageDrawable(new DownloadedDrawable(url));

            synchronized (sDownloadsInFlight) {
                // If we're already downloading this URL then add the ImageView
                // to the set of ImageViews that will need to updated when the
                // download is complete.

                if (sDownloadsInFlight.containsKey(url)) {
                    sDownloadsInFlight.get(url).add(new WeakReference<ImageView>(imageView));
                } else {
                    // Otherwise, add to the set, and force a download
                    HashSet<WeakReference<ImageView>> h = new HashSet<WeakReference<ImageView>>();
                    h.add(new WeakReference<ImageView>(imageView));
                    sDownloadsInFlight.put(url, h);
                    new BitmapDownloaderTask(url, imageView).execute();
                }
            }
        } else {
            imageView.setImageBitmap(bitmap);
            Animation anim = AnimationUtils.loadAnimation(Squeezer.getContext(), R.anim.fade_in);
            imageView.startAnimation(anim);
        }
    }

    /**
     * Retrieves the URL associated with the {@link ImageView}.
     *
     * @param imageView the imageView
     * @return the URL attached to the {@link DownloadedDrawable} associated
     *         with imageView, or null if no URL is associated.
     */
    private static String getUrl(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getUrl();
            }
        }
        return null;
    }

    /**
     * Downloads the image from url, scaling it to be no more than width x
     * height pixels, preserving the image's aspect ratio.
     *
     * @param url the URL to download from
     * @param width width to scale the image to
     * @param height height to scale the image to
     * @return the scaled bitmap
     */
    Bitmap downloadBitmap(String url, int width, int height) {
        final HttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode +
                        " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                byte[] data = org.apache.http.util.EntityUtils.toByteArray(entity);
                entity.consumeContent();

                // Calculate the correct size for the bitmap.
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                options.inSampleSize = calculateInSampleSize(options, width, height);
                options.inJustDecodeBounds = false;

                return BitmapFactory.decodeByteArray(data, 0, data.length, options);
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + url, e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(LOG_TAG, "Incorrect URL: " + url);
        } catch (Exception e) {
            getRequest.abort();
            Log.w(LOG_TAG, "Error while retrieving bitmap from " + url, e);
        }
        return null;
    }

    /**
     * Calculates the correct value to scale the bitmap described in
     * {@link BitmapFactory.options} options to approximate the requested width
     * and height.
     *
     * @param options contains the bitmap's measured dimensions
     * @param reqWidth the requested width
     * @param reqHeight the requested height
     * @return a value suitable for use as
     *         {@link BitmapFactory.options.inSampleSize} when decoding the
     *         bitmap
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > width) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    /**
     * Downloads an image from a URL, and associates it with an ImageView.
     */
    class BitmapDownloaderTask extends AsyncTask<Void, Void, Bitmap> {
        private final String mUrl;
        private final int viewWidth;
        private final int viewHeight;

        /**
         * Creates the task.
         *
         * @param url the URL to download
         * @param imageView the ImageView to be associated with the URL
         */
        public BitmapDownloaderTask(String url, ImageView imageView) {
            mUrl = url;

            // XXX: getWidth/Height() might return 0 if the ImageView hasn't
            // been measured yet. This is a crude workaround while we figure out
            // a better way to deal with that situation.
            viewWidth = Math.max(imageView.getWidth(), 256);
            viewHeight = Math.max(imageView.getHeight(), 256);
        }

        /**
         * Downloads and scales the bitmap from the URL.
         *
         * @param params unused
         * @return the scaled bitmap
         */
        @Override
        protected Bitmap doInBackground(Void... params) {
            return downloadBitmap(mUrl, viewWidth, viewHeight);
        }

        /**
         * Sets the bitmap as the image for each ImageView it was associated
         * with.
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            addBitmapToCache(mUrl, bitmap);

            // Iterate over all the imageviews that were associated with this URL.
            // Set them to the bitmap, if and only if they're still associated
            // with the URL.
            synchronized (sDownloadsInFlight) {
                for (WeakReference<ImageView> imageViewReference : sDownloadsInFlight.get(mUrl)) {
                    ImageView imageView = imageViewReference.get();
                    String url = getUrl(imageView);

                    if (url != null && url.equals(mUrl)) {
                        imageView.setImageBitmap(bitmap);
                        Animation anim = AnimationUtils.loadAnimation(Squeezer.getContext(),
                                R.anim.fade_in);
                        imageView.startAnimation(anim);
                    }
                }

                sDownloadsInFlight.remove(mUrl);
            }
        }
    }

    /**
     * A fake Drawable that will be attached to the imageView while the download
     * is in progress.
     * <p>
     * Contains a reference to the URL that should be loaded in to the
     * ImageView, so that if it's recycled with a new URL we can check.
     *
     * @see BitmapDownloaderTask.onPostExecute
     */
    static class DownloadedDrawable extends ColorDrawable {
        private final String mUrl;

        /**
         * Constructs a black drawable, and associates it with the URL.
         *
         * @param url the URL to associate with this drawable.
         */
        public DownloadedDrawable(String url) {
            super(Color.BLACK);
            mUrl = url;
        }

        /**
         * Returns the URL associated with this drawable.
         *
         * @return the URL
         */
        public String getUrl() {
            return mUrl;
        }
    }

    /*
     * Cache-related fields and methods.
     */

    /** How many milliseconds of inactivity to wait before purging the cache. */
    private static final int DELAY_BEFORE_PURGE = 10 * 1000;

    private final Handler purgeHandler = new Handler();

    /**
     * Clears the memory cache.
     */
    private final Runnable purger = new Runnable() {
        public void run() {
            mMemoryCache.evictAll();
        }
    };

    /**
     * Adds this bitmap to the cache, keyed off the URL.
     *
     * @param url the URL this bitmap was downloaded from
     * @param bitmap the downloaded bitmap.
     */
    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (mMemoryCache) {
                mMemoryCache.put(url, bitmap);
            }
        }
    }

    /**
     * Clears the timer that purges the cache, and sets a new one.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }
}