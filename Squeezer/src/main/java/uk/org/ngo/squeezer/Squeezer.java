package uk.org.ngo.squeezer;


import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import uk.org.ngo.squeezer.util.ImageCache;
import uk.org.ngo.squeezer.util.ImageFetcher;

// Trick to make the app context useful available everywhere.
// See http://stackoverflow.com/questions/987072/using-application-context-everywhere

public class Squeezer extends Application {

    private static Squeezer instance;

    private volatile static ImageFetcher sImageFetcher;

    public Squeezer() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    /**
     * @return an imagefetcher globally useful for the application, with a cache that
     *     is maintained across activities.
     */
    @NonNull
    public static ImageFetcher getImageFetcher() {
        ImageFetcher result = sImageFetcher;
        if (result == null) {
            synchronized (Squeezer.class) {
                result = sImageFetcher;
                if (result == null) {
                    sImageFetcher = new ImageFetcher(instance);
                    sImageFetcher.setLoadingImage(R.drawable.icon_pending_artwork);
                    ImageCache.ImageCacheParams imageCacheParams = new ImageCache.ImageCacheParams(instance, "artwork");
                    imageCacheParams.setMemCacheSizePercent(instance, 0.12f);
                    sImageFetcher.addImageCache(imageCacheParams);
                }
            }
        }
        return sImageFetcher;
    }

    /**
     * Clear the image memory cache if memory gets low.
     */
    @Override
    public void onLowMemory() {
        if (sImageFetcher != null) {
            sImageFetcher.clearMemoryCache();
        }
    }
}

