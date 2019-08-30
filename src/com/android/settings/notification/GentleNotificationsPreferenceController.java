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
 * limitations under the License.
 */

package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import androidx.annotation.VisibleForTesting;

public class GentleNotificationsPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final int ON = 1;

    private NotificationBackend mBackend;

    public GentleNotificationsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
    }

    @VisibleForTesting
    void setBackend(NotificationBackend backend) {
        mBackend = backend;
    }

    @Override
    public CharSequence getSummary() {
        boolean showOnLockscreen = showOnLockscreen();
        boolean showOnStatusBar = showOnStatusBar();

        if (showOnLockscreen) {
            if (showOnStatusBar) {
                return mContext.getString(
                        R.string.gentle_notifications_display_summary_shade_status_lock);
            } else {
                return mContext.getString(R.string.gentle_notifications_display_summary_shade_lock);
            }
        } else if (showOnStatusBar) {
            return mContext.getString(R.string.gentle_notifications_display_summary_shade_status);
        } else {
            return mContext.getString(R.string.gentle_notifications_display_summary_shade);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private boolean showOnLockscreen() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, ON) == ON;
    }

    private boolean showOnStatusBar() {
        return !mBackend.shouldHideSilentStatusBarIcons(mContext);
    }
}
