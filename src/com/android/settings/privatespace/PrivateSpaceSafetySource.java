/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Flags;
import android.os.UserManager;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;

/** Private Space safety source for the Safety Center */
public final class PrivateSpaceSafetySource {
    public static final String SAFETY_SOURCE_ID = "AndroidPrivateSpace";
    private static final String TAG = "PrivateSpaceSafetySrc";

    private PrivateSpaceSafetySource() {}

    /** Sets lock screen safety data for Safety Center. */
    public static void setSafetySourceData(Context context,
            SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            Log.i(TAG, "Safety Center disabled");
            return;
        }

        // Check the profile type - we don't want to show this for anything other than primary user.
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager != null && !userManager.isMainUser()) {
            Log.i(TAG, "setSafetySourceData not main user");
            return;
        }

        if (!Flags.allowPrivateProfile()) {
            // Setting null safetySourceData so that an old entry gets cleared out and this way
            // provide a response since SC always expects one on rescan.
            SafetyCenterManagerWrapper.get().setSafetySourceData(
                    context,
                    SAFETY_SOURCE_ID,
                    /* safetySourceData */ null,
                    safetyEvent
            );
            return;
        }

        PendingIntent pendingIntent = getPendingIntentForPsDashboard(context);

        SafetySourceStatus status = new SafetySourceStatus.Builder(
                context.getString(R.string.private_space_title),
                context.getString(R.string.private_space_summary),
                SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED)
                .setPendingIntent(pendingIntent).build();
        SafetySourceData safetySourceData =
                new SafetySourceData.Builder().setStatus(status).build();

        Log.d(TAG, "Setting safety source data");
        SafetyCenterManagerWrapper.get().setSafetySourceData(
                context,
                SAFETY_SOURCE_ID,
                safetySourceData,
                safetyEvent
        );
    }

    private static PendingIntent getPendingIntentForPsDashboard(Context context) {
        Intent privateSpaceAuthenticationIntent =
                new Intent(context, PrivateSpaceAuthenticationActivity.class)
                        .setIdentifier(SAFETY_SOURCE_ID);

        return PendingIntent
                .getActivity(
                        context,
                        /* requestCode */ 0,
                        privateSpaceAuthenticationIntent,
                        PendingIntent.FLAG_IMMUTABLE);
    }
}
