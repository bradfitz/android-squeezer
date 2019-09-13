/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Simplifies common {@link Notification} tasks.
 */
public class NotificationUtil {

    /**
     * Create notification channel for O (API 26) and above. Does nothing for pre-O (26) devices.
     *
     * @param context global information about an application environment
     * @param channelId The id of the channel
     * @param channelName The user-visible name of the channel
     * @param channelDescription The user-visible description of the channel
     */
    public static void createNotificationChannel(
            Context context,
            String channelId,
            String channelName,
            String channelDescription,
            int channelImportance,
            boolean channelEnableVibrate,
            int channelLockscreenVisibility
            ) {

        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Initializes NotificationChannel.
            NotificationChannel notificationChannel =
                    new NotificationChannel(channelId, channelName, channelImportance);
            notificationChannel.setDescription(channelDescription);
            notificationChannel.enableVibration(channelEnableVibrate);
            notificationChannel.setLockscreenVisibility(channelLockscreenVisibility);

            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
