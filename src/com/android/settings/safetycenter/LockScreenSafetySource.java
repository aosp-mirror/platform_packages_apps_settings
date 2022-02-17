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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settingslib.transition.SettingsTransitionHelper;

/** Lock Screen Safety Source for Safety Center. */
public final class LockScreenSafetySource {

    public static final String SAFETY_SOURCE_ID = "LockScreenSafetySource";

    private LockScreenSafetySource() {}

    /** Sends lock screen safety data to Safety Center. */
    public static void sendSafetyData(Context context) {
        if (!SafetyCenterStatusHolder.get().isEnabled(context)) {
            return;
        }

        // TODO(b/215515298): Replace placeholder SafetySourceData with real data.
        // TODO(b/217409995): Replace SECURITY_ALTERNATIVE with Safety Center metrics category.
        Intent clickIntent = new SubSettingLauncher(context)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.SECURITY_ALTERNATIVE)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .toIntent();
        PendingIntent pendingIntent = PendingIntent
                .getActivity(
                        context,
                        0 /* requestCode */,
                        clickIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        SafetySourceData safetySourceData =
                new SafetySourceData.Builder(SAFETY_SOURCE_ID).setStatus(
                        new SafetySourceStatus.Builder(
                                "Lock Screen",
                                "Lock screen settings",
                                SafetySourceStatus.STATUS_LEVEL_OK,
                                pendingIntent).build()
                ).build();

        SafetyCenterManagerWrapper.get().sendSafetyCenterUpdate(context, safetySourceData);
    }
}
