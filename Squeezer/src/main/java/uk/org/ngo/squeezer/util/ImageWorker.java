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

package uk.org.ngo.squeezer.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.google.common.base.Joiner;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;

import uk.org.ngo.squeezer.BuildConfig;

/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an
 * ImageView. It handles things like using a memory and disk cache, running the work in a background
 * thread and setting a placeholder image.
 */
public abstract class ImageWorker {

    private static final String TAG = "ImageWorker";

    private static final int FADE_IN_TIME = 200;

    private ImageCache mImageCache;

    private Bitmap mLoadingBitmap;

    private boolean mFadeInBitmap = true;

    private boolean mExitTasksEarly = false;

    protected boolean mPauseWork = false;

    private final Object mPauseWorkLock = new Object();

    protected final Resources mResources;

    private static final int MESSAGE_CLEAR = 0;

    private static final int MESSAGE_INIT_DISK_CACHE = 1;

    private static final int MESSAGE_FLUSH = 2;

    private static final int MESSAGE_CLOSE = 3;

    /** Joiner for the components that make up a key in the memory cache. */
    protected static final Joiner mMemCacheKeyJoiner = Joiner.on(':');

    /** Paint to use when colouring debug swatches on images. */
    private static final Paint mCacheDebugPaint = new Paint();

    /** Colour of debug swatch for images loaded from memory cache. */
    private static final int mCacheDebugColorMemory = Color.GREEN;

    /** Colour of debug swatch for images loaded from disk cache. */
    private static final int mCacheDebugColorDisk = Color.BLUE;

    /** Colour of debug swatch for images loaded from network (no caching). */
    private static final int mCacheDebugColorNetwork = Color.RED;

    protected ImageWorker(Context context) {
        mResources = context.getResources();
    }

    /**
     * Load an image specified by the data parameter into an ImageView (override {@link
     * ImageWorker#processBitmap(BitmapWorkerTaskParams)} to define the processing logic). A memory and disk cache
     * will be used if an {@link ImageCache} has been set using {@link
     * ImageWorker#setImageCache(ImageCache)}. If the image is found in the memory cache, it is set
     * immediately, otherwise an {@link AsyncTask} will be created to asynchronously load the
     * bitmap.
     *
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void loadImage(final Object data, final ImageView imageView) {
        if (data == null) {
            return;
        }

        Bitmap bitmap = null;

        int width = imageView.getWidth();
        int height = imageView.getHeight();

        // If the dimensions aren't known yet then the view hasn't been measured. Get a
        // ViewTreeObserver and listen for the PreDraw message. Using a GlobalLayoutListener
        // does not work for views that are in the list but drawn off-screen, possibly due
        // to the convertview. See http://stackoverflow.com/a/14325365 for some discussion.
        // The solution there, of posting a runnable, does not appear to reliably work on
        // devices running (at least) API 7. An OnPreDrawListener appears to work, and will
        // be called after measurement is complete.
        if (width == 0 || height == 0) {
            // Store the URL in the imageView's tag, in case the URL assigned to is changed.
            imageView.setTag(data);

            imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                    // If the imageView is still assigned to the URL then we can load in to it.
                    if (data.equals(imageView.getTag())) {
                        loadImage(data, imageView);
                    }
                    return true;
                }
            });
            return;
        }

        String memCacheKey = hashKeyForMemory(String.valueOf(data), width, height);

        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(memCacheKey);
        }

        if (bitmap != null) {
            // Bitmap found in memory cache
            if (BuildConfig.DEBUG) {
                addDebugSwatch(new Canvas(bitmap), mCacheDebugColorMemory);
            }
            imageView.setImageBitmap(bitmap);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mResources, mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            // NOTE: This uses a custom version of AsyncTask that has been pulled from the
            // framework and slightly modified. Refer to the docs at the top of the class
            // for more info on what was changed.
            task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR,
                    new BitmapWorkerTaskParams(width, height, data, memCacheKey));
        }
    }

    /**
     * Generates a hash key for the memory cache. The key includes the target width and height,
     * so that multiple copies of the image may exist in the cache at different sizes.
     *
     * @param data The identifier for the image (e.g., URL).
     * @param width Target width for the bitmap.
     * @param height Target height for the bitmap.
     * @return Cache key to use.
     */
    @NonNull
    private static String hashKeyForMemory(@NonNull String data, int width, int height) {
        return mMemCacheKeyJoiner.join(width, height, data);
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     *
     * @param bitmap
     */
    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     *
     * @param resId
     */
    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
    }

    /**
     * Adds a debug swatch to a canvas. The marker is a triangle pointing north-west
     * on the top left corner, the edges are 25% of the canvas' width and height.
     *
     * @param canvas The canvas to draw on.
     * @param color The colour to use for the swatch.
     */
    public static void addDebugSwatch(Canvas canvas, int color) {
        float width = canvas.getWidth();
        float height = canvas.getHeight();

        Path path = new Path();
        path.lineTo(width / 4, 0);
        path.lineTo(0, height / 4);
        path.lineTo(0, 0);

        // Draw the swatch.
        mCacheDebugPaint.setColor(color);
        mCacheDebugPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, mCacheDebugPaint);

        // Stroke the swatch with a white hairline.
        mCacheDebugPaint.setColor(Color.WHITE);
        mCacheDebugPaint.setStyle(Paint.Style.STROKE);
        mCacheDebugPaint.setStrokeWidth(0);
        canvas.drawPath(path, mCacheDebugPaint);
    }

    /**
     * Adds an {@link ImageCache} to this worker in the background, to prevent disk access on
     * the UI thread.
     *
     * @param imageCacheParams A description of the cache.
     */
    public void addImageCache(ImageCache.ImageCacheParams imageCacheParams) {
        setImageCache(new ImageCache(imageCacheParams));
        new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
    }

    /**
     * Sets the {@link ImageCache} object to use with this ImageWorker. Usually you will not need to
     * call this directly, instead use {@link ImageWorker#addImageCache} which will create and add
     * the {@link ImageCache} object in a background thread (to ensure no disk access on the main/UI
     * thread).
     *
     * @param imageCache
     */
    public void setImageCache(ImageCache imageCache) {
        mImageCache = imageCache;
    }

    /**
     * If set to true, the image will fade-in once it has been loaded by the background thread.
     */
    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    /**
     * Set the flag that determines whether tasks should exit early. Set to true if any pending
     * work should be abandoned (e.g., in {@link Activity#onPause}).
     *
     * @param exitTasksEarly
     */
    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
    }

    /**
     * Subclasses should override this to define any processing or work that must happen to produce
     * the final bitmap. This will be executed in a background thread and be long running. For
     * example, you could resize a large bitmap here, or pull down an image from the network.
     *
     * @param params The parameters to identify which image to process, as provided by {@link
     * ImageWorker#loadImage(Object, ImageView)}
     *
     * @return The processed bitmap, or null if processing failed.
     */
    @Nullable
    protected abstract Bitmap processBitmap(BitmapWorkerTaskParams params);

    /**
     * Cancels any pending work attached to the provided ImageView.
     *
     * @param imageView
     */
    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (BuildConfig.DEBUG) {
                final Object bitmapData = bitmapWorkerTask.data;
                Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
            }
        }
    }

    /**
     * Returns true if the current work has been cancelled or if there was no work in progress on
     * this image view. Returns false if the work in progress deals with the same data. The work is
     * not stopped in that case.
     */
    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.data;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
                }
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Any imageView
     *
     * @return Retrieve the currently active work task (if any) associated with this imageView. null
     * if there is no such task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    protected class BitmapWorkerTaskParams {
        /** Desired bitmap width. */
        public final int width;

        /** Desired bitmap height. */
        public final int height;

        /** Identifier for the bitmap to fetch (e.g., a URL). */
        @NonNull
        public final Object data;

        /** Cache key to use when saving the bitmap in the memory cache. */
        @NonNull
        public final String memCacheKey;

        public BitmapWorkerTaskParams(int width, int height,
                                      @NonNull Object data, @NonNull String memCacheKey) {
            this.width = width;
            this.height = height;
            this.data = data;
            this.memCacheKey = memCacheKey;
        }
    }

    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    private class BitmapWorkerTask extends AsyncTask<BitmapWorkerTaskParams, Void, Bitmap> {

        private Object data;

        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * Background processing.
         */
        @Override
        protected Bitmap doInBackground(BitmapWorkerTaskParams... params) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground - starting work");
            }

            boolean loadedFromNetwork = false;

            data = params[0].data;
            final String dataString = String.valueOf(data);
            Bitmap bitmap = null;
            Bitmap scaledBitmap = null;

            // Wait here if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            // If the image cache is available and this task has not been cancelled by another
            // thread and the ImageView that was originally bound to this task is still bound back
            // to this task and our "exit early" flag is not set then try and fetch the bitmap from
            // the cache
            if (mImageCache != null && !isCancelled() && getAttachedImageView() != null
                    && !mExitTasksEarly) {
                bitmap = mImageCache.getBitmapFromDiskCache(dataString);
            }

            // If the bitmap was not found in the cache and this task has not been cancelled by
            // another thread and the ImageView that was originally bound to this task is still
            // bound back to this task and our "exit early" flag is not set, then call the main
            // process method (as implemented by a subclass)
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null
                    && !mExitTasksEarly) {
                bitmap = processBitmap(params[0]);
                loadedFromNetwork = true;
            }

            // If the bitmap was loaded then add it to the disk cache at the original size.
            if (bitmap != null && mImageCache != null) {
                mImageCache.addBitmapToDiskCache(dataString, bitmap);
            }

            // Scale the bitmap to the requested size.
            if (bitmap != null && params[0].width > 0 && params[0].height > 0) {
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, params[0].width, params[0].height,
                        false);
            }

            // If the bitmap was processed and the image cache is available, then add the processed
            // bitmap to the memory cache for future use. Note we don't check if the task was
            // cancelled here, if it was, and the thread is still running, we may as well add the
            // processed bitmap to our cache as it might be used again in the future.
            if (scaledBitmap != null && mImageCache != null) {
                mImageCache.addBitmapToMemoryCache(params[0].memCacheKey, scaledBitmap);
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground - finished work");
            }

            if (BuildConfig.DEBUG && scaledBitmap != null) {
                if (loadedFromNetwork) {
                    addDebugSwatch(new Canvas(scaledBitmap), mCacheDebugColorNetwork);
                } else {
                    addDebugSwatch(new Canvas(scaledBitmap), mCacheDebugColorDisk);
                }

            }
            return scaledBitmap;
        }

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled() || mExitTasksEarly) {
                bitmap = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (bitmap != null && imageView != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onPostExecute - setting bitmap");
                }
                setImageBitmap(imageView, bitmap);
            }
        }

        @Override
        protected void onCancelled(Bitmap bitmap) {
            super.onCancelled(bitmap);
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        /**
         * Returns the ImageView associated with this task as long as the ImageView's task still
         * points to this task as well. Returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work is in progress.
     * Contains a reference to the actual worker task, so that it can be stopped if a new binding is
     * required, and makes sure that only the last started worker process can bind its result,
     * independently of the finish order.
     */
    private static class AsyncDrawable extends BitmapDrawable {

        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * Called when the processing is complete and the final bitmap should be set on the ImageView.
     *
     * @param imageView
     * @param bitmap
     */
    private void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (mFadeInBitmap) {
            // Transition drawable between the pending image and the final bitmap.
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[]{
                            imageView.getDrawable(),
                            new BitmapDrawable(mResources, bitmap)
                    });

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageDrawable(new BitmapDrawable(mResources, bitmap));
        }
    }

    public void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            switch ((Integer) params[0]) {
                case MESSAGE_CLEAR:
                    clearCacheInternal();
                    break;
                case MESSAGE_INIT_DISK_CACHE:
                    initDiskCacheInternal();
                    break;
                case MESSAGE_FLUSH:
                    flushCacheInternal();
                    break;
                case MESSAGE_CLOSE:
                    closeCacheInternal();
                    break;
            }
            return null;
        }
    }

    protected void initDiskCacheInternal() {
        if (mImageCache != null) {
            mImageCache.initDiskCache();
        }
    }

    protected void clearCacheInternal() {
        if (mImageCache != null) {
            mImageCache.clearCache();
        }
    }

    protected void flushCacheInternal() {
        if (mImageCache != null) {
            mImageCache.flush();
        }
    }

    protected void closeCacheInternal() {
        if (mImageCache != null) {
            mImageCache.close();
            mImageCache = null;
        }
    }

    public void clearCache() {
        new CacheAsyncTask().execute(MESSAGE_CLEAR);
    }

    public void flushCache() {
        new CacheAsyncTask().execute(MESSAGE_FLUSH);
    }

    public void closeCache() {
        new CacheAsyncTask().execute(MESSAGE_CLOSE);
    }
}
