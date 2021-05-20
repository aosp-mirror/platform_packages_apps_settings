/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.enterprise;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.enterprise.ActionDisabledLearnMoreButtonLauncher;

import java.util.function.BiConsumer;

/**
 * Helper class to set up the "Learn more" button in the action disabled dialog.
 */
public class ActionDisabledLearnMoreButtonLauncherImpl
        implements ActionDisabledLearnMoreButtonLauncher {

    static final BiConsumer<Activity, EnforcedAdmin> SHOW_ADMIN_POLICIES =
            (activity, enforcedAdmin) -> {
                showAdminPolicies(enforcedAdmin, activity);
                activity.finish();
            };

    static final BiConsumer<Activity, String> LAUNCH_HELP_PAGE = (activity, url) -> {
        launchLearnMoreHelpPage(activity, url);
        activity.finish();
    };

    @Override
    public void setupLearnMoreButtonToShowAdminPolicies(
            Activity activity,
            AlertDialog.Builder builder,
            int enforcementAdminUserId,
            EnforcedAdmin enforcedAdmin) {
        requireNonNull(activity);
        requireNonNull(builder);
        requireNonNull(enforcedAdmin);
        // The "Learn more" button appears only if the restriction is enforced by an admin in the
        // same profile group. Otherwise the admin package and its policies are not accessible to
        // the current user.
        final UserManager um = UserManager.get(activity);
        if (um.isSameProfileGroup(enforcementAdminUserId, um.getUserHandle())) {
            setupLearnMoreButton(builder, () ->
                    SHOW_ADMIN_POLICIES.accept(activity, enforcedAdmin));
        }
    }

    @Override
    public void setupLearnMoreButtonToLaunchHelpPage(
            Activity activity,
            AlertDialog.Builder builder,
            String url) {
        requireNonNull(activity);
        requireNonNull(builder);
        requireNonNull(url);
        setupLearnMoreButton(builder, () -> LAUNCH_HELP_PAGE.accept(activity, url));
    }

    private void setupLearnMoreButton(AlertDialog.Builder builder, Runnable runnable) {
        builder.setNeutralButton(R.string.learn_more, (dialog, which) -> {
            runnable.run();
        });
    }

    private static void launchLearnMoreHelpPage(Activity activity, String url) {
        activity.startActivityAsUser(createLearnMoreIntent(url), UserHandle.SYSTEM);
    }

    private static Intent createLearnMoreIntent(String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }

    private static void showAdminPolicies(
            EnforcedAdmin enforcedAdmin,
            Activity activity) {
        final Intent intent = new Intent();
        if (enforcedAdmin.component != null) {
            intent.setClass(activity, DeviceAdminAdd.class);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    enforcedAdmin.component);
            intent.putExtra(DeviceAdminAdd.EXTRA_CALLED_FROM_SUPPORT_DIALOG, true);
            // DeviceAdminAdd class may need to run as managed profile.
            activity.startActivityAsUser(intent, enforcedAdmin.user);
        } else {
            intent.setClass(activity, Settings.DeviceAdminSettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Activity merges both managed profile and parent users
            // admins so show as same user as this activity.
            activity.startActivity(intent);
        }
    }
}
