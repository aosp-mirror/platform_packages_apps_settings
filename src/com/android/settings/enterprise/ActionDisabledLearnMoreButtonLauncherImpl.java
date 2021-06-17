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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd;
import com.android.settingslib.enterprise.ActionDisabledLearnMoreButtonLauncher;

/**
 * Helper class to set up the "Learn more" button in the action disabled dialog.
 */
public final class ActionDisabledLearnMoreButtonLauncherImpl
        extends ActionDisabledLearnMoreButtonLauncher {

    private final Activity mActivity;
    private final AlertDialog.Builder mBuilder;

    ActionDisabledLearnMoreButtonLauncherImpl(Activity activity, AlertDialog.Builder builder) {
        mActivity = requireNonNull(activity, "activity cannot be null");
        mBuilder = requireNonNull(builder, "builder cannot be null");
    }

    @Override
    public void setLearnMoreButton(Runnable action) {
        requireNonNull(action, "action cannot be null");

        mBuilder.setNeutralButton(R.string.learn_more, (dialog, which) -> action.run());
    }

    @Override
    protected void launchShowAdminPolicies(Context context, UserHandle user, ComponentName admin) {
        requireNonNull(context, "context cannot be null");
        requireNonNull(user, "user cannot be null");
        requireNonNull(admin, "admin cannot be null");

        Intent intent = new Intent()
                .setClass(mActivity, DeviceAdminAdd.class)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                .putExtra(DeviceAdminAdd.EXTRA_CALLED_FROM_SUPPORT_DIALOG, true);
        // DeviceAdminAdd class may need to run as managed profile.
        mActivity.startActivityAsUser(intent, user);
    }

    @Override
    protected void launchShowAdminSettings(Context context) {
        requireNonNull(context, "context cannot be null");

        Intent intent = new Intent()
                .setClass(mActivity, Settings.DeviceAdminSettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Activity merges both managed profile and parent users
        // admins so show as same user as this activity.
        mActivity.startActivity(intent);
    }

    @Override
    protected void finishSelf() {
        mActivity.finish();
    }
}
