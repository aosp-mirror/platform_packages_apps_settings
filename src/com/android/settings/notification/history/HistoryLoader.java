/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.history;

import android.app.NotificationHistory;
import android.app.NotificationHistory.HistoricalNotification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Slog;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryLoader {
    private static final String TAG = "HistoryLoader";
    private final Context mContext;
    private final NotificationBackend mBackend;
    private final PackageManager mPm;

    public HistoryLoader(Context context, NotificationBackend backend, PackageManager pm) {
        mContext = context;
        mBackend = backend;
        mPm = pm;
    }

    public void load(OnHistoryLoaderListener listener) {
        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                Map<String, NotificationHistoryPackage> historicalNotifications = new HashMap<>();
                NotificationHistory history =
                        mBackend.getNotificationHistory(mContext.getPackageName(),
                                mContext.getAttributionTag());
                while (history.hasNextNotification()) {
                    HistoricalNotification hn = history.getNextNotification();

                    String key = hn.getPackage() + "|" + hn.getUid();
                    NotificationHistoryPackage hnsForPackage = historicalNotifications.getOrDefault(
                            key,
                            new NotificationHistoryPackage(hn.getPackage(), hn.getUid()));
                    hnsForPackage.notifications.add(hn);
                    historicalNotifications.put(key, hnsForPackage);
                }
                List<NotificationHistoryPackage> packages =
                        new ArrayList<>(historicalNotifications.values());
                Collections.sort(packages,
                        (o1, o2) -> -1 * Long.compare(o1.getMostRecent(), o2.getMostRecent()));
                for (NotificationHistoryPackage nhp : packages) {
                    ApplicationInfo info;
                    try {
                        info = mPm.getApplicationInfoAsUser(
                                nhp.pkgName,
                                PackageManager.MATCH_UNINSTALLED_PACKAGES
                                        | PackageManager.MATCH_DISABLED_COMPONENTS
                                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE
                                        | PackageManager.MATCH_DIRECT_BOOT_AWARE,
                                UserHandle.getUserId(nhp.uid));
                        if (info != null) {
                            nhp.label = String.valueOf(mPm.getApplicationLabel(info));
                            nhp.icon = mPm.getUserBadgedIcon(mPm.getApplicationIcon(info),
                                    UserHandle.of(UserHandle.getUserId(nhp.uid)));
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // app is gone, just show package name and generic icon
                        nhp.icon = mPm.getDefaultActivityIcon();
                    }
                }
                ThreadUtils.postOnMainThread(() -> listener.onHistoryLoaded(packages));
            } catch (Exception e) {
                Slog.e(TAG, "Error loading history", e);
            }
        });
    }

    interface OnHistoryLoaderListener {
        void onHistoryLoaded(List<NotificationHistoryPackage> notificationsByPackage);
    }
}
