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

import android.content.Context;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Calendar;
import java.util.Set;

/**
 * A controller that evaluates whether the notification can be triggered and update the
 * SharedPreference.
 */
public class NotificationController {
    private static final String TAG = NotificationController.class.getSimpleName();
    private static final int DISMISS_COUNT_THRESHOLD = 2;
    private static final int NOTIFICATION_COUNT_THRESHOLD = 2;
    private static final int MULTIPLE_BASE = 2;
    // seven days: 7 * 24 * 60
    private static final int MIN_DURATION_BETWEEN_NOTIFICATIONS_MIN = 10080;
    private static final String PROPERTY_MIN_DURATION =
            "android.localenotification.duration.threshold";

    private static NotificationController sInstance = null;

    private final LocaleNotificationDataManager mDataManager;

    /**
     * Get {@link NotificationController} instance.
     *
     * @param context The context
     * @return {@link NotificationController} instance
     */
    public static synchronized NotificationController getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new NotificationController(context);
        }
        return sInstance;
    }

    private NotificationController(Context context) {
        mDataManager = new LocaleNotificationDataManager(context);
    }

    @VisibleForTesting
    LocaleNotificationDataManager getDataManager() {
        return mDataManager;
    }

    /**
     * Increment the dismissCount of the notification.
     *
     * @param locale A locale used to query the {@link NotificationInfo}
     */
    public void incrementDismissCount(@NonNull String locale) {
        NotificationInfo currentInfo = mDataManager.getNotificationInfo(locale);
        NotificationInfo newInfo = new NotificationInfo(currentInfo.getUidCollection(),
                currentInfo.getNotificationCount(),
                currentInfo.getDismissCount() + 1,
                currentInfo.getLastNotificationTimeMs(),
                currentInfo.getNotificationId());
        mDataManager.putNotificationInfo(locale, newInfo);
    }

    /**
     * Whether the notification can be triggered or not.
     *
     * @param uid     The application's uid.
     * @param locale  The application's locale which the user updated to.
     * @return true if the notification needs to be triggered. Otherwise, false.
     */
    public boolean shouldTriggerNotification(int uid, @NonNull String locale) {
        if (LocaleUtils.isInSystemLocale(locale)) {
            return false;
        } else {
            // Add the uid into the locale's uid list and update the notification count if the
            // notification can be triggered.
            return updateLocaleNotificationInfo(uid, locale);
        }
    }

    /**
     * Get the notification id
     *
     * @param locale The locale which the application sets to
     * @return the notification id
     */
    public int getNotificationId(@NonNull String locale) {
        NotificationInfo info = mDataManager.getNotificationInfo(locale);
        return (info != null) ? info.getNotificationId() : -1;
    }

    /**
     * Remove the {@link NotificationInfo} with the corresponding locale
     *
     * @param locale The locale which the application sets to
     */
    public void removeNotificationInfo(@NonNull String locale) {
        mDataManager.removeNotificationInfo(locale);
    }

    private boolean updateLocaleNotificationInfo(int uid, String locale) {
        NotificationInfo info = mDataManager.getNotificationInfo(locale);
        if (info == null) {
            // Create an empty record with the uid and update the SharedPreference.
            NotificationInfo emptyInfo = new NotificationInfo(Set.of(uid), 0, 0, 0, 0);
            mDataManager.putNotificationInfo(locale, emptyInfo);
            return false;
        }
        Set uidCollection = info.getUidCollection();
        if (uidCollection.contains(uid)) {
            return false;
        }

        NotificationInfo newInfo =
                createNotificationInfoWithNewUidAndCount(uidCollection, uid, info);
        mDataManager.putNotificationInfo(locale, newInfo);
        return newInfo.getNotificationCount() > info.getNotificationCount();
    }

    private NotificationInfo createNotificationInfoWithNewUidAndCount(
            Set<Integer> uidSet, int uid, NotificationInfo info) {
        int dismissCount = info.getDismissCount();
        int notificationCount = info.getNotificationCount();
        long lastNotificationTime = info.getLastNotificationTimeMs();
        int notificationId = info.getNotificationId();
        if (dismissCount < DISMISS_COUNT_THRESHOLD
                && notificationCount < NOTIFICATION_COUNT_THRESHOLD) {
            // Add the uid into the locale's uid list
            uidSet.add(uid);
            // Notification should fire on multiples of 2 apps using the locale.
            if (uidSet.size() % MULTIPLE_BASE == 0
                    && !isNotificationFrequent(lastNotificationTime)) {
                // Increment the count because the notification can be triggered.
                notificationCount = info.getNotificationCount() + 1;
                lastNotificationTime = Calendar.getInstance().getTimeInMillis();
                Log.i(TAG, "notificationCount:" + notificationCount);
                if (notificationCount == 1) {
                    notificationId = (int) SystemClock.uptimeMillis();
                }
            }
        }
        return new NotificationInfo(uidSet, notificationCount, dismissCount, lastNotificationTime,
                notificationId);
    }

    /**
     * Evaluates if the notification is triggered frequently.
     *
     * @param lastNotificationTime The timestamp that the last notification was triggered.
     * @return true if the duration of the two continuous notifications is smaller than the
     * threshold.
     * Otherwise, false.
     */
    private boolean isNotificationFrequent(long lastNotificationTime) {
        Calendar time = Calendar.getInstance();
        int threshold = SystemProperties.getInt(PROPERTY_MIN_DURATION,
                MIN_DURATION_BETWEEN_NOTIFICATIONS_MIN);
        time.add(Calendar.MINUTE, threshold * -1);
        return time.getTimeInMillis() < lastNotificationTime;
    }
}
