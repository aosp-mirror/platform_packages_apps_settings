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
 * limitations under the License.
 */

package com.android.settings.enterprise;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.util.IconDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.util.Objects;

/**
 * Helper class for {@link ActionDisabledByAdminDialog} which sets up the dialog.
 */
public class ActionDisabledByAdminDialogHelper {

    private static final String TAG = ActionDisabledByAdminDialogHelper.class.getName();
    private EnforcedAdmin mEnforcedAdmin;
    private ViewGroup mDialogView;
    private String mRestriction = null;
    private Activity mActivity;

    public ActionDisabledByAdminDialogHelper(Activity activity) {
        mActivity = activity;
    }

    public AlertDialog.Builder prepareDialogBuilder(String restriction,
            EnforcedAdmin enforcedAdmin) {
        mEnforcedAdmin = enforcedAdmin;
        mRestriction = restriction;

        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        mDialogView = (ViewGroup) LayoutInflater.from(builder.getContext()).inflate(
                R.layout.admin_support_details_dialog, null);
        initializeDialogViews(mDialogView, mEnforcedAdmin.component, mEnforcedAdmin.userId,
                mRestriction);
        return builder
            .setPositiveButton(R.string.okay, null)
            .setNeutralButton(R.string.learn_more,
                    (dialog, which) -> {
                        showAdminPolicies(mEnforcedAdmin, mActivity);
                        mActivity.finish();
                    })
            .setView(mDialogView);
    }

    public void updateDialog(String restriction, EnforcedAdmin admin) {
        if (mEnforcedAdmin.equals(admin) && Objects.equals(mRestriction, restriction)) {
            return;
        }
        mEnforcedAdmin = admin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcedAdmin.component, mEnforcedAdmin.userId,
                mRestriction);
    }

    private void initializeDialogViews(View root, ComponentName admin, int userId,
            String restriction) {
        if (admin == null) {
            return;
        }
        if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(mActivity, admin)
                || !RestrictedLockUtils.isCurrentUserOrProfile(mActivity, userId)) {
            admin = null;
        } else {
            final Drawable badgedIcon = Utils.getBadgedIcon(
                    IconDrawableFactory.newInstance(mActivity),
                    mActivity.getPackageManager(),
                    admin.getPackageName(),
                    userId);
            ((ImageView) root.findViewById(R.id.admin_support_icon)).setImageDrawable(badgedIcon);
        }

        setAdminSupportTitle(root, restriction);
        setAdminSupportDetails(mActivity, root, new EnforcedAdmin(admin, userId));
    }

    @VisibleForTesting
    void setAdminSupportTitle(View root, String restriction) {
        final TextView titleView = root.findViewById(R.id.admin_support_dialog_title);
        if (titleView == null) {
            return;
        }
        if (restriction == null) {
            titleView.setText(R.string.disabled_by_policy_title);
            return;
        }
        switch (restriction) {
            case UserManager.DISALLOW_ADJUST_VOLUME:
                titleView.setText(R.string.disabled_by_policy_title_adjust_volume);
                break;
            case UserManager.DISALLOW_OUTGOING_CALLS:
                titleView.setText(R.string.disabled_by_policy_title_outgoing_calls);
                break;
            case UserManager.DISALLOW_SMS:
                titleView.setText(R.string.disabled_by_policy_title_sms);
                break;
            case DevicePolicyManager.POLICY_DISABLE_CAMERA:
                titleView.setText(R.string.disabled_by_policy_title_camera);
                break;
            case DevicePolicyManager.POLICY_DISABLE_SCREEN_CAPTURE:
                titleView.setText(R.string.disabled_by_policy_title_screen_capture);
                break;
            case DevicePolicyManager.POLICY_MANDATORY_BACKUPS:
                titleView.setText(R.string.disabled_by_policy_title_turn_off_backups);
                break;
            case DevicePolicyManager.POLICY_SUSPEND_PACKAGES:
                titleView.setText(R.string.disabled_by_policy_title_suspend_packages);
                break;
            default:
                // Use general text if no specialized title applies
                titleView.setText(R.string.disabled_by_policy_title);
        }
    }

    @VisibleForTesting
    void setAdminSupportDetails(final Activity activity, final View root,
            final EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin == null || enforcedAdmin.component == null) {
            return;
        }
        final DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(activity,
                enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                activity, enforcedAdmin.userId)) {
            enforcedAdmin.component = null;
        } else {
            if (enforcedAdmin.userId == UserHandle.USER_NULL) {
                enforcedAdmin.userId = UserHandle.myUserId();
            }
            CharSequence supportMessage = null;
            if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                supportMessage = dpm.getShortSupportMessageForUser(
                        enforcedAdmin.component, enforcedAdmin.userId);
            }
            if (supportMessage != null) {
                final TextView textView = root.findViewById(R.id.admin_support_msg);
                textView.setText(supportMessage);
            }
        }
    }

    @VisibleForTesting
    void showAdminPolicies(final EnforcedAdmin enforcedAdmin, final Activity activity) {
        final Intent intent = new Intent();
        if (enforcedAdmin.component != null) {
            intent.setClass(activity, DeviceAdminAdd.class);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    enforcedAdmin.component);
            intent.putExtra(DeviceAdminAdd.EXTRA_CALLED_FROM_SUPPORT_DIALOG, true);
            // DeviceAdminAdd class may need to run as managed profile.
            activity.startActivityAsUser(intent,
                    new UserHandle(enforcedAdmin.userId));
        } else {
            intent.setClass(activity, Settings.DeviceAdminSettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Activity merges both managed profile and parent users
            // admins so show as same user as this activity.
            activity.startActivity(intent);
        }
    }
}
