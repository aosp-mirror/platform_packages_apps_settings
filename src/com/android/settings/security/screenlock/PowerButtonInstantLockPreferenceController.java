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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.core.AbstractPreferenceController;

public class PowerButtonInstantLockPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;
    private final TrustAgentManager mTrustAgentManager;

    public PowerButtonInstantLockPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
        mTrustAgentManager = FeatureFactory.getFeatureFactory()
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
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(
                mLockPatternUtils.getPowerButtonInstantlyLocks(mUserId));
        final CharSequence trustAgentLabel = mTrustAgentManager.getActiveTrustAgentLabel(
                mContext, mLockPatternUtils);
        if (!TextUtils.isEmpty(trustAgentLabel)) {
            preference.setSummary(mContext.getString(
                    R.string.lockpattern_settings_power_button_instantly_locks_summary,
                    trustAgentLabel));
        } else {
            preference.setSummary(com.android.settingslib.R.string.summary_empty);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_POWER_INSTANTLY_LOCKS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mLockPatternUtils.setPowerButtonInstantlyLocks((Boolean) newValue, mUserId);
        return true;
    }
}
