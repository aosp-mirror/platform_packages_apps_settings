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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED;

import static com.android.settings.gestures.OneHandedEnablePreferenceController.SUPPORT_ONE_HANDED_MODE;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Handles swipe bottom to expand notification panel gesture.
 **/
public class SwipeBottomToNotificationPreferenceController extends TogglePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;

    private static final String PREF_KEY = "gesture_swipe_bottom_to_notification";

    public SwipeBottomToNotificationPreferenceController(Context context, String key) {
        super(context, key);
    }

    /** Indicates whether the gesture is available or not. */
    public static boolean isGestureAvailable(Context context) {
        // Disable the gesture once One-Handed mode gesture enabled.
        if (SystemProperties.getBoolean(SUPPORT_ONE_HANDED_MODE, false)) {
            return !OneHandedSettingsUtils.isOneHandedModeEnabled(context);
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), PREF_KEY);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED, isChecked ? ON : OFF);
        return true;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED, OFF) == ON;
    }

    @Override
    public CharSequence getSummary() {
        // This toggle preference summary will be updated in gesture preference page since we bound
        // it with entry preference in gesture.xml
        return mContext.getText(
                isChecked() ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }
}
