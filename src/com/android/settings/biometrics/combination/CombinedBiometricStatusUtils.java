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

package com.android.settings.biometrics.combination;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.utils.StringUtil;

/**
 * Utilities for combined biometric details shared between Security Settings and Safety Center.
 */
public class CombinedBiometricStatusUtils {

    private final int mUserId;
    private final Context mContext;
    @Nullable
    FingerprintManager mFingerprintManager;
    @Nullable
    FaceManager mFaceManager;

    public CombinedBiometricStatusUtils(Context context, int userId) {
        mContext = context;
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        mUserId = userId;
    }

    /**
     * Returns whether the combined biometric settings entity should be shown.
     */
    public boolean isAvailable() {
        return Utils.hasFingerprintHardware(mContext) && Utils.hasFaceHardware(mContext);
    }

    /**
     * Returns whether at least one face template or fingerprint has been enrolled.
     */
    public boolean hasEnrolled() {
        return hasEnrolledFingerprints() || hasEnrolledFace();
    }

    /**
     * Returns the {@link EnforcedAdmin} in case parental consent is required to change both
     * face and fingerprint settings.
     *
     * @return null if either face or fingerprint settings do not require a parental consent.
     */
    public EnforcedAdmin getDisablingAdmin() {
        // This controller currently is shown if fingerprint&face exist on the device. If this
        // changes in the future, the modalities passed into the below will need to be updated.

        final EnforcedAdmin faceAdmin = ParentalControlsUtils
                .parentConsentRequired(mContext, BiometricAuthenticator.TYPE_FACE);
        final EnforcedAdmin fpAdmin = ParentalControlsUtils
                .parentConsentRequired(mContext, BiometricAuthenticator.TYPE_FINGERPRINT);

        final boolean faceConsentRequired = faceAdmin != null;
        final boolean fpConsentRequired = fpAdmin != null;

        // Result is only required if all modalities require consent.
        // If the admins are non-null, they are actually always the same.
        return faceConsentRequired && fpConsentRequired ? faceAdmin : null;
    }

    /**
     * Returns the title of combined biometric settings entity.
     */
    public String getTitle() {
        UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager != null && userManager.isProfile()) {
            return mContext.getString(R.string.security_settings_work_biometric_preference_title);
        } else {
            return mContext.getString(R.string.security_settings_biometric_preference_title);
        }
    }

    /**
     * Returns the summary of combined biometric settings entity.
     */
    public String getSummary() {
        final int numFingerprintsEnrolled = mFingerprintManager != null
                ? mFingerprintManager.getEnrolledFingerprints(mUserId).size() : 0;
        final boolean faceEnrolled = hasEnrolledFace();

        if (faceEnrolled && numFingerprintsEnrolled > 1) {
            return mContext.getString(
                    R.string.security_settings_biometric_preference_summary_both_fp_multiple);
        } else if (faceEnrolled && numFingerprintsEnrolled == 1) {
            return mContext.getString(
                    R.string.security_settings_biometric_preference_summary_both_fp_single);
        } else if (faceEnrolled) {
            return mContext.getString(R.string.security_settings_face_preference_summary);
        } else if (numFingerprintsEnrolled > 0) {
            return StringUtil.getIcuPluralsString(mContext, numFingerprintsEnrolled,
                    R.string.security_settings_fingerprint_preference_summary);
        } else {
            return mContext.getString(
                    R.string.security_settings_biometric_preference_summary_none_enrolled);
        }
    }

    private boolean hasEnrolledFingerprints() {
        return mFingerprintManager != null && mFingerprintManager.hasEnrolledFingerprints(mUserId);
    }

    private boolean hasEnrolledFace() {
        return mFaceManager != null && mFaceManager.hasEnrolledTemplates(mUserId);
    }

    /**
     * Returns the class name of the Settings page corresponding to combined biometric settings
     * based on the current user.
     */
    public String getSettingsClassNameBasedOnUser() {
        UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager != null && userManager.isProfile()) {
            return getProfileSettingsClassName();
        } else {
            return getSettingsClassName();
        }
    }

    /**
     * Returns the class name of the Settings page corresponding to combined biometric settings.
     */
    public String getSettingsClassName() {
        return Settings.CombinedBiometricSettingsActivity.class.getName();
    }

    /**
     * Returns the class name of the Settings page corresponding to combined biometric settings
     * for work profile.
     */
    public String getProfileSettingsClassName() {
        return Settings.CombinedBiometricProfileSettingsActivity.class.getName();
    }

    /**
     * Returns the class name of the Settings page corresponding to combined biometric settings for
     * Private profile.
     */
    public String getPrivateProfileSettingsClassName() {
        return Settings.PrivateSpaceBiometricSettingsActivity.class.getName();
    }
}
