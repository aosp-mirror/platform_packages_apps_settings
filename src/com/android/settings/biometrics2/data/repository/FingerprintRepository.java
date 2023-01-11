/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.data.repository;

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.List;

/**
 * This repository is used to call all APIs in {@link FingerprintManager}
 */
public class FingerprintRepository {

    @NonNull private final FingerprintManager mFingerprintManager;

    public FingerprintRepository(@NonNull FingerprintManager fingerprintManager) {
        mFingerprintManager = fingerprintManager;
    }

    /**
     * The first sensor type is UDFPS sensor or not
     */
    public boolean canAssumeUdfps() {
        FingerprintSensorPropertiesInternal prop = getFirstFingerprintSensorPropertiesInternal();
        return prop != null && prop.isAnyUdfpsType();
    }

    /**
     * The first sensor type is Side fps sensor or not
     */
    public boolean canAssumeSfps() {
        FingerprintSensorPropertiesInternal prop = getFirstFingerprintSensorPropertiesInternal();
        return prop != null && prop.isAnySidefpsType();
    }

    /**
     * Get max possible number of fingerprints for a user
     */
    public int getMaxFingerprints() {
        FingerprintSensorPropertiesInternal prop = getFirstFingerprintSensorPropertiesInternal();
        return prop != null ? prop.maxEnrollmentsPerUser : 0;
    }

    /**
     * Get number of fingerprints that this user enrolled.
     */
    public int getNumOfEnrolledFingerprintsSize(int userId) {
        final List<Fingerprint> list = mFingerprintManager.getEnrolledFingerprints(userId);
        return list != null ? list.size() : 0;
    }

    /**
     * Get maximum possible fingerprints in setup wizard flow
     */
    public int getMaxFingerprintsInSuw(@NonNull Resources resources) {
        return resources.getInteger(R.integer.suw_max_fingerprints_enrollable);
    }

    @Nullable
    private FingerprintSensorPropertiesInternal getFirstFingerprintSensorPropertiesInternal() {
        // TODO(b/264827022) use API addAuthenticatorsRegisteredCallback
        final List<FingerprintSensorPropertiesInternal> props =
                mFingerprintManager.getSensorPropertiesInternal();
        return props.size() > 0 ? props.get(0) : null;
    }

    /**
     * Call FingerprintManager to generate challenge for first sensor
     */
    public void generateChallenge(int userId,
            @NonNull FingerprintManager.GenerateChallengeCallback callback) {
        mFingerprintManager.generateChallenge(userId, callback);
    }

    /**
     * Get parental consent required or not during enrollment process
     */
    public boolean isParentalConsentRequired(@NonNull Context context) {
        return ParentalControlsUtils.parentConsentRequired(context, TYPE_FINGERPRINT) != null;
    }

    /**
     * Get fingerprint is disable by admin or not
     */
    public boolean isDisabledByAdmin(@NonNull Context context, int userId) {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                context, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, userId) != null;
    }
}
