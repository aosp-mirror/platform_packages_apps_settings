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

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.server.notification.Flags;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;

public class PoliteNotificationGlobalPreferenceController extends
        SettingsMainSwitchPreferenceController {

    public static final int ON = 1;
    public static final int OFF = 0;
    public PoliteNotificationGlobalPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO: b/291897570 - remove this when the feature flag is removed!
        if (Flags.politeNotifications()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
