/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.android.settings.safetycenter.BiometricSourcesUtils.REQUEST_CODE_FINGERPRINT_SETTING;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.safetycenter.SafetyEvent;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricNavigationUtils;
import com.android.settings.biometrics.fingerprint.FingerprintStatusUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;

/** Fingerprint biometrics Safety Source for Safety Center. */
public final class FingerprintSafetySource {

    public static final String SAFETY_SOURCE_ID = "AndroidFingerprintUnlock";

    private FingerprintSafetySource() {}

    /** Sets biometric safety data for Safety Center. */
    public static void setSafetySourceData(Context context, SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        // Handle private profile case
        UserManager userManager = UserManager.get(context);
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

        UserHandle userHandle = Process.myUserHandle();
        int userId = userHandle.getIdentifier();
        FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(context);
        FingerprintStatusUtils fingerprintStatusUtils =
                new FingerprintStatusUtils(context, fingerprintManager, userId);
        BiometricNavigationUtils biometricNavigationUtils = new BiometricNavigationUtils(userId);
        UserHandle profileParentUserHandle = userManager.getProfileParent(userHandle);
        if (profileParentUserHandle == null) {
            profileParentUserHandle = userHandle;
        }
        Context profileParentContext = context.createContextAsUser(profileParentUserHandle, 0);

        if (Utils.hasFingerprintHardware(context)) {
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
                    safetyEvent,
                    FeatureFactory.getFeatureFactory().getBiometricsFeatureProvider()
                            .getSafetySourceIssue(SAFETY_SOURCE_ID));
            return;
        }

        SafetyCenterManagerWrapper.get()
                .setSafetySourceData(
                        context, SAFETY_SOURCE_ID, /* safetySourceData= */ null, safetyEvent);
    }

    /** Notifies Safety Center of a change in fingerprint biometrics settings. */
    public static void onBiometricsChanged(Context context) {
        setSafetySourceData(
                context,
                new SafetyEvent.Builder(SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED)
                        .build());
    }
}
