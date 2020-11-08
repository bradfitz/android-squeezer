package uk.org.ngo.squeezer.widget;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class OnSwipeListener extends GestureDetector.SimpleOnGestureListener {

    public boolean onSwipeUp(){
        return false;
    }

    public boolean onSwipeDown(){
        return false;
    }

    public boolean onSwipeLeft(){
        return false;
    }

    public boolean onSwipeRight(){
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            return velocityX < 0 ? onSwipeLeft() : onSwipeRight();
        } else {
            return velocityY < 0 ? onSwipeUp() : onSwipeDown();
        }
    }

}