/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.Nullable;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;

/**
 * Business logic for {@link SetNewPasswordActivity}.
 *
 * <p>On devices that supports fingerprint, this controller directs the user to configure
 * fingerprint + a backup password if the device admin allows fingerprint for keyguard and
 * the user has never configured a fingerprint before.
 */
final class SetNewPasswordController {

    interface Ui {
        /** Starts the {@link ChooseLockGeneric} activity with the given extras. */
        void launchChooseLock(@Nullable Bundle chooseLockFingerprintExtras);
    }

    private final int mCurrentUserId;
    private final PackageManager mPackageManager;
    @Nullable private final FingerprintManager mFingerprintManager;
    private final DevicePolicyManager mDevicePolicyManager;
    private final Ui mUi;

    public SetNewPasswordController(Context context, Ui ui) {
        this(context.getUserId(),
                context.getPackageManager(),
                (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE),
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE),
                ui);
    }

    @VisibleForTesting
    SetNewPasswordController(
            int currentUserId,
            PackageManager packageManager,
            FingerprintManager fingerprintManager,
            DevicePolicyManager devicePolicyManager,
            Ui ui) {
        mCurrentUserId = currentUserId;
        mPackageManager = checkNotNull(packageManager);
        mFingerprintManager = fingerprintManager;
        mDevicePolicyManager = checkNotNull(devicePolicyManager);
        mUi = checkNotNull(ui);
    }

    /**
     * Dispatches the set new password intent to the correct activity that handles it.
     */
    public void dispatchSetNewPasswordIntent() {
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                && mFingerprintManager != null
                && mFingerprintManager.isHardwareDetected()
                && !mFingerprintManager.hasEnrolledFingerprints()
                && !isFingerprintDisabledByAdmin()) {
            mUi.launchChooseLock(getFingerprintChooseLockExtras());
        } else {
            mUi.launchChooseLock(null);
        }
    }

    private Bundle getFingerprintChooseLockExtras() {
        Bundle chooseLockExtras = new Bundle();
        if (mFingerprintManager != null) {
            long challenge = mFingerprintManager.preEnroll();
            chooseLockExtras.putInt(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            chooseLockExtras.putBoolean(
                    ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
            chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
            chooseLockExtras.putLong(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
            chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
            if (mCurrentUserId != UserHandle.USER_NULL) {
                chooseLockExtras.putInt(Intent.EXTRA_USER_ID, mCurrentUserId);
            }
        }
        return chooseLockExtras;
    }

    private boolean isFingerprintDisabledByAdmin() {
        int disabledFeatures = mDevicePolicyManager.getKeyguardDisabledFeatures(
                null, mCurrentUserId);
        return (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0;
    }
}
