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

package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.android.settings.display.ScreenTimeoutSettings.FALLBACK_SCREEN_TIMEOUT_VALUE;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

/**
 * The controller of {@link ScreenTimeoutSettings}.
 */
public class ScreenTimeoutPreferenceController extends BasePreferenceController {
    public static String PREF_NAME = "screen_timeout";

    public ScreenTimeoutPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        final RestrictedLockUtils.EnforcedAdmin admin = getEnforcedAdmin();
        if (admin != null) {
            preference.setEnabled(false);
            ((RestrictedPreference) preference).setDisabledByAdmin(admin);
        }
        preference.setSummary(getTimeoutSummary());
    }

    private CharSequence getTimeoutSummary() {
        final long currentTimeout = getCurrentScreenTimeout();
        final CharSequence[] timeoutEntries = mContext.getResources().getStringArray(
                R.array.screen_timeout_entries);
        final CharSequence[] timeoutValues = mContext.getResources().getStringArray(
                R.array.screen_timeout_values);
        final CharSequence description = TimeoutPreferenceController.getTimeoutDescription(
                currentTimeout, timeoutEntries, timeoutValues);
        return mContext.getString(R.string.screen_timeout_summary, description);
    }

    private RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin() {
        final DevicePolicyManager dpm = mContext.getSystemService(DevicePolicyManager.class);
        if (dpm != null) {
            return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                    mContext, UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT,
                    UserHandle.myUserId());
        }
        return null;
    }

    private long getCurrentScreenTimeout() {
        return Settings.System.getLong(mContext.getContentResolver(),
                SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
    }
}
