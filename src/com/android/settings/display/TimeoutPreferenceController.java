/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;

public class TimeoutPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "TimeoutPrefContr";

    /** If there is no setting in the provider, use this. */
    public static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private final String mScreenTimeoutKey;

    public TimeoutPreferenceController(Context context, String key) {
        super(context);
        mScreenTimeoutKey = key;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mScreenTimeoutKey;
    }

    @Override
    public void updateState(Preference preference) {
        final TimeoutListPreference timeoutListPreference = (TimeoutListPreference) preference;
        final long currentTimeout = Settings.System.getLong(mContext.getContentResolver(),
                SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        timeoutListPreference.setValue(String.valueOf(currentTimeout));
        final DevicePolicyManager dpm =
                (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtilsInternal.checkIfMaximumTimeToLockIsSet(mContext);
            final long maxTimeout =
                    dpm.getMaximumTimeToLock(null /* admin */, UserHandle.myUserId());
            timeoutListPreference.removeUnusableTimeouts(maxTimeout, admin);
        }
        updateTimeoutPreferenceDescription(timeoutListPreference,
                Long.parseLong(timeoutListPreference.getValue()));

        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT,
                UserHandle.myUserId());
        if (admin != null) {
            timeoutListPreference.removeUnusableTimeouts(0/* disable all*/, admin);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_TIMEOUT, value);
            updateTimeoutPreferenceDescription((TimeoutListPreference) preference, value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist screen timeout setting", e);
        }
        return true;
    }

    public static CharSequence getTimeoutDescription(
            long currentTimeout, CharSequence[] entries, CharSequence[] values) {
        if (currentTimeout < 0 || entries == null || values == null
                || values.length != entries.length) {
            return null;
        }

        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (currentTimeout == timeout) {
                return entries[i];
            }
        }
        return null;
    }

    private void updateTimeoutPreferenceDescription(TimeoutListPreference preference,
            long currentTimeout) {
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        final String summary;
        if (preference.isDisabledByAdmin()) {
            summary = mContext.getString(com.android.settings.R.string.disabled_by_policy_title);
        } else {
            final CharSequence timeoutDescription = getTimeoutDescription(
                    currentTimeout, entries, values);
            summary = timeoutDescription == null
                    ? ""
                    : mContext.getString(R.string.screen_timeout_summary, timeoutDescription);
        }
        preference.setSummary(summary);
    }

}
