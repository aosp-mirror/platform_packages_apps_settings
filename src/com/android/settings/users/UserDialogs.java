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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;

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
        int titleResId;
        int messageResId;
        if (UserHandle.myUserId() == removingUserId) {
            titleResId = R.string.user_confirm_remove_self_title;
            messageResId = R.string.user_confirm_remove_self_message;
        } else if (userInfo.isRestricted()) {
            titleResId = R.string.user_profile_confirm_remove_title;
            messageResId = R.string.user_profile_confirm_remove_message;
        } else if (userInfo.isManagedProfile()) {
            titleResId = R.string.work_profile_confirm_remove_title;
            messageResId = R.string.work_profile_confirm_remove_message;
        } else {
            titleResId = R.string.user_confirm_remove_title;
            messageResId = R.string.user_confirm_remove_message;
        }
        return new AlertDialog.Builder(context)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(R.string.user_delete_button, onConfirmListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
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
}
