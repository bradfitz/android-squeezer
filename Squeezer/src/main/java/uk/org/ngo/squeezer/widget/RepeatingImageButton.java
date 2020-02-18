/*
 * Copyright (c) 2008 The Android Open Source Project
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

package uk.org.ngo.squeezer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * A button that will repeatedly call a 'listener' method as long as the button is pressed.
 */
@SuppressLint("NewApi")
public class RepeatingImageButton extends androidx.appcompat.widget.AppCompatImageButton {

    private long mStartTime;

    private int mRepeatCount;

    private RepeatListener mListener;

    private long mInterval = 500;

    public RepeatingImageButton(Context context) {
        this(context, null);
    }

    public RepeatingImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.imageButtonStyle);
    }

    @SuppressLint("NewApi")
    public RepeatingImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
        setLongClickable(true);
    }

    /**
     * Sets the listener to be called while the button is pressed and the interval in milliseconds
     * with which it will be called.
     *
     * @param l The listener that will be called
     * @param interval The interval in milliseconds for calls
     */
    public void setRepeatListener(RepeatListener l, long interval) {
        mListener = l;
        mInterval = interval;
    }

    @Override
    public boolean performLongClick() {
        mStartTime = SystemClock.elapsedRealtime();
        mRepeatCount = 0;
        post(mRepeater);
        return true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // remove the repeater, but call the hook one more time
            removeCallbacks(mRepeater);
            if (mStartTime != 0) {
                doRepeat(true);
                mStartTime = 0;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // remove the repeater, but call the hook one more time
                removeCallbacks(mRepeater);
                if (mStartTime != 0) {
                    doRepeat(true);
                    mStartTime = 0;
                }
        }
        return super.onKeyUp(keyCode, event);
    }

    private final Runnable mRepeater = new Runnable() {
        @Override
        public void run() {
            doRepeat(false);
            if (isPressed()) {
                postDelayed(this, mInterval);
            }
        }
    };

    private void doRepeat(boolean last) {
        long now = SystemClock.elapsedRealtime();
        if (mListener != null) {
            mListener.onRepeat(this, now - mStartTime, last ? -1 : mRepeatCount++);
        }
    }

    public interface RepeatListener {

        /**
         * This method will be called repeatedly at roughly the interval specified in
         * setRepeatListener(), for as long as the button is pressed.
         *
         * @param v The button as a View.
         * @param duration The number of milliseconds the button has been pressed so far.
         * @param repeatcount The number of previous calls in this sequence. If this is going to be
         * the last call in this sequence (i.e. the user just stopped pressing the button), the
         * value will be -1.
         */
        void onRepeat(View v, long duration, int repeatcount);
    }
}
