/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

/**
 * Activity that shows a dialog explaining that a CA cert is allowing someone to monitor network
 * traffic.
 */
public class MonitoringCertInfoActivity extends Activity implements OnClickListener {

    private boolean hasDeviceOwner = false;

    @Override
    protected void onCreate(Bundle savedStates) {
        super.onCreate(savedStates);

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ssl_ca_cert_dialog_title);
        builder.setCancelable(true);
        hasDeviceOwner = dpm.getDeviceOwner() != null;
        int buttonLabel;
        if (hasDeviceOwner) {
            // Institutional case.  Show informational message.
            String message = this.getResources().getString(R.string.ssl_ca_cert_info_message,
                    dpm.getDeviceOwnerName());
            builder.setMessage(message);
            buttonLabel = R.string.done_button;
        } else {
            // Consumer case.  Show scary warning.
            builder.setIcon(android.R.drawable.stat_notify_error);
            builder.setMessage(R.string.ssl_ca_cert_warning_message);
            buttonLabel = R.string.ssl_ca_cert_settings_button;
        }

        builder.setPositiveButton(buttonLabel, this);

        final Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        try {
            WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
        } catch (RemoteException e) {
        }
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel(DialogInterface dialog) {
                finish();
            }
        });

        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (hasDeviceOwner) {
            finish();
        } else {
            Intent intent =
                    new Intent(android.provider.Settings.ACTION_TRUSTED_CREDENTIALS_USER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }
}
