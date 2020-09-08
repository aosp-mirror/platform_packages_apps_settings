/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.users;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * Helper class for displaying dialogs related to user settings.
 */
public final class UserDialogs {

    /**
     * Creates a dialog to confirm with the user if it's ok to remove the user
     * and delete all the data.
     *
     * @param context a Context object
     * @param removingUserId The userId of the user to remove
     * @param onConfirmListener Callback object for positive action
     * @return the created Dialog
     */
    public static Dialog createRemoveDialog(Context context, int removingUserId,
            DialogInterface.OnClickListener onConfirmListener) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        UserInfo userInfo = um.getUserInfo(removingUserId);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.user_delete_button, onConfirmListener)
                .setNegativeButton(android.R.string.cancel, null);
        if (userInfo.isManagedProfile()) {
            builder.setTitle(R.string.work_profile_confirm_remove_title);
            View view = createRemoveManagedUserDialogView(context, removingUserId);
            if (view != null) {
                builder.setView(view);
            } else {
                builder.setMessage(R.string.work_profile_confirm_remove_message);
            }
        } else if (UserHandle.myUserId() == removingUserId) {
            builder.setTitle(R.string.user_confirm_remove_self_title);
            builder.setMessage(R.string.user_confirm_remove_self_message);
        } else if (userInfo.isRestricted()) {
            builder.setTitle(R.string.user_profile_confirm_remove_title);
            builder.setMessage(R.string.user_profile_confirm_remove_message);
        } else {
            builder.setTitle(R.string.user_confirm_remove_title);
            builder.setMessage(R.string.user_confirm_remove_message);
        }
        return builder.create();
    }

    /**
     * Creates a view to be used in the confirmation dialog for removing work profile.
     */
    private static View createRemoveManagedUserDialogView(Context context, int userId) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo mdmApplicationInfo = Utils.getAdminApplicationInfo(context, userId);
        if (mdmApplicationInfo == null) {
            return null;
        }
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.delete_managed_profile_dialog, null);
        ImageView imageView =
                (ImageView) view.findViewById(R.id.delete_managed_profile_mdm_icon_view);
        Drawable badgedApplicationIcon = packageManager.getUserBadgedIcon(
                packageManager.getApplicationIcon(mdmApplicationInfo), new UserHandle(userId));
        imageView.setImageDrawable(badgedApplicationIcon);

        CharSequence appLabel = packageManager.getApplicationLabel(mdmApplicationInfo);
        CharSequence badgedAppLabel = packageManager.getUserBadgedLabel(appLabel,
                new UserHandle(userId));
        TextView textView =
                (TextView) view.findViewById(R.id.delete_managed_profile_device_manager_name);
        textView.setText(appLabel);
        if (!appLabel.toString().contentEquals(badgedAppLabel)) {
            textView.setContentDescription(badgedAppLabel);
        }

        return view;
    }

    /**
     * Creates a dialog to confirm that the user is ok to enable phone calls and SMS.
     *
     * @param onConfirmListener Callback object for positive action
     */
    public static Dialog createEnablePhoneCallsAndSmsDialog(Context context,
            DialogInterface.OnClickListener onConfirmListener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.user_enable_calling_and_sms_confirm_title)
                .setMessage(R.string.user_enable_calling_and_sms_confirm_message)
                .setPositiveButton(R.string.okay, onConfirmListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    /**
     * Creates a dialog to confirm that the user is ok to enable phone calls (no SMS).
     *
     * @param onConfirmListener Callback object for positive action
     */
    public static Dialog createEnablePhoneCallsDialog(Context context,
            DialogInterface.OnClickListener onConfirmListener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.user_enable_calling_confirm_title)
                .setMessage(R.string.user_enable_calling_confirm_message)
                .setPositiveButton(R.string.okay, onConfirmListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    /**
     * Creates a dialog to confirm that the user is ok to start setting up a new user.
     *
     * @param onConfirmListener Callback object for positive action
     */
    public static Dialog createSetupUserDialog(Context context,
            DialogInterface.OnClickListener onConfirmListener) {
        return new AlertDialog.Builder(context)
                .setTitle(com.android.settingslib.R.string.user_setup_dialog_title)
                .setMessage(com.android.settingslib.R.string.user_setup_dialog_message)
                .setPositiveButton(com.android.settingslib.R.string.user_setup_button_setup_now,
                        onConfirmListener)
                .setNegativeButton(com.android.settingslib.R.string.user_setup_button_setup_later,
                        null)
                .create();
    }
}
