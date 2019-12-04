/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings;

import android.annotation.StringRes;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;

import androidx.appcompat.app.AlertDialog;

/** Dialog to confirm a reboot immediately, or later. */
public class RebootDialog implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {
    private final Activity mActivity;
    private final AlertDialog mDialog;
    private final String mRebootReason;

    public RebootDialog(Activity activity, @StringRes int titleRes, @StringRes int messageRes,
            String rebootReason) {
        mActivity = activity;
        mDialog = new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.app_src_cert_reboot_dialog_button_restart, this)
                .setNegativeButton(R.string.app_src_cert_reboot_dialog_button_not_now, null)
                .setOnDismissListener(this)
                .create();
        mRebootReason = rebootReason;
    }

    /** Shows the dialog. */
    public void show() {
        mDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
            pm.reboot(mRebootReason);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mActivity.finish();
    }
}
