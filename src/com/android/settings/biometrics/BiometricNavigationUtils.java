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

package com.android.settings.biometrics;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.app.UnlaunchableAppActivity;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.transition.SettingsTransitionHelper;

/**
 * Utilities for navigation shared between Security Settings and Safety Center.
 */
public class BiometricNavigationUtils {

    private final int mUserId;

    public BiometricNavigationUtils(int userId) {
        mUserId = userId;
    }

    /**
     * Tries to launch the Settings screen if Quiet Mode is not enabled
     * for managed profile, otherwise shows a dialog to disable the Quiet Mode.
     *
     * @param className The class name of Settings screen to launch.
     * @param extras    Extras to put into the launching {@link Intent}.
     * @return true if the Settings screen is launching.
     */
    public boolean launchBiometricSettings(Context context, String className, Bundle extras) {
        final Intent quietModeDialogIntent = getQuietModeDialogIntent(context);
        if (quietModeDialogIntent != null) {
            context.startActivity(quietModeDialogIntent);
            return false;
        }
        context.startActivity(getSettingsPageIntent(className, extras));
        return true;
    }

    /**
     * Returns {@link Intent} to launch an appropriate Settings screen.
     *
     * <p>If the Setting is disabled by admin, returns {@link Intent} to launch an explanation.
     * If Quiet Mode is enabled for managed profile, returns {@link Intent} to launch a dialog
     * to disable the Quiet Mode. Otherwise, returns {@link Intent} to launch the Settings screen.
     *
     * @param className     The class name of Settings screen to launch.
     * @param enforcedAdmin Details of admin account that disables changing the setting.
     * @param extras        Extras to put into the result {@link Intent}.
     */
    public Intent getBiometricSettingsIntent(Context context, String className,
            EnforcedAdmin enforcedAdmin, Bundle extras) {
        if (enforcedAdmin != null) {
            return getRestrictedDialogIntent(context, enforcedAdmin);
        }
        final Intent quietModeDialogIntent = getQuietModeDialogIntent(context);
        return quietModeDialogIntent != null ? quietModeDialogIntent
                : getSettingsPageIntent(className, extras);
    }

    private Intent getQuietModeDialogIntent(Context context) {
        final UserManager userManager = UserManager.get(context);
        if (userManager.isQuietModeEnabled(UserHandle.of(mUserId))) {
            return UnlaunchableAppActivity.createInQuietModeDialogIntent(mUserId);
        }
        return null;
    }

    private Intent getRestrictedDialogIntent(Context context, EnforcedAdmin enforcedAdmin) {
        final Intent intent = RestrictedLockUtils
                .getShowAdminSupportDetailsIntent(context, enforcedAdmin);
        int targetUserId = mUserId;
        if (enforcedAdmin.user != null && RestrictedLockUtils
                .isCurrentUserOrProfile(context, enforcedAdmin.user.getIdentifier())) {
            targetUserId = enforcedAdmin.user.getIdentifier();
        }
        intent.putExtra(DevicePolicyManager.EXTRA_RESTRICTION, enforcedAdmin.enforcedRestriction);
        intent.putExtra(Intent.EXTRA_USER_ID, targetUserId);
        return intent;
    }

    private Intent getSettingsPageIntent(String className, Bundle extras) {
        final Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, className);
        if (!extras.isEmpty()) {
            intent.putExtras(extras);
        }
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
        intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE);

        return intent;
    }
}
