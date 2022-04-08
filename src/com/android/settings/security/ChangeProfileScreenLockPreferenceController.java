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
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;

public class ChangeProfileScreenLockPreferenceController extends
        ChangeScreenLockPreferenceController {

    private static final String KEY_UNLOCK_SET_OR_CHANGE_PROFILE = "unlock_set_or_change_profile";

    public ChangeProfileScreenLockPreferenceController(Context context,
            SecuritySettings host) {
        super(context, host);
    }

    public boolean isAvailable() {
        if (mProfileChallengeUserId == UserHandle.USER_NULL ||
                !mLockPatternUtils.isSeparateProfileChallengeAllowed(mProfileChallengeUserId)) {
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
        return KEY_UNLOCK_SET_OR_CHANGE_PROFILE;
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
                .setTitleRes(R.string.lock_settings_picker_title_profile)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
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
