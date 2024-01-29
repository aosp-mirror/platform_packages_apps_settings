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

package com.android.settings.privatespace.onelock;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;
import static com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.transition.SettingsTransitionHelper;


/** Represents the preference controller for changing private space lock. */
public class PrivateSpaceLockController extends AbstractPreferenceController {
    private static final String TAG = "PrivateSpaceLockContr";
    private static final String KEY_CHANGE_PROFILE_LOCK =
            "change_private_space_lock";

    private final SettingsPreferenceFragment mHost;
    private final UserManager mUserManager;
    private final LockPatternUtils mLockPatternUtils;
    private final int mProfileUserId;

    public PrivateSpaceLockController(Context context, SettingsPreferenceFragment host) {
        super(context);
        mUserManager = context.getSystemService(UserManager.class);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mHost = host;
        UserHandle privateProfileHandle = PrivateSpaceMaintainer.getInstance(context)
                .getPrivateProfileHandle();
        if (privateProfileHandle != null) {
            mProfileUserId = privateProfileHandle.getIdentifier();
        } else {
            mProfileUserId = -1;
            Log.e(TAG, "Private profile user handle is not expected to be null.");
        }
    }

    @Override
    public boolean isAvailable() {
        return android.os.Flags.allowPrivateProfile();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CHANGE_PROFILE_LOCK;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        //Checks if the profile is in quiet mode and show a dialog to unpause the profile.
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUserManager,
                mProfileUserId)) {
            return false;
        }
        final Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileUserId);
        extras.putBoolean(HIDE_INSECURE_OPTIONS, true);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
                .setExtras(extras)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .launch();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileUserId)) {
            preference.setSummary(
                    mContext.getString(getCredentialTypeResId(mProfileUserId)));
            preference.setEnabled(true);
        } else {
            preference.setSummary(mContext.getString(
                    R.string.lock_settings_profile_unified_summary));
            preference.setEnabled(false);
        }
    }

    private int getCredentialTypeResId(int userId) {
        int credentialType = mLockPatternUtils.getCredentialTypeForUser(userId);
        switch (credentialType) {
            case CREDENTIAL_TYPE_PATTERN :
                return R.string.unlock_set_unlock_mode_pattern;
            case CREDENTIAL_TYPE_PIN:
                return R.string.unlock_set_unlock_mode_pin;
            case CREDENTIAL_TYPE_PASSWORD:
                return R.string.unlock_set_unlock_mode_password;
            default:
                // This is returned for CREDENTIAL_TYPE_NONE
                return R.string.unlock_set_unlock_mode_off;
        }
    }
}
