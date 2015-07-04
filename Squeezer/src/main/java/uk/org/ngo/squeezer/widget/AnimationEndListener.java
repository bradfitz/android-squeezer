package uk.org.ngo.squeezer.widget;

import android.view.animation.Animation;

/**
 * {@link Animation.AnimationListener} with default empty implementations of
 * {@link Animation.AnimationListener#onAnimationRepeat(Animation)} and
 * {@link Animation.AnimationListener#onAnimationStart(Animation)}.
 * <p>
 * This is just for a more convenient syntax if you only need to override the end action.
 */
public abstract class AnimationEndListener implements Animation.AnimationListener {
    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}
