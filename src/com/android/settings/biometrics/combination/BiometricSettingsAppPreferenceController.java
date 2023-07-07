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
package com.android.settings.biometrics.combination;

import static android.provider.Settings.Secure.BIOMETRIC_APP_ENABLED;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings;

import com.android.settings.Utils;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

/**
 * Preference controller that controls whether the biometrics authentication to be used in apps.
 */
public class BiometricSettingsAppPreferenceController extends TogglePreferenceController {
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = ON;

    private int mUserId;
    private FaceManager mFaceManager;
    private FingerprintManager mFingerprintManager;

    public BiometricSettingsAppPreferenceController(Context context, String key) {
        super(context, key);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
    }

    protected EnforcedAdmin getRestrictingAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(mContext,
                DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS, mUserId);
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(), BIOMETRIC_APP_ENABLED,
                DEFAULT, mUserId) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(), BIOMETRIC_APP_ENABLED,
                isChecked ? ON : OFF, mUserId);
    }

    @Override
    public int getAvailabilityStatus() {
        final ActiveUnlockStatusUtils activeUnlockStatusUtils =
                new ActiveUnlockStatusUtils(mContext);
        if (!Utils.isMultipleBiometricsSupported(mContext)
                && !activeUnlockStatusUtils.isAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mFaceManager == null || mFingerprintManager == null) {
            return AVAILABLE_UNSEARCHABLE;
        }
        // This preference will be available only if the user has registered either face auth
        // or fingerprint.
        final boolean hasFaceEnrolledUser = mFaceManager.hasEnrolledTemplates(mUserId);
        final boolean hasFingerprintEnrolledUser =
                mFingerprintManager.hasEnrolledTemplates(mUserId);
        if (hasFaceEnrolledUser || hasFingerprintEnrolledUser) {
            return AVAILABLE;
        } else {
            return AVAILABLE_UNSEARCHABLE;
        }
    }

    @Override
    public final boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
