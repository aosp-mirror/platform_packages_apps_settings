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

import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.safetycenter.SafetySourceStatus;
import android.safetycenter.SafetySourceStatus.IconAction;

import com.android.settings.R;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

/** Lock Screen Safety Source for Safety Center. */
public final class LockScreenSafetySource {

    public static final String SAFETY_SOURCE_ID = "AndroidLockScreen";
    public static final String NO_SCREEN_LOCK_ISSUE_ID = "NoScreenLockIssue";
    public static final String NO_SCREEN_LOCK_ISSUE_TYPE_ID = "NoScreenLockIssueType";
    public static final String SET_SCREEN_LOCK_ACTION_ID = "SetScreenLockAction";

    private static final int REQUEST_CODE_SCREEN_LOCK = 1;
    private static final int REQUEST_CODE_SCREEN_LOCK_SETTINGS = 2;

    private LockScreenSafetySource() {}

    /** Sets lock screen safety data for Safety Center. */
    public static void setSafetySourceData(
            Context context,
            ScreenLockPreferenceDetailsUtils screenLockPreferenceDetailsUtils,
            SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager != null && userManager.isProfile()) {
            return; // LockScreen source only supports primary profile.
        }

        if (!screenLockPreferenceDetailsUtils.isAvailable()) {
            SafetyCenterManagerWrapper.get()
                    .setSafetySourceData(
                            context, SAFETY_SOURCE_ID, /* safetySourceData= */ null, safetyEvent);
            return;
        }

        final int userId = UserHandle.myUserId();
        final RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtilsInternal.checkIfPasswordQualityIsSet(context, userId);
        final PendingIntent pendingIntent =
                createPendingIntent(
                        context,
                        screenLockPreferenceDetailsUtils.getLaunchChooseLockGenericFragmentIntent(
                                SettingsEnums.SAFETY_CENTER),
                        REQUEST_CODE_SCREEN_LOCK);
        final IconAction gearMenuIconAction =
                createGearMenuIconAction(context, screenLockPreferenceDetailsUtils);
        final boolean lockScreenAllowedByAdmin =
                !screenLockPreferenceDetailsUtils.isPasswordQualityManaged(userId, admin);
        final boolean isLockPatternSecure = screenLockPreferenceDetailsUtils.isLockPatternSecure();
        final int severityLevel =
                lockScreenAllowedByAdmin
                        ? isLockPatternSecure
                                ? SafetySourceData.SEVERITY_LEVEL_INFORMATION
                                : SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION
                        : SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED;

        final SafetySourceStatus status =
                new SafetySourceStatus.Builder(
                                context.getString(R.string.unlock_set_unlock_launch_picker_title),
                                lockScreenAllowedByAdmin
                                        ? screenLockPreferenceDetailsUtils.getSummary(
                                                UserHandle.myUserId())
                                        : context.getString(R.string.disabled_by_policy_title),
                                severityLevel)
                        .setPendingIntent(lockScreenAllowedByAdmin ? pendingIntent : null)
                        .setEnabled(lockScreenAllowedByAdmin)
                        .setIconAction(lockScreenAllowedByAdmin ? gearMenuIconAction : null)
                        .build();
        final SafetySourceData.Builder safetySourceDataBuilder =
                new SafetySourceData.Builder().setStatus(status);
        if (lockScreenAllowedByAdmin && !isLockPatternSecure) {
            safetySourceDataBuilder.addIssue(createNoScreenLockIssue(context, pendingIntent));
        }
        final SafetySourceData safetySourceData = safetySourceDataBuilder.build();

        SafetyCenterManagerWrapper.get()
                .setSafetySourceData(context, SAFETY_SOURCE_ID, safetySourceData, safetyEvent);
    }

    /** Notifies Safety Center of a change in lock screen settings. */
    public static void onLockScreenChange(Context context) {
        setSafetySourceData(
                context,
                new ScreenLockPreferenceDetailsUtils(context),
                new SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build());

        // Also send refreshed safety center data for biometrics, since changing lockscreen settings
        // can unset biometrics.
        BiometricsSafetySource.onBiometricsChanged(context);
    }

    private static IconAction createGearMenuIconAction(
            Context context, ScreenLockPreferenceDetailsUtils screenLockPreferenceDetailsUtils) {
        return screenLockPreferenceDetailsUtils.shouldShowGearMenu()
                ? new IconAction(
                        IconAction.ICON_TYPE_GEAR,
                        createPendingIntent(
                                context,
                                screenLockPreferenceDetailsUtils.getLaunchScreenLockSettingsIntent(
                                        SettingsEnums.SAFETY_CENTER),
                                REQUEST_CODE_SCREEN_LOCK_SETTINGS))
                : null;
    }

    private static PendingIntent createPendingIntent(
            Context context, Intent intent, int requestCode) {
        return PendingIntent.getActivity(
                context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static SafetySourceIssue createNoScreenLockIssue(
            Context context, PendingIntent pendingIntent) {
        final SafetySourceIssue.Action action =
                new SafetySourceIssue.Action.Builder(
                                SET_SCREEN_LOCK_ACTION_ID,
                                context.getString(R.string.no_screen_lock_issue_action_label),
                                pendingIntent)
                        .build();
        // Custom notification deliberately has zero actions
        final SafetySourceIssue.Notification customNotification =
                new SafetySourceIssue.Notification.Builder(
                                context.getString(R.string.no_screen_lock_issue_notification_title),
                                context.getString(R.string.no_screen_lock_issue_notification_text))
                        .build();
        return new SafetySourceIssue.Builder(
                        NO_SCREEN_LOCK_ISSUE_ID,
                        context.getString(R.string.no_screen_lock_issue_title),
                        context.getString(R.string.no_screen_lock_issue_summary),
                        SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION,
                        NO_SCREEN_LOCK_ISSUE_TYPE_ID)
                .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_DEVICE)
                .addAction(action)
                .setIssueActionability(SafetySourceIssue.ISSUE_ACTIONABILITY_MANUAL)
                .setCustomNotification(customNotification)
                .setNotificationBehavior(SafetySourceIssue.NOTIFICATION_BEHAVIOR_DELAYED)
                .build();
    }
}
