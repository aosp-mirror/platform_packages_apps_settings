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

import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class ShowOnlyUnseenNotificationsOnLockscreenPreferenceController
        extends TogglePreferenceController {

    private static final int UNSET = 0;
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
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, UNSET) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, isChecked ? ON : OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        int setting = Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, UNSET);
        if (setting == UNSET) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }
}
