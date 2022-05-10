/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.ParentalControlsUtilsInternal;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.RestrictedLockUtils;

/**
 * Utilities for things at the cross-section of biometrics and parental controls. For example,
 * determining if parental consent is required, determining which strings should be shown, etc.
 */
public class ParentalControlsUtils {

    private static final String TAG = "ParentalControlsUtils";

    /**
     * Public version that enables test paths, see
     * {@link android.hardware.biometrics.ParentalControlsUtilsInternal#getTestComponentName}
     * @return non-null EnforcedAdmin if parental consent is required
     */
    public static RestrictedLockUtils.EnforcedAdmin parentConsentRequired(@NonNull Context context,
            @BiometricAuthenticator.Modality int modality) {

        final int userId = UserHandle.myUserId();
        final UserHandle userHandle = new UserHandle(userId);
        final ComponentName testComponentName = ParentalControlsUtilsInternal.getTestComponentName(
                context, userId);
        if (testComponentName != null) {
            Log.d(TAG, "Requiring consent for test flow");
            return new RestrictedLockUtils.EnforcedAdmin(testComponentName,
                    UserManager.DISALLOW_BIOMETRIC, userHandle);
        }

        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        return parentConsentRequiredInternal(dpm, modality, userHandle);
    }

    /**
     * Internal testable version.
     * @return non-null EnforcedAdmin if parental consent is required
     */
    @Nullable
    @VisibleForTesting
    static RestrictedLockUtils.EnforcedAdmin parentConsentRequiredInternal(
            @NonNull DevicePolicyManager dpm, @BiometricAuthenticator.Modality int modality,
            @NonNull UserHandle userHandle) {
        if (ParentalControlsUtilsInternal.parentConsentRequired(dpm, modality,
                userHandle)) {
            final ComponentName cn =
                    ParentalControlsUtilsInternal.getSupervisionComponentName(dpm, userHandle);
            return new RestrictedLockUtils.EnforcedAdmin(cn, UserManager.DISALLOW_BIOMETRIC,
                    userHandle);
        } else {
            return null;
        }
    }
}
