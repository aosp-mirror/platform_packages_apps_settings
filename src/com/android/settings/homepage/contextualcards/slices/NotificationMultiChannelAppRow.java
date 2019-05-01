/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards.slices;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.PackageInfo;

import com.android.settings.notification.NotificationBackend;

import java.util.concurrent.Callable;

/**
 * This class is responsible for getting notification app row from package which has multiple
 * notification channels.{@link NotificationChannelSlice} uses it to improve latency.
 */
class NotificationMultiChannelAppRow implements Callable<NotificationBackend.AppRow> {

    private final Context mContext;
    private final NotificationBackend mNotificationBackend;
    private final PackageInfo mPackageInfo;

    public NotificationMultiChannelAppRow(Context context, NotificationBackend notificationBackend,
            PackageInfo packageInfo) {
        mContext = context;
        mNotificationBackend = notificationBackend;
        mPackageInfo = packageInfo;
    }

    @Override
    public NotificationBackend.AppRow call() throws Exception {
        final int channelCount = mNotificationBackend.getChannelCount(
                mPackageInfo.applicationInfo.packageName, mPackageInfo.applicationInfo.uid);
        if (channelCount > 1) {
            return mNotificationBackend.loadAppRow(mContext, mContext.getPackageManager(),
                    mContext.getSystemService(RoleManager.class), mPackageInfo);
        }
        return null;
    }
}
