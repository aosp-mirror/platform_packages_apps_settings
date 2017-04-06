/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.utils;

import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.os.RemoteException;

/**
 * Wrappers around methods in {@link INotificationManager} and {@link NotificationChannel} to
 * facilitate unit testing.
 *
 * TODO: delete this class once robolectric supports Android O
 */
public class NotificationChannelHelper {
    private INotificationManager mNotificationManager;

    public NotificationChannelHelper(
            INotificationManager notificationManager) {
        mNotificationManager = notificationManager;
    }

    /**
     * Returns the notification channel settings for a app given its package name, user id, and
     * channel id.
     */
    public NotificationChannelWrapper getNotificationChannelForPackage(String pkg, int uid,
            String channelId, boolean includeDeleted) throws RemoteException {
        NotificationChannel channel = mNotificationManager.getNotificationChannelForPackage(
                pkg, uid, channelId, includeDeleted);
        return channel == null ? null : new NotificationChannelWrapper(channel);
    }

    /**
     * Wrapper around {@link NotificationChannel} to facilitate unit testing.
     *
     * TODO: delete this class once robolectric supports Android O
     */
    public class NotificationChannelWrapper {
        private NotificationChannel mChannel;

        public NotificationChannelWrapper(NotificationChannel channel) {
            mChannel = channel;
        }

        public int getImportance() {
            return mChannel.getImportance();
        }
    }
}
