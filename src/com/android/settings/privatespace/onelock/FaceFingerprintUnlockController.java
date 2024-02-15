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

import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.combination.CombinedBiometricStatusPreferenceController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settingslib.core.lifecycle.Lifecycle;

/** Represents the preference controller to enroll biometrics for private space lock. */
public class FaceFingerprintUnlockController extends CombinedBiometricStatusPreferenceController {
    private static final String TAG = "PSBiometricCtrl";
    private static final String KEY_SET_UNSET_FACE_FINGERPRINT = "private_space_biometrics";
    private final int mProfileUserId;

    public FaceFingerprintUnlockController(Context context, Lifecycle lifecycle) {
        super(context, KEY_SET_UNSET_FACE_FINGERPRINT, lifecycle);
        mProfileUserId = getUserId();
    }

    protected boolean isUserSupported() {
        return android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enableBiometricsToUnlockPrivateSpace()
                && mProfileUserId != UserHandle.USER_NULL;
    }

    @Override
    protected int getUserId() {
        UserHandle privateProfileHandle =
                PrivateSpaceMaintainer.getInstance(mContext).getPrivateProfileHandle();
        if (privateProfileHandle != null) {
            return privateProfileHandle.getIdentifier();
        } else {
            Log.e(TAG, "Private profile user handle is not expected to be null.");
        }
        return UserHandle.USER_NULL;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SET_UNSET_FACE_FINGERPRINT;
    }

    @Override
    protected String getSettingsClassName() {
        return mCombinedBiometricStatusUtils.getPrivateProfileSettingsClassName();
    }

    @Override
    public void updateState(Preference preference) {
        if (mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileUserId)) {
            super.updateState(preference);
            preference.setEnabled(true);
        } else {
            Utils.removeEnrolledFaceForUser(mContext, getUserId());
            Utils.removeEnrolledFingerprintForUser(mContext, getUserId());
            preference.setSummary(
                    mContext.getString(R.string.lock_settings_profile_unified_summary));
            preference.setEnabled(false);
        }
    }
}
