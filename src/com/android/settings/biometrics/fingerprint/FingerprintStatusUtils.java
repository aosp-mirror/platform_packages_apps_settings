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

package com.android.settings.biometrics.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.utils.StringUtil;

/**
 * Utilities for fingerprint details shared between Security Settings and Safety Center.
 */
public class FingerprintStatusUtils {

    private final int mUserId;
    private final Context mContext;
    private final FingerprintManager mFingerprintManager;

    public FingerprintStatusUtils(Context context, FingerprintManager fingerprintManager,
            int userId) {
        mContext = context;
        mFingerprintManager = fingerprintManager;
        mUserId = userId;
    }

    /**
     * Returns whether the fingerprint settings entity should be shown.
     */
    public boolean isAvailable() {
        return !Utils.isMultipleBiometricsSupported(mContext)
                && Utils.hasFingerprintHardware(mContext);
    }

    /**
     * Returns the {@link EnforcedAdmin} if parental consent is required to change face settings.
     *
     * @return null if face settings does not require a parental consent.
     */
    public EnforcedAdmin getDisablingAdmin() {
        return ParentalControlsUtils.parentConsentRequired(
                mContext, BiometricAuthenticator.TYPE_FINGERPRINT);
    }
    /**
     * Returns the title of fingerprint settings entity.
     */
    public String getTitle() {
        UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager != null && userManager.isProfile()) {
            return mContext.getString(
                    Utils.isPrivateProfile(mUserId, mContext)
                            ? R.string.private_space_fingerprint_unlock_title
                            : R.string.security_settings_work_fingerprint_preference_title);
        } else {
            return mContext.getString(R.string.security_settings_fingerprint_preference_title);
        }
    }

    /**
     * Returns the summary of fingerprint settings entity.
     */
    public String getSummary() {
        if (shouldShowDisabledByAdminStr()) {
            return mContext.getString(
                    com.android.settingslib.widget.restricted.R.string.disabled_by_admin);
        }
        if (hasEnrolled()) {
            final int numEnrolled = mFingerprintManager.getEnrolledFingerprints(mUserId).size();
            return StringUtil.getIcuPluralsString(mContext, numEnrolled,
                    R.string.security_settings_fingerprint_preference_summary);
        } else {
            return mContext.getString(
                    R.string.security_settings_fingerprint_preference_summary_none);
        }
    }

    /**
     * Returns the class name of the Settings page corresponding to fingerprint settings.
     */
    public String getSettingsClassName() {
        return FingerprintSettings.class.getName();
    }

    /**
     * Returns whether at least one fingerprint has been enrolled.
     */
    public boolean hasEnrolled() {
        return mFingerprintManager.hasEnrolledFingerprints(mUserId);
    }

    /**
     * Indicates if the fingerprint feature should show the "Disabled by Admin" string.
     */
    private boolean shouldShowDisabledByAdminStr() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                mContext, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId) != null;
    }
}
