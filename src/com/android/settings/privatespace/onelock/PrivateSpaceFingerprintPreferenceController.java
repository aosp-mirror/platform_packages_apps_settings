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

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.combination.BiometricFingerprintStatusPreferenceController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

public class PrivateSpaceFingerprintPreferenceController
        extends BiometricFingerprintStatusPreferenceController {
    private static final String TAG = "PrivateSpaceFingerCtrl";

    public PrivateSpaceFingerprintPreferenceController(Context context, String key) {
        super(context, key);
    }

    public PrivateSpaceFingerprintPreferenceController(
            Context context, String key, Lifecycle lifecycle) {
        super(context, key, lifecycle);
    }

    @Override
    protected boolean isUserSupported() {
        return android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enableBiometricsToUnlockPrivateSpace()
                && getUserId() != UserHandle.USER_NULL;
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
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile()
                        && android.multiuser.Flags.enableBiometricsToUnlockPrivateSpace()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        if (mLockPatternUtils.isSeparateProfileChallengeEnabled(getUserId())) {
            super.updateState(preference);
            preference.setEnabled(true);
        } else {
            Utils.removeEnrolledFingerprintForUser(mContext, getUserId());
            preference.setSummary(
                    mContext.getString(R.string.lock_settings_profile_unified_summary));
            preference.setEnabled(false);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (!Utils.isMultipleBiometricsSupported(mContext)) {
            preference.setTitle(R.string.private_space_fingerprint_title);
        }
    }
}
