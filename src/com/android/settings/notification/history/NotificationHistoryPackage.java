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
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class NotificationHistoryPackage {
    String pkgName;
    int uid;
    TreeSet<NotificationHistory.HistoricalNotification> notifications;
    CharSequence label;
    Drawable icon;

    public NotificationHistoryPackage(String pkgName, int uid) {
        this.pkgName = pkgName;
        this.uid = uid;
        notifications = new TreeSet<>(
                (o1, o2) -> Long.compare(o2.getPostedTimeMs(), o1.getPostedTimeMs()));
    }

    public long getMostRecent() {
        if (notifications.isEmpty()) {
            return 0;
        }
        return notifications.first().getPostedTimeMs();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationHistoryPackage that = (NotificationHistoryPackage) o;
        return uid == that.uid &&
                Objects.equals(pkgName, that.pkgName) &&
                Objects.equals(notifications, that.notifications) &&
                Objects.equals(label, that.label) &&
                Objects.equals(icon, that.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkgName, uid, notifications, label, icon);
    }
}
