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

import static com.android.settings.safetycenter.BiometricSourcesUtils.REQUEST_CODE_COMBINED_BIOMETRIC_SETTING;
import static com.android.settings.safetycenter.BiometricSourcesUtils.REQUEST_CODE_FACE_SETTING;
import static com.android.settings.safetycenter.BiometricSourcesUtils.REQUEST_CODE_FINGERPRINT_SETTING;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.safetycenter.SafetyEvent;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricNavigationUtils;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;
import com.android.settings.biometrics.combination.CombinedBiometricStatusUtils;
import com.android.settings.biometrics.face.FaceStatusUtils;
import com.android.settings.biometrics.fingerprint.FingerprintStatusUtils;
import com.android.settingslib.RestrictedLockUtils;

/** Combined Biometrics Safety Source for Safety Center. */
public final class BiometricsSafetySource {

    public static final String SAFETY_SOURCE_ID = "AndroidBiometrics";

    private BiometricsSafetySource() {}

    /** Sets biometric safety data for Safety Center. */
    public static void setSafetySourceData(Context context, SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        UserHandle userHandle = Process.myUserHandle();
        int userId = userHandle.getIdentifier();
        UserManager userManager = UserManager.get(context);
        UserHandle profileParentUserHandle = userManager.getProfileParent(userHandle);
        if (profileParentUserHandle == null) {
            profileParentUserHandle = userHandle;
        }
        Context profileParentContext = context.createContextAsUser(profileParentUserHandle, 0);
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()
                && userManager.isPrivateProfile()) {
            // SC always expects a response from the source if the broadcast has been sent for this
            // source, therefore, we need to send a null SafetySourceData.
            SafetyCenterManagerWrapper.get()
                    .setSafetySourceData(
                            context, SAFETY_SOURCE_ID, /* safetySourceData= */ null, safetyEvent);
            return;
        }

        BiometricNavigationUtils biometricNavigationUtils = new BiometricNavigationUtils(userId);
        CombinedBiometricStatusUtils combinedBiometricStatusUtils =
                new CombinedBiometricStatusUtils(context, userId);
        ActiveUnlockStatusUtils activeUnlockStatusUtils = new ActiveUnlockStatusUtils(context);
        if (!userManager.isProfile() && activeUnlockStatusUtils.isAvailable()) {
            RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    combinedBiometricStatusUtils.getDisablingAdmin();
            BiometricSourcesUtils.setBiometricSafetySourceData(
                    SAFETY_SOURCE_ID,
                    context,
                    activeUnlockStatusUtils.getTitleForActiveUnlock(),
                    combinedBiometricStatusUtils.getSummary(),
                    BiometricSourcesUtils.createPendingIntent(
                            context,
                            biometricNavigationUtils.getBiometricSettingsIntent(
                                    context,
                                    combinedBiometricStatusUtils.getSettingsClassName(),
                                    disablingAdmin,
                                    Bundle.EMPTY),
                            REQUEST_CODE_COMBINED_BIOMETRIC_SETTING),
                    disablingAdmin == null /* enabled */,
                    combinedBiometricStatusUtils.hasEnrolled(),
                    safetyEvent);
            return;
        }
        if (combinedBiometricStatusUtils.isAvailable()) {
            RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    combinedBiometricStatusUtils.getDisablingAdmin();
            BiometricSourcesUtils.setBiometricSafetySourceData(
                    SAFETY_SOURCE_ID,
                    context,
                    combinedBiometricStatusUtils.getTitle(),
                    combinedBiometricStatusUtils.getSummary(),
                    BiometricSourcesUtils.createPendingIntent(
                            profileParentContext,
                            biometricNavigationUtils
                                    .getBiometricSettingsIntent(
                                            context,
                                            combinedBiometricStatusUtils
                                                    .getSettingsClassNameBasedOnUser(),
                                            disablingAdmin,
                                            Bundle.EMPTY)
                                    .setIdentifier(Integer.toString(userId)),
                            REQUEST_CODE_COMBINED_BIOMETRIC_SETTING),
                    disablingAdmin == null /* enabled */,
                    combinedBiometricStatusUtils.hasEnrolled(),
                    safetyEvent);
            return;
        }

        FaceManager faceManager = Utils.getFaceManagerOrNull(context);
        FaceStatusUtils faceStatusUtils = new FaceStatusUtils(context, faceManager, userId);

        if (faceStatusUtils.isAvailable()) {
            RestrictedLockUtils.EnforcedAdmin disablingAdmin = faceStatusUtils.getDisablingAdmin();
            BiometricSourcesUtils.setBiometricSafetySourceData(
                    SAFETY_SOURCE_ID,
                    context,
                    faceStatusUtils.getTitle(),
                    faceStatusUtils.getSummary(),
                    BiometricSourcesUtils.createPendingIntent(
                            profileParentContext,
                            biometricNavigationUtils
                                    .getBiometricSettingsIntent(
                                            context,
                                            faceStatusUtils.getSettingsClassName(),
                                            disablingAdmin,
                                            Bundle.EMPTY)
                                    .setIdentifier(Integer.toString(userId)),
                            REQUEST_CODE_FACE_SETTING),
                    disablingAdmin == null /* enabled */,
                    faceStatusUtils.hasEnrolled(),
                    safetyEvent);

            return;
        }

        FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(context);
        FingerprintStatusUtils fingerprintStatusUtils =
                new FingerprintStatusUtils(context, fingerprintManager, userId);

        if (fingerprintStatusUtils.isAvailable()) {
            RestrictedLockUtils.EnforcedAdmin disablingAdmin =
                    fingerprintStatusUtils.getDisablingAdmin();
            BiometricSourcesUtils.setBiometricSafetySourceData(
                    SAFETY_SOURCE_ID,
                    context,
                    fingerprintStatusUtils.getTitle(),
                    fingerprintStatusUtils.getSummary(),
                    BiometricSourcesUtils.createPendingIntent(
                            profileParentContext,
                            biometricNavigationUtils
                                    .getBiometricSettingsIntent(
                                            context,
                                            fingerprintStatusUtils.getSettingsClassName(),
                                            disablingAdmin,
                                            Bundle.EMPTY)
                                    .setIdentifier(Integer.toString(userId)),
                            REQUEST_CODE_FINGERPRINT_SETTING),
                    disablingAdmin == null /* enabled */,
                    fingerprintStatusUtils.hasEnrolled(),
                    safetyEvent);
            return;
        }

        SafetyCenterManagerWrapper.get()
                .setSafetySourceData(
                        context, SAFETY_SOURCE_ID, /* safetySourceData= */ null, safetyEvent);
    }

    /** Notifies Safety Center of a change in biometrics settings. */
    public static void onBiometricsChanged(Context context) {
        setSafetySourceData(
                context,
                new SafetyEvent.Builder(SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED)
                        .build());
    }
}
