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
import android.os.UserHandle;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;
import android.safetycenter.SafetySourceStatus.IconAction;

import com.android.settings.R;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

/** Lock Screen Safety Source for Safety Center. */
public final class LockScreenSafetySource {

    public static final String SAFETY_SOURCE_ID = "LockScreenSafetySource";

    private LockScreenSafetySource() {
    }

    /** Sends lock screen safety data to Safety Center. */
    public static void sendSafetyData(Context context,
            ScreenLockPreferenceDetailsUtils screenLockPreferenceDetailsUtils) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        if (!screenLockPreferenceDetailsUtils.isAvailable()) {
            return;
        }

        final int userId = UserHandle.myUserId();
        final RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtilsInternal
                .checkIfPasswordQualityIsSet(context, userId);
        final PendingIntent pendingIntent = createPendingIntent(context,
                screenLockPreferenceDetailsUtils.getLaunchChooseLockGenericFragmentIntent());
        final IconAction gearMenuIconAction = createGearMenuIconAction(context,
                screenLockPreferenceDetailsUtils);

        final SafetySourceStatus status = new SafetySourceStatus.Builder(
                context.getString(R.string.unlock_set_unlock_launch_picker_title),
                screenLockPreferenceDetailsUtils.getSummary(UserHandle.myUserId()),
                screenLockPreferenceDetailsUtils.isLockPatternSecure()
                        ? SafetySourceStatus.STATUS_LEVEL_OK
                        : SafetySourceStatus.STATUS_LEVEL_RECOMMENDATION,
                pendingIntent)
                .setEnabled(
                        !screenLockPreferenceDetailsUtils.isPasswordQualityManaged(userId, admin))
                .setIconAction(gearMenuIconAction).build();
        final SafetySourceData safetySourceData = new SafetySourceData.Builder(
                SAFETY_SOURCE_ID).setStatus(status).build();

        SafetyCenterManagerWrapper.get().sendSafetyCenterUpdate(context, safetySourceData);
    }

    private static IconAction createGearMenuIconAction(Context context,
            ScreenLockPreferenceDetailsUtils screenLockPreferenceDetailsUtils) {
        return screenLockPreferenceDetailsUtils.shouldShowGearMenu() ? new IconAction(
                IconAction.ICON_TYPE_GEAR,
                createPendingIntent(context,
                        screenLockPreferenceDetailsUtils.getLaunchScreenLockSettingsIntent()))
                : null;
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
