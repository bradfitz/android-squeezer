/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.download;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import uk.org.ngo.squeezer.service.SqueezeService;


/**
 * Handle events from the download manager
 * <p>
 * This class is registered in the manifest.
 */
public class DownloadStatusReceiver extends BroadcastReceiver {
    private static final String TAG = DownloadStatusReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                handleUserRequest(context);
            }
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                SqueezeService.onDownloadComplete(context, intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void handleUserRequest(Context context) {
        Log.i(TAG, "Download notification clicked");
        Intent intent = new Intent(context, CancelDownloadsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
