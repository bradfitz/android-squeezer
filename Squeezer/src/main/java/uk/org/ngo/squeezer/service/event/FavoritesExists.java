package uk.org.ngo.squeezer.service.event;

import android.net.Uri;
import android.support.annotation.NonNull;

import static android.R.attr.id;

/**
 * Event sent after receiving a response to a "favorites exists" query.
 */
public class FavoritesExists {
    /** URL of the thing that was checked. */
    @NonNull
    public Uri url;

    /** True if it's listed as a favorite. */
    public boolean isFavorite;

    /** The index in to favorites (required if you want to delete it), or the empty string. */
    @NonNull
    public String index;

    public FavoritesExists(@NonNull Uri url, boolean isFavorite, @NonNull String index) {
        this.url = url;
        this.isFavorite = isFavorite;
        this.index = index;
    }

    @Override
    public String toString() {
        return "FavoritesExists{" + id + " / " + isFavorite + " / " + index + '}';
    }
}
