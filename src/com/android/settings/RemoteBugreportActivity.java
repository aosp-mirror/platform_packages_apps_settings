/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.SHARE_REMOTE_BUGREPORT_DIALOG_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.SHARE_REMOTE_BUGREPORT_FINISHED_REQUEST_CONSENT;
import static android.app.admin.DevicePolicyResources.Strings.Settings.SHARE_REMOTE_BUGREPORT_NOT_FINISHED_REQUEST_CONSENT;
import static android.app.admin.DevicePolicyResources.Strings.Settings.SHARING_REMOTE_BUGREPORT_MESSAGE;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * UI for the remote bugreport dialog. Shows one of 3 possible dialogs:
 * <ul>
 * <li>bugreport is still being taken and can be shared or declined</li>
 * <li>bugreport has been taken and can be shared or declined</li>
 * <li>bugreport has already been accepted to be shared, but is still being taken</li>
 * </ul>
 */
public class RemoteBugreportActivity extends Activity {

    private static final String TAG = "RemoteBugreportActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);

        final int notificationType = getIntent().getIntExtra(
                DevicePolicyManager.EXTRA_BUGREPORT_NOTIFICATION_TYPE, -1);

        if (notificationType == DevicePolicyManager.NOTIFICATION_BUGREPORT_ACCEPTED_NOT_FINISHED) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(devicePolicyManager.getResources().getString(
                            SHARING_REMOTE_BUGREPORT_MESSAGE,
                            () -> getString(R.string.sharing_remote_bugreport_dialog_message)))
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();
            dialog.show();
        } else if (notificationType == DevicePolicyManager.NOTIFICATION_BUGREPORT_STARTED
                || notificationType
                        == DevicePolicyManager.NOTIFICATION_BUGREPORT_FINISHED_NOT_ACCEPTED) {

            int defaultMessageId =  notificationType
                    == DevicePolicyManager.NOTIFICATION_BUGREPORT_STARTED
                    ? R.string.share_remote_bugreport_dialog_message
                    : R.string.share_remote_bugreport_dialog_message_finished;
            String overrideMessageId = notificationType
                    == DevicePolicyManager.NOTIFICATION_BUGREPORT_STARTED
                    ? SHARE_REMOTE_BUGREPORT_NOT_FINISHED_REQUEST_CONSENT
                    : SHARE_REMOTE_BUGREPORT_FINISHED_REQUEST_CONSENT;

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(devicePolicyManager.getResources().getString(
                            SHARE_REMOTE_BUGREPORT_DIALOG_TITLE,
                            () -> getString(R.string.share_remote_bugreport_dialog_title)))
                    .setMessage(devicePolicyManager.getResources().getString(overrideMessageId,
                            () -> getString(defaultMessageId)))
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.decline_remote_bugreport_action,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(
                                    DevicePolicyManager.ACTION_BUGREPORT_SHARING_DECLINED);
                            RemoteBugreportActivity.this.sendBroadcastAsUser(intent,
                                    UserHandle.SYSTEM, android.Manifest.permission.DUMP);
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.share_remote_bugreport_action,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(
                                    DevicePolicyManager.ACTION_BUGREPORT_SHARING_ACCEPTED);
                            RemoteBugreportActivity.this.sendBroadcastAsUser(intent,
                                    UserHandle.SYSTEM, android.Manifest.permission.DUMP);
                            finish();
                        }
                    })
                    .create();
            dialog.show();
        } else {
            Log.e(TAG, "Incorrect dialog type, no dialog shown. Received: " + notificationType);
        }
    }
}
