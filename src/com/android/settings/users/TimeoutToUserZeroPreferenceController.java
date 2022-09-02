/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.users;

import static android.provider.Settings.Secure.TIMEOUT_TO_USER_ZERO;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.Arrays;

/**
 * Controls the preference which launches a settings screen for user to configure whether to
 * automatically switch to the admin user when the device is docked.
 */
public class TimeoutToUserZeroPreferenceController extends BasePreferenceController {
    private final String[] mEntries;
    private final String[] mValues;

    public TimeoutToUserZeroPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);

        mEntries = mContext.getResources().getStringArray(
                com.android.settings.R.array.switch_to_user_zero_when_docked_timeout_entries);
        mValues = mContext.getResources().getStringArray(
                com.android.settings.R.array.switch_to_user_zero_when_docked_timeout_values);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        updateState(screen.findPreference(getPreferenceKey()));
    }

    @Override
    public int getAvailabilityStatus() {
        // Feature not available on device.
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enableTimeoutToUserZeroWhenDocked)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        // Multi-user feature disabled by user.
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.USER_SWITCHER_ENABLED, 0) != 1) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // Is currently user zero. Only non user zero can have this setting.
        if (UserHandle.myUserId() == UserHandle.USER_SYSTEM) {
            return DISABLED_FOR_USER;
        }

        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final String key = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                TIMEOUT_TO_USER_ZERO, UserHandle.myUserId());
        final int index = Arrays.asList(mValues).indexOf(key != null ? key :
                mValues[TimeoutToUserZeroSettings.DEFAULT_TIMEOUT_SETTING_VALUE_INDEX]);

        return mEntries[index];
    }
}
