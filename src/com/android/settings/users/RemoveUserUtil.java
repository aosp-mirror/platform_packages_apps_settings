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
 * limitations under the License.
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

public class RemoveUserUtil {

    static Dialog createConfirmationDialog(Context context, int removingUserId,
            DialogInterface.OnClickListener onConfirmListener) {
        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        final UserInfo userInfo = um.getUserInfo(removingUserId);
        Dialog dlg = new AlertDialog.Builder(context)
                .setTitle(UserHandle.myUserId() == removingUserId
                    ? R.string.user_confirm_remove_self_title
                    : (userInfo.isRestricted()
                        ? R.string.user_profile_confirm_remove_title
                        : R.string.user_confirm_remove_title))
                .setMessage(UserHandle.myUserId() == removingUserId
                    ? R.string.user_confirm_remove_self_message
                    : (userInfo.isRestricted()
                        ? R.string.user_profile_confirm_remove_message
                        : R.string.user_confirm_remove_message))
                .setPositiveButton(R.string.user_delete_button,
                        onConfirmListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        return dlg;
    }
}
