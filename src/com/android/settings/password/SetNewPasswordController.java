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

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FACE;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.Utils;

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
        void launchChooseLock(Bundle chooseLockFingerprintExtras);
    }

    /**
     * Which user is setting new password.
     */
    private final int mTargetUserId;
    private final PackageManager mPackageManager;
    @Nullable
    private final FingerprintManager mFingerprintManager;
    @Nullable
    private final FaceManager mFaceManager;
    private final DevicePolicyManager mDevicePolicyManager;
    private final Ui mUi;

    public static SetNewPasswordController create(Context context, Ui ui, Intent intent,
            IBinder activityToken) {
        // Trying to figure out which user is setting new password. If it is
        // ACTION_SET_NEW_PARENT_PROFILE_PASSWORD, it is the current user to set
        // new password. Otherwise, it is the user who starts this activity
        // setting new password.
        final int userId;
        if (ACTION_SET_NEW_PASSWORD.equals(intent.getAction())) {
            userId = Utils.getSecureTargetUser(activityToken,
                    UserManager.get(context), null, intent.getExtras()).getIdentifier();
        } else {
            userId = ActivityManager.getCurrentUser();
        }
        // Create a wrapper of FingerprintManager for testing, see IFingerPrintManager for details.
        final FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(context);
        final FaceManager faceManager = Utils.getFaceManagerOrNull(context);
        return new SetNewPasswordController(userId,
                context.getPackageManager(),
                fingerprintManager, faceManager,
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE), ui);
    }

    @VisibleForTesting
    SetNewPasswordController(
            int targetUserId,
            PackageManager packageManager,
            FingerprintManager fingerprintManager,
            FaceManager faceManager,
            DevicePolicyManager devicePolicyManager,
            Ui ui) {
        mTargetUserId = targetUserId;
        mPackageManager = checkNotNull(packageManager);
        mFingerprintManager = fingerprintManager;
        mFaceManager = faceManager;
        mDevicePolicyManager = checkNotNull(devicePolicyManager);
        mUi = checkNotNull(ui);
    }

    /**
     * Dispatches the set new password intent to the correct activity that handles it.
     */
    public void dispatchSetNewPasswordIntent() {
        final Bundle extras;

        final boolean hasFeatureFingerprint = mPackageManager
                .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        final boolean hasFeatureFace = mPackageManager
                .hasSystemFeature(PackageManager.FEATURE_FACE);

        final boolean shouldShowFingerprintEnroll = mFingerprintManager != null
                && mFingerprintManager.isHardwareDetected()
                && !mFingerprintManager.hasEnrolledFingerprints(mTargetUserId)
                && !isFingerprintDisabledByAdmin();
        final boolean shouldShowFaceEnroll = mFaceManager != null
                && mFaceManager.isHardwareDetected()
                && !mFaceManager.hasEnrolledTemplates(mTargetUserId)
                && !isFaceDisabledByAdmin();

        if (hasFeatureFace && shouldShowFaceEnroll
                && hasFeatureFingerprint && shouldShowFingerprintEnroll) {
            extras = getBiometricChooseLockExtras();
        } else if (hasFeatureFace && shouldShowFaceEnroll) {
            extras = getFaceChooseLockExtras();
        } else if (hasFeatureFingerprint && shouldShowFingerprintEnroll) {
            extras = getFingerprintChooseLockExtras();
        } else {
            extras = new Bundle();
        }

        // No matter we show fingerprint options or not, we should tell the next activity which
        // user is setting new password.
        extras.putInt(Intent.EXTRA_USER_ID, mTargetUserId);
        mUi.launchChooseLock(extras);
    }

    private Bundle getBiometricChooseLockExtras() {
        Bundle chooseLockExtras = new Bundle();
        chooseLockExtras.putBoolean(
                ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true);
        chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
        chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, true);
        return chooseLockExtras;
    }

    private Bundle getFingerprintChooseLockExtras() {
        Bundle chooseLockExtras = new Bundle();
        chooseLockExtras.putBoolean(
                ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true);
        chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
        chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
        return chooseLockExtras;
    }

    private Bundle getFaceChooseLockExtras() {
        Bundle chooseLockExtras = new Bundle();
        chooseLockExtras.putBoolean(
                ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true);
        chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
        chooseLockExtras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, true);
        return chooseLockExtras;
    }

    private boolean isFingerprintDisabledByAdmin() {
        int disabledFeatures =
                mDevicePolicyManager.getKeyguardDisabledFeatures(null, mTargetUserId);
        return (disabledFeatures & KEYGUARD_DISABLE_FINGERPRINT) != 0;
    }

    private boolean isFaceDisabledByAdmin() {
        int disabledFeatures =
                mDevicePolicyManager.getKeyguardDisabledFeatures(null, mTargetUserId);
        return (disabledFeatures & KEYGUARD_DISABLE_FACE) != 0;
    }
}
