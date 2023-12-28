/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.localepicker;

import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_NOTIFICATION_ID;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.overlay.FeatureFactory;

/**
 * A Broadcast receiver that handles the locale notification which is swiped away.
 */
public class NotificationCancelReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationCancelReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String appLocale = intent.getExtras().getString(EXTRA_APP_LOCALE);
        int notificationId = intent.getExtras().getInt(EXTRA_NOTIFICATION_ID, -1);
        int savedNotificationID = getNotificationController(context).getNotificationId(
                appLocale);
        Log.i(TAG, "Locale notification is swiped away.");
        if (savedNotificationID == notificationId) {
            getNotificationController(context).incrementDismissCount(appLocale);
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(context,
                    SettingsEnums.ACTION_NOTIFICATION_SWIPE_FOR_SYSTEM_LOCALE);
        }
    }

    @VisibleForTesting
    NotificationController getNotificationController(Context context) {
        return NotificationController.getInstance(context);
    }
}
