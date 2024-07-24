/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.UserInfo;
import android.content.pm.UserProperties;
import android.os.RemoteException;
import android.os.UserManager;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;

import com.android.internal.widget.LockPatternUtils;

/** Class containing methods shared between CDCA and CDCBA */
public class ConfirmDeviceCredentialUtils {

    public static void checkForPendingIntent(Activity activity) {
        // See Change-Id I52c203735fa9b53fd2f7df971824747eeb930f36 for context
        int taskId = activity.getIntent().getIntExtra(Intent.EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            try {
                IActivityManager activityManager = ActivityManager.getService();
                final ActivityOptions options = ActivityOptions.makeBasic();
                activityManager.startActivityFromRecents(taskId, options.toBundle());
                return;
            } catch (RemoteException e) {
                // Do nothing.
            }
        }
        IntentSender intentSender = activity.getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
        if (intentSender != null) {
            try {
                ActivityOptions activityOptions =
                        ActivityOptions.makeBasic()
                                .setPendingIntentBackgroundActivityStartMode(
                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                activity.startIntentSenderForResult(intentSender, -1, null, 0, 0, 0,
                        activityOptions.toBundle());
            } catch (IntentSender.SendIntentException e) {
                /* ignore */
            }
        }
    }

    public static void reportSuccessfulAttempt(LockPatternUtils utils, UserManager userManager,
            DevicePolicyManager dpm, int userId, boolean isStrongAuth) {
        if (isStrongAuth) {
            utils.reportSuccessfulPasswordAttempt(userId);
            if (isBiometricUnlockEnabledForPrivateSpace()) {
                final UserInfo userInfo = userManager.getUserInfo(userId);
                if (userInfo != null) {
                    if (isProfileThatAlwaysRequiresAuthToDisableQuietMode(userManager, userInfo)
                            || userInfo.isManagedProfile()) {
                        // Keyguard is responsible to disable StrongAuth for primary user. Disable
                        // StrongAuth for profile challenges only here.
                        utils.userPresent(userId);
                    }
                }
            }
        } else {
            dpm.reportSuccessfulBiometricAttempt(userId);
        }
        if (!isBiometricUnlockEnabledForPrivateSpace()) {
            if (userManager.isManagedProfile(userId)) {
                // Disable StrongAuth for work challenge only here.
                utils.userPresent(userId);
            }
        }
    }

    /**
     * Returns true if the userInfo passed as the parameter corresponds to a profile that always
     * requires auth to disable quiet mode and false otherwise
     */
    private static boolean isProfileThatAlwaysRequiresAuthToDisableQuietMode(
            UserManager userManager, @NonNull UserInfo userInfo) {
        final UserProperties userProperties =
                    userManager.getUserProperties(userInfo.getUserHandle());
        return userProperties.isAuthAlwaysRequiredToDisableQuietMode() && userInfo.isProfile();
    }

    private static boolean isBiometricUnlockEnabledForPrivateSpace() {
        return android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enableBiometricsToUnlockPrivateSpace();
    }

    /**
     * Request hiding soft-keyboard before animating away credential UI, in case IME
     * insets animation get delayed by dismissing animation.
     * @param view used to get root {@link WindowInsets} and {@link WindowInsetsController}.
     */
    public static void hideImeImmediately(@NonNull View view) {
        if (view.isAttachedToWindow()
                && view.getRootWindowInsets().isVisible(WindowInsets.Type.ime())) {
            view.getWindowInsetsController().hide(WindowInsets.Type.ime());
        }
    }
}
