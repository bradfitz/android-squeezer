package uk.org.ngo.squeezer.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.util.ThemeManager;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * View that shows a imageButton to display and toggle the "favorite" state of the current item. If
 * the state is unknown then shows an indeterminate progress bar instead.
 * <p>
 * To use, instantiate the view as normal. Once you have called {@link #findViewById(int)} to
 * find it, you should call {@link #setThemeId(int)} to set the current theme, and
 * {@link #setFavoriteListener(FavoriteListener)} to provide callbacks that will be called when
 * the user requests that the item be added or removed from favorites.
 * <p>
 * It is the hosting activity/fragment's responsibility to actually do this, typically by
 * using {@link uk.org.ngo.squeezer.service.ISqueezeService#favoritesAdd(Uri, String)} and
 * {@link uk.org.ngo.squeezer.service.ISqueezeService#favoritesDelete(Uri, String)}.
 * <p>
 * When the hosting activity/fragment learns the "favorite state" of the item it should call
 * {@link #setState(int, String)} to set the state, and the view will update accordingly.
 */
public class FavoriteButton extends RelativeLayout {
    public interface FavoriteListener {
        /**
         * Called when the current item should be added to favorites.
         */
        void addFavorite();

        /**
         * Called when the item given by {@code index} should be removed from favorites.
         *
         * @param index The "favorite index" of the item to be removed.
         */
        void removeFavorite(@NonNull String index);
    }

    @Retention(SOURCE)
    @IntDef({FAVORITE_UNKNOWN, FAVORITE_TRUE, FAVORITE_FALSE})
    @interface FavoriteState {}
    /** Unknown if this item is a favorite or not, waiting for the server to respond. */
    public static final int FAVORITE_UNKNOWN = 0;
    /** Item is not a favorite. */
    public static final int FAVORITE_TRUE = 1;
    /** Item is a favorite. */
    public static final int FAVORITE_FALSE = 2;

    /** Item's state. */
    private @FavoriteState int state = FAVORITE_UNKNOWN;

    /** If state is FAVORITE_TRUE then this is the item's index ID in favorites. */
    private String index;

    /** Button that allows the user to toggle the song's favorite state. */
    private ImageButton imageButton;

    /** Indeterminate progress shown while waiting for the server to respond with favorite state. */
    private ProgressBar progressBar;

    /** Application's theme ID. */
    private int themeId;

    /** Listener for callbacks. */
    private FavoriteListener favoriteListener;

    public FavoriteButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.favorite_button, this);

        imageButton = (ImageButton) view.findViewById(R.id.buttonFavorite);
        imageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                toggleFavoriteState();
            }
        });

        progressBar = (ProgressBar) view.findViewById(R.id.progressFavorite);
    }

    /**
     * Record the current theme ID. See {@link BaseActivity#getThemeId()}
     *
     * @param themeId ID to record.
     */
    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    public void setFavoriteListener(FavoriteListener favoriteListener) {
        this.favoriteListener = favoriteListener;
    }

    /**
     * Set the state of the favorite view. You would normally call this after receiving a
     * {@link uk.org.ngo.squeezer.service.event.FavoritesExists} event.
     *
     * @param state The item's state.
     * @param index The item's index in to favorites.
     */
    public void setState(@FavoriteState int state, @Nullable String index) {
        this.state = state;
        this.index = index;
        updateFavoriteIcon();
    }

    /**
     * Handle clicks on the button. If the listener is configured then call the appropriate
     * callback.
     */
    private void toggleFavoriteState() {
        if (favoriteListener == null) {
            return;
        }

        switch (state) {
            case FAVORITE_TRUE:  // Is a favorite, so remove it.
                favoriteListener.removeFavorite(index);
                break;
            case FAVORITE_FALSE: // Not a favorite, so add it.
                favoriteListener.addFavorite();
                break;
            case FAVORITE_UNKNOWN:  // Should never happen.
                break;
        }
    }

    /**
     * Update the favorite icon.
     * <p>
     * If the favorite state of the current song is unknown then show the indeterminate progress
     * bar and hide the button for showing/toggling the favorite state.
     * <p>
     * If the favorite state is known then show the button with the appropriate icon.
     */
    private void updateFavoriteIcon() {
        if (state == FAVORITE_UNKNOWN) {
            imageButton.setVisibility(GONE);
            progressBar.setVisibility(VISIBLE);
            return;
        }

        imageButton.setImageResource(state == FAVORITE_TRUE ?
                R.drawable.ic_action_favorites_remove :
                R.drawable.ic_action_favorites_add);

        if (themeId == ThemeManager.Theme.DARK.mThemeId) {
            imageButton.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            imageButton.setAlpha(204);
        } else {
            imageButton.setAlpha(121);
        }

        imageButton.setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
    }
}
