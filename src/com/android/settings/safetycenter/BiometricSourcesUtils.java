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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.safetycenter.SafetySourceStatus;

/** Static helpers for setting SafetyCenter data for biometric safety sources. */
public final class BiometricSourcesUtils {

    public static final int REQUEST_CODE_COMBINED_BIOMETRIC_SETTING = 10;
    public static final int REQUEST_CODE_FACE_SETTING = 20;
    public static final int REQUEST_CODE_FINGERPRINT_SETTING = 30;

    private BiometricSourcesUtils() {}

    /** Sets data for one of the biometrics sources */
    public static void setBiometricSafetySourceData(
            String safetySourceId,
            Context context,
            String title,
            String summary,
            PendingIntent pendingIntent,
            boolean enabled,
            boolean hasEnrolled,
            SafetyEvent safetyEvent) {
        setBiometricSafetySourceData(
                safetySourceId,
                context,
                title,
                summary,
                pendingIntent,
                enabled,
                hasEnrolled,
                safetyEvent,
                null
        );
    }

    /** Sets data for one of the biometrics sources */
    public static void setBiometricSafetySourceData(
            String safetySourceId,
            Context context,
            String title,
            String summary,
            PendingIntent pendingIntent,
            boolean enabled,
            boolean hasEnrolled,
            SafetyEvent safetyEvent,
            SafetySourceIssue safetySourceIssue) {
        int severityLevel =
                enabled && hasEnrolled
                        ? SafetySourceData.SEVERITY_LEVEL_INFORMATION
                        : SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED;

        SafetySourceStatus status =
                new SafetySourceStatus.Builder(title, summary, severityLevel)
                        .setPendingIntent(pendingIntent)
                        .setEnabled(enabled)
                        .build();

        SafetySourceData.Builder builder = new SafetySourceData.Builder().setStatus(status);
        if (safetySourceIssue != null) {
            builder.addIssue(safetySourceIssue);
        }
        SafetySourceData safetySourceData = builder.build();


        SafetyCenterManagerWrapper.get()
                .setSafetySourceData(context, safetySourceId, safetySourceData, safetyEvent);
    }

    /** Helper method for creating a pending intent. */
    public static PendingIntent createPendingIntent(
            Context context, Intent intent, int requestCode) {
        return PendingIntent.getActivity(
                context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
    }
}
