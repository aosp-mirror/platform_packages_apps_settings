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

package com.android.settings.notification;

import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class ShowOnlyUnseenNotificationsOnLockscreenPreferenceController
        extends TogglePreferenceController {

    // This is the default value for phones, before notification minimalism, this setting is
    // unavailable to phones, we use this value to hide the toggle on phones.
    private static final int UNSET_UNAVAILABLE = 0;
    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 2;

    public ShowOnlyUnseenNotificationsOnLockscreenPreferenceController(
            Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, UNSET_UNAVAILABLE) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, isChecked ? ON : OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        // Hide when the notifications on lock screen page flag is enabled.
        if (Flags.notificationLockScreenSettings()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (Flags.notificationMinimalism()) {
            if (!isNotifOnLockScreenEnabled()) {
                return DISABLED_DEPENDENT_SETTING;
            }
            // We want to show the switch when the lock screen notification minimalism flag is on.
            return AVAILABLE;
        }
        int setting = Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, UNSET_UNAVAILABLE);
        if (setting == UNSET_UNAVAILABLE) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }

    private boolean isNotifOnLockScreenEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) == 1;
    }
}
