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

package com.android.settings.safetycenter;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricNavigationUtils;
import com.android.settings.biometrics.combination.CombinedBiometricStatusUtils;
import com.android.settings.biometrics.face.FaceStatusUtils;
import com.android.settings.biometrics.fingerprint.FingerprintStatusUtils;
import com.android.settingslib.RestrictedLockUtils;

/** Combined Biometrics Safety Source for Safety Center. */
public final class BiometricsSafetySource {

    public static final String SAFETY_SOURCE_ID = "AndroidBiometrics";
    private static final int REQUEST_CODE_COMBINED_BIOMETRIC_SETTING = 10;
    private static final int REQUEST_CODE_FACE_SETTING = 20;
    private static final int REQUEST_CODE_FINGERPRINT_SETTING = 30;

    private BiometricsSafetySource() {
    }

    /** Sets biometric safety data for Safety Center. */
    public static void setSafetySourceData(Context context, SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        final int userId = UserHandle.myUserId();

        final BiometricNavigationUtils biometricNavigationUtils = new BiometricNavigationUtils(
                userId);
        final CombinedBiometricStatusUtils combinedBiometricStatusUtils =
                new CombinedBiometricStatusUtils(context, userId);

        if (combinedBiometricStatusUtils.isAvailable()) {
            final RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    combinedBiometricStatusUtils.getDisablingAdmin();
            setBiometricSafetySourceData(context,
                    context.getString(R.string.security_settings_biometric_preference_title),
                    combinedBiometricStatusUtils.getSummary(),
                    createPendingIntent(context,
                            biometricNavigationUtils.getBiometricSettingsIntent(context,
                                    combinedBiometricStatusUtils.getSettingsClassName(),
                                    disablingAdmin, Bundle.EMPTY),
                            REQUEST_CODE_COMBINED_BIOMETRIC_SETTING),
                    disablingAdmin == null /* enabled */,
                    combinedBiometricStatusUtils.hasEnrolled(),
                    safetyEvent);
            return;
        }

        final FaceManager faceManager = Utils.getFaceManagerOrNull(context);
        final FaceStatusUtils faceStatusUtils = new FaceStatusUtils(context, faceManager, userId);

        if (faceStatusUtils.isAvailable()) {
            final RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    faceStatusUtils.getDisablingAdmin();
            setBiometricSafetySourceData(context,
                    context.getString(R.string.security_settings_face_preference_title),
                    faceStatusUtils.getSummary(),
                    createPendingIntent(context,
                            biometricNavigationUtils.getBiometricSettingsIntent(context,
                                    faceStatusUtils.getSettingsClassName(), disablingAdmin,
                                    Bundle.EMPTY),
                            REQUEST_CODE_FACE_SETTING),
                    disablingAdmin == null /* enabled */,
                    faceStatusUtils.hasEnrolled(),
                    safetyEvent);

            return;
        }

        final FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(context);
        final FingerprintStatusUtils fingerprintStatusUtils = new FingerprintStatusUtils(context,
                fingerprintManager, userId);

        if (fingerprintStatusUtils.isAvailable()) {
            final RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    fingerprintStatusUtils.getDisablingAdmin();
            setBiometricSafetySourceData(context,
                    context.getString(R.string.security_settings_fingerprint_preference_title),
                    fingerprintStatusUtils.getSummary(),
                    createPendingIntent(context,
                            biometricNavigationUtils.getBiometricSettingsIntent(context,
                                    fingerprintStatusUtils.getSettingsClassName(), disablingAdmin,
                                    Bundle.EMPTY),
                            REQUEST_CODE_FINGERPRINT_SETTING),
                    disablingAdmin == null /* enabled */,
                    fingerprintStatusUtils.hasEnrolled(),
                    safetyEvent);
        }
    }

    /** Notifies Safety Center of a change in biometrics settings. */
    public static void onBiometricsChanged(Context context) {
        setSafetySourceData(
                context,
                new SafetyEvent.Builder(SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build()
        );
    }

    private static void setBiometricSafetySourceData(Context context, String title, String summary,
            PendingIntent pendingIntent, boolean enabled, boolean hasEnrolled,
            SafetyEvent safetyEvent) {
        final int severityLevel =
                enabled && hasEnrolled ? SafetySourceData.SEVERITY_LEVEL_INFORMATION
                        : SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED;

        final SafetySourceStatus status = new SafetySourceStatus.Builder(title, summary,
                severityLevel).setPendingIntent(pendingIntent).setEnabled(enabled).build();
        final SafetySourceData safetySourceData =
                new SafetySourceData.Builder().setStatus(status).build();

        SafetyCenterManagerWrapper.get().setSafetySourceData(
                context, SAFETY_SOURCE_ID, safetySourceData, safetyEvent);
    }

    private static PendingIntent createPendingIntent(Context context, Intent intent,
            int requestCode) {
        return PendingIntent
                .getActivity(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE);
    }
}
