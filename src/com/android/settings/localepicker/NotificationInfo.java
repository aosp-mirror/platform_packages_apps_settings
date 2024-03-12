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

import java.util.Objects;
import java.util.Set;

class NotificationInfo {
    private Set<Integer> mUidCollection;
    private int mNotificationCount;
    private int mDismissCount;
    private long mLastNotificationTimeMs;
    private int mNotificationId;

    private NotificationInfo() {
    }

    NotificationInfo(Set<Integer> uidCollection, int notificationCount, int dismissCount,
            long lastNotificationTimeMs, int notificationId) {
        this.mUidCollection = uidCollection;
        this.mNotificationCount = notificationCount;
        this.mDismissCount = dismissCount;
        this.mLastNotificationTimeMs = lastNotificationTimeMs;
        this.mNotificationId = notificationId;
    }

    public Set<Integer> getUidCollection() {
        return mUidCollection;
    }

    public int getNotificationCount() {
        return mNotificationCount;
    }

    public int getDismissCount() {
        return mDismissCount;
    }

    public long getLastNotificationTimeMs() {
        return mLastNotificationTimeMs;
    }

    public int getNotificationId() {
        return mNotificationId;
    }

    public void setUidCollection(Set<Integer> uidCollection) {
        this.mUidCollection = uidCollection;
    }

    public void setNotificationCount(int notificationCount) {
        this.mNotificationCount = notificationCount;
    }

    public void setDismissCount(int dismissCount) {
        this.mDismissCount = dismissCount;
    }

    public void setLastNotificationTimeMs(long lastNotificationTimeMs) {
        this.mLastNotificationTimeMs = lastNotificationTimeMs;
    }

    public void setNotificationId(int notificationId) {
        this.mNotificationId = notificationId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof NotificationInfo)) return false;
        NotificationInfo that = (NotificationInfo) o;
        return (mUidCollection.equals(that.mUidCollection))
                && (mDismissCount == that.mDismissCount)
                && (mNotificationCount == that.mNotificationCount)
                && (mLastNotificationTimeMs == that.mLastNotificationTimeMs)
                && (mNotificationId == that.mNotificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUidCollection, mDismissCount, mNotificationCount,
                mLastNotificationTimeMs, mNotificationId);
    }
}
