/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settingslib.transition.SettingsTransitionHelper;

public class ChangeProfileScreenLockPreferenceController extends
        ChangeScreenLockPreferenceController {

    private static final String KEY_UNLOCK_SET_OR_CHANGE_PROFILE = "unlock_set_or_change_profile";

    private final String mPreferenceKey;

    public ChangeProfileScreenLockPreferenceController(Context context,
            SettingsPreferenceFragment host) {
        this(context, host, KEY_UNLOCK_SET_OR_CHANGE_PROFILE);
    }

    public ChangeProfileScreenLockPreferenceController(Context context,
            SettingsPreferenceFragment host, String key) {
        super(context, host);
        this.mPreferenceKey = key;
    }

    public boolean isAvailable() {
        if (mProfileChallengeUserId == UserHandle.USER_NULL ||
                !mUm.isManagedProfile(mProfileChallengeUserId)) {
            return false;
        }
        if (!mLockPatternUtils.isSecure(mProfileChallengeUserId)) {
            return true;
        }
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUm, mProfileChallengeUserId)) {
            return false;
        }
        final Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .launch();

        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference, mProfileChallengeUserId);

        if (!mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)) {
            final String summary = mContext.getString(
                    R.string.lock_settings_profile_unified_summary);
            mPreference.setSummary(summary);
            mPreference.setEnabled(false);
        } else {
            // PO may disallow to change profile password, and the profile's password is
            // separated from screen lock password. Disable profile specific "Screen lock" menu.
            disableIfPasswordQualityManaged(mProfileChallengeUserId);
        }
    }
}
