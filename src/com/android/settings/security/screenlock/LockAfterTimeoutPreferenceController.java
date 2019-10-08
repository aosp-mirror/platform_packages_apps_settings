/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.security.screenlock;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.display.TimeoutListPreference;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;

public class LockAfterTimeoutPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;
    private final TrustAgentManager mTrustAgentManager;
    private final DevicePolicyManager mDPM;

    public LockAfterTimeoutPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
        mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mTrustAgentManager = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider().getTrustAgentManager();
    }

    @Override
    public boolean isAvailable() {
        if (!mLockPatternUtils.isSecure(mUserId)) {
            return false;
        }
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCK_AFTER_TIMEOUT;
    }

    @Override
    public void updateState(Preference preference) {
        setupLockAfterPreference((TimeoutListPreference) preference);
        updateLockAfterPreferenceSummary((TimeoutListPreference) preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            final int timeout = Integer.parseInt((String) newValue);
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            updateState(preference);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist lockAfter timeout setting", e);
        }
        return true;
    }

    private void setupLockAfterPreference(TimeoutListPreference preference) {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        preference.setValue(String.valueOf(currentTimeout));
        if (mDPM != null) {
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtilsInternal.checkIfMaximumTimeToLockIsSet(mContext);
            final long adminTimeout =
                    mDPM.getMaximumTimeToLock(null /* admin */, UserHandle.myUserId());
            final long displayTimeout = Math.max(0,
                    Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            final long maxTimeout = Math.max(0, adminTimeout - displayTimeout);
            preference.removeUnusableTimeouts(maxTimeout, admin);
        }
    }

    private void updateLockAfterPreferenceSummary(TimeoutListPreference preference) {
        final CharSequence summary;
        if (preference.isDisabledByAdmin()) {
            summary = mContext.getText(R.string.disabled_by_policy_title);
        } else {
            // Update summary message with current value
            long currentTimeout = Settings.Secure.getLong(mContext.getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.valueOf(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }

            final CharSequence trustAgentLabel = mTrustAgentManager
                    .getActiveTrustAgentLabel(mContext, mLockPatternUtils);
            if (!TextUtils.isEmpty(trustAgentLabel)) {
                if (Long.valueOf(values[best].toString()) == 0) {
                    summary = mContext.getString(R.string.lock_immediately_summary_with_exception,
                            trustAgentLabel);
                } else {
                    summary = mContext.getString(R.string.lock_after_timeout_summary_with_exception,
                            entries[best], trustAgentLabel);
                }
            } else {
                summary = mContext.getString(R.string.lock_after_timeout_summary, entries[best]);
            }
        }
        preference.setSummary(summary);
    }
}
