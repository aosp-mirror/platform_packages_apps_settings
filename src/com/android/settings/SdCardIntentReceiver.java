/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Config;
import android.util.Log;

/**
 * 
 */
public class SdCardIntentReceiver extends BroadcastReceiver {

    private static final int SDCARD_STATUS = 1;
    private static final String TAG = "SdCardIntentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        String action = intent.getAction();
        if (Config.LOGD) Log.d(TAG, "onReceiveIntent " + action);

        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            nm.cancel(SDCARD_STATUS);

            Intent statusIntent = new Intent(Intent.ACTION_MAIN, null);
            statusIntent.setClass(context, SdCardSettings.class);
            nm.notify(SDCARD_STATUS, new Notification(context,
                    android.R.drawable.stat_notify_sdcard,
                    null,
                    System.currentTimeMillis(),
                    context.getText(R.string.sdcard_setting),
                    null,
                    statusIntent));
        } else if (action.equals(Intent.ACTION_MEDIA_REMOVED)) {
            nm.cancel(SDCARD_STATUS);
        } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
            nm.cancel(SDCARD_STATUS);

            Intent statusIntent = new Intent(Intent.ACTION_MAIN, null);
            statusIntent.setClass(context, SdCardSettings.class);
            nm.notify(SDCARD_STATUS, new Notification(context,
                    android.R.drawable.stat_notify_sdcard_usb,
                    null,
                    System.currentTimeMillis(),
                    "SD Card",
                    null,
                    statusIntent));
        }
    }
}
