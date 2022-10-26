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

import static android.app.admin.DevicePolicyResources.Strings.Settings.DISABLED_BY_IT_ADMIN_TITLE;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Process;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.Utils;
import com.android.settingslib.enterprise.ActionDisabledByAdminController;
import com.android.settingslib.enterprise.ActionDisabledByAdminControllerFactory;

import java.util.Objects;

/**
 * Helper class for {@link ActionDisabledByAdminDialog} which sets up the dialog.
 */
public final class ActionDisabledByAdminDialogHelper {

    private static final String TAG = ActionDisabledByAdminDialogHelper.class.getName();
    @VisibleForTesting EnforcedAdmin mEnforcedAdmin;
    private ViewGroup mDialogView;
    private String mRestriction;
    private final ActionDisabledByAdminController mActionDisabledByAdminController;
    private final Activity mActivity;

    public ActionDisabledByAdminDialogHelper(Activity activity) {
        this(activity, null /* restriction */);
    }

    public ActionDisabledByAdminDialogHelper(Activity activity, String restriction) {
        mActivity = activity;
        mDialogView = (ViewGroup) LayoutInflater.from(mActivity).inflate(
                R.layout.support_details_dialog, null);
        mActionDisabledByAdminController = ActionDisabledByAdminControllerFactory
                .createInstance(mActivity, restriction,
                        new DeviceAdminStringProviderImpl(mActivity),
                        UserHandle.SYSTEM);
        DevicePolicyManager devicePolicyManager =
                mActivity.getSystemService(DevicePolicyManager.class);

        TextView title = mDialogView.findViewById(R.id.admin_support_dialog_title);
        title.setText(devicePolicyManager.getResources().getString(DISABLED_BY_IT_ADMIN_TITLE,
                () -> mActivity.getString(R.string.disabled_by_policy_title)));

    }

    private @UserIdInt int getEnforcementAdminUserId(@NonNull EnforcedAdmin admin) {
        return admin.user == null ? UserHandle.USER_NULL : admin.user.getIdentifier();
    }

    private @UserIdInt int getEnforcementAdminUserId() {
        return getEnforcementAdminUserId(mEnforcedAdmin);
    }

    public AlertDialog.Builder prepareDialogBuilder(String restriction,
            EnforcedAdmin enforcedAdmin) {
        DialogInterface.OnClickListener listener = mActionDisabledByAdminController
                .getPositiveButtonListener(mActivity, enforcedAdmin);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setPositiveButton(listener == null
                        ? R.string.suggestion_button_close : R.string.okay, listener)
                .setView(mDialogView);
        prepareDialogBuilder(builder, restriction, enforcedAdmin);
        return builder;
    }

    @VisibleForTesting
    void prepareDialogBuilder(AlertDialog.Builder builder, String restriction,
            EnforcedAdmin enforcedAdmin) {
        mActionDisabledByAdminController.initialize(
                new ActionDisabledLearnMoreButtonLauncherImpl(mActivity, builder));

        mEnforcedAdmin = enforcedAdmin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcedAdmin, getEnforcementAdminUserId(),
                mRestriction);
        mActionDisabledByAdminController.setupLearnMoreButton(mActivity);
    }

    public void updateDialog(String restriction, EnforcedAdmin admin) {
        if (mEnforcedAdmin.equals(admin) && Objects.equals(mRestriction, restriction)) {
            return;
        }
        mEnforcedAdmin = admin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcedAdmin, getEnforcementAdminUserId(),
                mRestriction);
    }

    private void initializeDialogViews(View root, EnforcedAdmin enforcedAdmin, int userId,
            String restriction) {
        ComponentName admin = enforcedAdmin.component;
        if (admin == null) {
            return;
        }

        mActionDisabledByAdminController.updateEnforcedAdmin(enforcedAdmin, userId);
        setAdminSupportIcon(root, admin, userId);

        if (isNotCurrentUserOrProfile(admin, userId)) {
            admin = null;
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

    private boolean isNotCurrentUserOrProfile(ComponentName admin, int userId) {
        return !RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(mActivity, admin)
                || !RestrictedLockUtils.isCurrentUserOrProfile(mActivity, userId);
    }

    @VisibleForTesting
    void setAdminSupportIcon(View root, ComponentName admin, int userId) {
        ImageView supportIconView = root.requireViewById(R.id.admin_support_icon);
        supportIconView.setImageDrawable(
                mActivity.getDrawable(R.drawable.ic_lock_closed));

        supportIconView.setImageTintList(Utils.getColorAccent(mActivity));
    }

    @VisibleForTesting
    void setAdminSupportTitle(View root, String restriction) {
        final TextView titleView = root.findViewById(R.id.admin_support_dialog_title);
        if (titleView == null) {
            return;
        }
        titleView.setText(mActionDisabledByAdminController.getAdminSupportTitle(restriction));
    }

    @VisibleForTesting
    void setAdminSupportDetails(final Activity activity, final View root,
            final EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin == null || enforcedAdmin.component == null) {
            return;
        }

        final DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        CharSequence supportMessage = null;
        if (!RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(activity,
                enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                activity, getEnforcementAdminUserId(enforcedAdmin))) {
            enforcedAdmin.component = null;
        } else {
            if (enforcedAdmin.user == null) {
                enforcedAdmin.user = UserHandle.of(UserHandle.myUserId());
            }
            if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                supportMessage = dpm.getShortSupportMessageForUser(enforcedAdmin.component,
                        getEnforcementAdminUserId(enforcedAdmin));
            }
        }
        final CharSequence supportContentString =
                mActionDisabledByAdminController.getAdminSupportContentString(
                        mActivity, supportMessage);
        final TextView textView = root.findViewById(R.id.admin_support_msg);
        if (supportContentString != null) {
            textView.setText(supportContentString);
        }
    }
}
