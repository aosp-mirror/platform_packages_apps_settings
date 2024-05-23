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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls the toggle that determines whether notifications
 * should only vibrate (no sound) when the device is unlocked.
 */
public class PoliteNotifVibrateUnlockedToggleController extends TogglePreferenceController {

    public PoliteNotifVibrateUnlockedToggleController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO: b/291897570 - remove this when the feature flag is removed!
        if (!Flags.politeNotifications() || !Flags.vibrateWhileUnlocked()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return mContext.getSystemService(Vibrator.class).hasVibrator() ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, OFF) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
