/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;

public class FaceSettingsLockscreenBypassPreferenceController
        extends FaceSettingsPreferenceController {

    @VisibleForTesting
    protected FaceManager mFaceManager;
    private UserManager mUserManager;

    public FaceSettingsLockscreenBypassPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            mFaceManager = context.getSystemService(FaceManager.class);
        }

        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public boolean isChecked() {
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            return false;
        } else if (getRestrictingAdmin() != null) {
            return false;
        }
        int defaultValue = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthDismissesKeyguard) ? 1 : 0;
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, defaultValue, getUserHandle()) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, isChecked ? 1 : 0, getUserHandle());
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if (getRestrictingAdmin() != null) {
            preference.setEnabled(false);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        // When the device supports multiple biometrics auth, this preference won't be shown
        // in face unlock category.
        if (Utils.isMultipleBiometricsSupported(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mUserManager.isManagedProfile(getUserId())) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (mFaceManager != null && mFaceManager.isHardwareDetected()) {
            return mFaceManager.hasEnrolledTemplates(getUserId())
                    ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    private int getUserHandle() {
        return UserHandle.of(getUserId()).getIdentifier();
    }
}
