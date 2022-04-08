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

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.Objects;

/**
 * Helper class for {@link ActionDisabledByAdminDialog} which sets up the dialog.
 */
public class ActionDisabledByAdminDialogHelper {

    private static final String TAG = ActionDisabledByAdminDialogHelper.class.getName();
    @VisibleForTesting EnforcedAdmin mEnforcedAdmin;
    private ViewGroup mDialogView;
    private String mRestriction = null;
    private Activity mActivity;

    public ActionDisabledByAdminDialogHelper(Activity activity) {
        mActivity = activity;
    }

    private @UserIdInt int getEnforcementAdminUserId(@NonNull EnforcedAdmin admin) {
        if (admin.user == null) {
            return UserHandle.USER_NULL;
        } else {
            return admin.user.getIdentifier();
        }
    }

    private @UserIdInt int getEnforcementAdminUserId() {
        return getEnforcementAdminUserId(mEnforcedAdmin);
    }

    public AlertDialog.Builder prepareDialogBuilder(String restriction,
            EnforcedAdmin enforcedAdmin) {
        mEnforcedAdmin = enforcedAdmin;
        mRestriction = restriction;
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        mDialogView = (ViewGroup) LayoutInflater.from(builder.getContext()).inflate(
                R.layout.admin_support_details_dialog, null);
        initializeDialogViews(mDialogView, mEnforcedAdmin.component, getEnforcementAdminUserId(),
                mRestriction);
        builder.setPositiveButton(R.string.okay, null).setView(mDialogView);
        maybeSetLearnMoreButton(builder);
        return builder;
    }

    @VisibleForTesting
    void maybeSetLearnMoreButton(AlertDialog.Builder builder) {
        // The "Learn more" button appears only if the restriction is enforced by an admin in the
        // same profile group. Otherwise the admin package and its policies are not accessible to
        // the current user.
        final UserManager um = UserManager.get(mActivity.getApplicationContext());
        if (um.isSameProfileGroup(getEnforcementAdminUserId(mEnforcedAdmin), um.getUserHandle())) {
            builder.setNeutralButton(R.string.learn_more, (dialog, which) -> {
                showAdminPolicies(mEnforcedAdmin, mActivity);
                mActivity.finish();
            });
        }
    }

    public void updateDialog(String restriction, EnforcedAdmin admin) {
        if (mEnforcedAdmin.equals(admin) && Objects.equals(mRestriction, restriction)) {
            return;
        }
        mEnforcedAdmin = admin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcedAdmin.component, getEnforcementAdminUserId(),
                mRestriction);
    }

    private void initializeDialogViews(View root, ComponentName admin, int userId,
            String restriction) {
        if (admin == null) {
            return;
        }
        ImageView supportIconView = root.requireViewById(R.id.admin_support_icon);
        if (!RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(mActivity, admin)
                || !RestrictedLockUtils.isCurrentUserOrProfile(mActivity, userId)) {
            admin = null;

            supportIconView.setImageDrawable(
                    mActivity.getDrawable(com.android.internal.R.drawable.ic_info));

            TypedArray ta = mActivity.obtainStyledAttributes(new int[]{android.R.attr.colorAccent});
            supportIconView.setImageTintList(ColorStateList.valueOf(ta.getColor(0, 0)));
            ta.recycle();
        } else {
            final Drawable badgedIcon = Utils.getBadgedIcon(
                    IconDrawableFactory.newInstance(mActivity),
                    mActivity.getPackageManager(),
                    admin.getPackageName(),
                    userId);
            supportIconView.setImageDrawable(badgedIcon);
        }

        setAdminSupportTitle(root, restriction);

        final UserHandle user;
        if (userId == UserHandle.USER_NULL) {
            user = null;
        } else {
            user = UserHandle.of(userId);
        }

        setAdminSupportDetails(mActivity, root, new EnforcedAdmin(admin, user));
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
        if (!RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(activity,
                enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                activity, getEnforcementAdminUserId(enforcedAdmin))) {
            enforcedAdmin.component = null;
        } else {
            if (enforcedAdmin.user == null) {
                enforcedAdmin.user = UserHandle.of(UserHandle.myUserId());
            }
            CharSequence supportMessage = null;
            if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                supportMessage = dpm.getShortSupportMessageForUser(enforcedAdmin.component,
                        getEnforcementAdminUserId(enforcedAdmin));
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
