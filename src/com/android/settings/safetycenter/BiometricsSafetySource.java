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

    public static final String SAFETY_SOURCE_ID = "Biometrics";

    private BiometricsSafetySource() {
    }

    /** Sends biometric safety data to Safety Center. */
    public static void sendSafetyData(Context context) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        final BiometricNavigationUtils biometricNavigationUtils = new BiometricNavigationUtils();
        final CombinedBiometricStatusUtils combinedBiometricStatusUtils =
                new CombinedBiometricStatusUtils(context);

        if (combinedBiometricStatusUtils.isAvailable()) {
            final RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    combinedBiometricStatusUtils.getDisablingAdmin();
            sendBiometricSafetySourceData(context,
                    context.getString(R.string.security_settings_biometric_preference_title),
                    combinedBiometricStatusUtils.getSummary(),
                    biometricNavigationUtils.getBiometricSettingsIntent(context,
                            combinedBiometricStatusUtils.getSettingsClassName(), disablingAdmin,
                            Bundle.EMPTY),
                    disablingAdmin == null /* enabled */);
            return;
        }

        final FaceManager faceManager = Utils.getFaceManagerOrNull(context);
        final FaceStatusUtils faceStatusUtils = new FaceStatusUtils(context, faceManager);

        if (faceStatusUtils.isAvailable()) {
            final RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    faceStatusUtils.getDisablingAdmin();
            sendBiometricSafetySourceData(context,
                    context.getString(R.string.security_settings_face_preference_title),
                    faceStatusUtils.getSummary(),
                    biometricNavigationUtils.getBiometricSettingsIntent(context,
                            faceStatusUtils.getSettingsClassName(), disablingAdmin,
                            Bundle.EMPTY),
                    disablingAdmin == null /* enabled */);
            return;
        }

        final FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(context);
        final FingerprintStatusUtils fingerprintStatusUtils = new FingerprintStatusUtils(context,
                fingerprintManager);

        if (fingerprintStatusUtils.isAvailable()) {
            final RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    fingerprintStatusUtils.getDisablingAdmin();
            sendBiometricSafetySourceData(context,
                    context.getString(R.string.security_settings_fingerprint_preference_title),
                    fingerprintStatusUtils.getSummary(),
                    biometricNavigationUtils.getBiometricSettingsIntent(context,
                            fingerprintStatusUtils.getSettingsClassName(), disablingAdmin,
                            Bundle.EMPTY),
                    disablingAdmin == null /* enabled */);
        }
    }

    private static void sendBiometricSafetySourceData(Context context, String title, String summary,
            Intent clickIntent, boolean enabled) {
        final PendingIntent pendingIntent = createPendingIntent(context, clickIntent);

        final SafetySourceStatus status = new SafetySourceStatus.Builder(title, summary,
                SafetySourceStatus.STATUS_LEVEL_NONE, pendingIntent)
                .setEnabled(enabled).build();
        final SafetySourceData safetySourceData = new SafetySourceData.Builder(SAFETY_SOURCE_ID)
                .setStatus(status).build();

        SafetyCenterManagerWrapper.get().sendSafetyCenterUpdate(context, safetySourceData);
    }

    private static PendingIntent createPendingIntent(Context context, Intent intent) {
        return PendingIntent
                .getActivity(
                        context,
                        0 /* requestCode */,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE);
    }
}
