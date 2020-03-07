/*
 * Copyright (c) 2011 Google Inc.  All Rights Reserved.
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


package uk.org.ngo.squeezer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.sdsmdg.harjot.crollerTest.Croller;
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener;

import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.service.ISqueezeService;


/**
 * Implement a custom volume toast view
 */
public class VolumePanel extends Handler implements OnCrollerChangeListener {

    private static final int TIMEOUT_DELAY = 3000;

    private static final int MSG_VOLUME_CHANGED = 0;

    private static final int MSG_TIMEOUT = 2;

    private final BaseActivity mActivity;

    /**
     * Dialog displaying the volume panel.
     */
    private final Dialog mDialog;

    /**
     * View displaying volume sliders.
     */
    private final View mView;

    private final TextView mMessage;
    private final TextView mLabel;

    private final Croller mSeekbar;
    private int mCurrentProgress = 0;
    private boolean mTrackingTouch = false;

    @SuppressLint({"InflateParams"}) // OK, as view is passed to Dialog.setView()
    public VolumePanel(BaseActivity activity) {
        mActivity = activity;

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.volume_adjust, null);
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                resetTimeout();
                return false;
            }
        });

        mMessage = mView.findViewById(R.id.message);
        mLabel = mView.findViewById(R.id.label);
        mSeekbar = mView.findViewById(R.id.level);

        mSeekbar.setOnCrollerChangeListener(this);

        mDialog = new Dialog(mActivity, R.style.VolumePanel) { //android.R.style.Theme_Panel) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (isShowing() && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    forceTimeout();
                    return true;
                }
                return false;
            }
        };
        mDialog.setTitle("Volume Control");
        mDialog.setContentView(mView);

        // Set window properties to match other toasts/dialogs.
        Window window = mDialog.getWindow();
        window.setGravity(Gravity.TOP);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = null;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
    }

    public void dismiss() {
        removeMessages(MSG_TIMEOUT);
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void resetTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessageDelayed(obtainMessage(MSG_TIMEOUT), TIMEOUT_DELAY);
    }

    private void forceTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessage(obtainMessage(MSG_TIMEOUT));
    }

    @Override
    public void onProgressChanged(Croller croller, int progress) {
        if (mCurrentProgress != progress) {
            mCurrentProgress = progress;
            ISqueezeService service = mActivity.getService();
            if (service != null) {
                service.adjustVolumeTo(progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(Croller croller) {
        mTrackingTouch = true;
        removeMessages(MSG_TIMEOUT);
    }

    @Override
    public void onStopTrackingTouch(Croller croller) {
        mTrackingTouch = false;
        resetTimeout();
    }

    public void postVolumeChanged(int newVolume, String additionalMessage) {
        if (hasMessages(MSG_VOLUME_CHANGED)) {
            return;
        }
        obtainMessage(MSG_VOLUME_CHANGED, newVolume, 0, additionalMessage).sendToTarget();
    }

    private void onShowVolumeChanged(int newVolume, String additionalMessage) {
        if (mTrackingTouch) {
            return;
        }

        mCurrentProgress = newVolume;
        mSeekbar.setProgress(newVolume);
        mMessage.setText(mActivity.getString(R.string.volume, mActivity.getString(R.string.app_name)));
        mLabel.setText(additionalMessage);

        if (!mDialog.isShowing() && !mActivity.isFinishing()) {
            mDialog.setContentView(mView);
            mDialog.show();
        }

        resetTimeout();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

            case MSG_VOLUME_CHANGED: {
                onShowVolumeChanged(msg.arg1, (String) msg.obj);
                break;
            }

            case MSG_TIMEOUT: {
                dismiss();
                break;
            }
        }
    }
}

