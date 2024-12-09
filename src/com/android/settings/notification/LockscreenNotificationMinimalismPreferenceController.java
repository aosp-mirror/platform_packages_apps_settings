/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.provider.Settings.Secure.LOCK_SCREEN_NOTIFICATION_MINIMALISM;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class LockscreenNotificationMinimalismPreferenceController
        extends TogglePreferenceController {

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    public LockscreenNotificationMinimalismPreferenceController(
            Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_NOTIFICATION_MINIMALISM, ON) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_NOTIFICATION_MINIMALISM, isChecked ? ON : OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        // Hide when the notifications on lock screen settings page flag is enabled.
        if (Flags.notificationLockScreenSettings()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (!Flags.notificationMinimalism()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        int lockScreenNotif = Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, 0);
        if (lockScreenNotif == 0) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }
}
