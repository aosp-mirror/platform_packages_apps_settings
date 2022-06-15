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
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.android.settingslib.RestrictedLockUtils;

/**
 * Activity that shows a dialog explaining that a CA cert is allowing someone to monitor network
 * traffic. This activity should be launched for the user into which the CA cert is installed
 * unless Intent.EXTRA_USER_ID is provided.
 */
public class MonitoringCertInfoActivity extends Activity implements OnClickListener,
        OnDismissListener {

    private int mUserId;

    @Override
    protected void onCreate(Bundle savedStates) {
        super.onCreate(savedStates);

        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());

        final UserHandle user;
        if (mUserId == UserHandle.USER_NULL) {
            user = null;
        } else {
            user = UserHandle.of(mUserId);
        }

        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        final int numberOfCertificates = getIntent().getIntExtra(
                Settings.EXTRA_NUMBER_OF_CERTIFICATES, 1);
        final int titleId = RestrictedLockUtils.getProfileOrDeviceOwner(this, user) != null
                ? R.plurals.ssl_ca_cert_settings_button // Check certificate
                : R.plurals.ssl_ca_cert_dialog_title; // Trust or remove certificate
        final CharSequence title = getResources().getQuantityText(titleId, numberOfCertificates);
        setTitle(title);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton(getResources().getQuantityText(
                R.plurals.ssl_ca_cert_settings_button, numberOfCertificates) , this);
        builder.setNeutralButton(R.string.cancel, null);
        builder.setOnDismissListener(this);

        if (dpm.getProfileOwnerAsUser(mUserId) != null) {
            builder.setMessage(getResources().getQuantityString(R.plurals.ssl_ca_cert_info_message,
                    numberOfCertificates, dpm.getProfileOwnerNameAsUser(mUserId)));
        } else if (dpm.getDeviceOwnerComponentOnCallingUser() != null) {
            builder.setMessage(getResources().getQuantityString(
                    R.plurals.ssl_ca_cert_info_message_device_owner, numberOfCertificates,
                    dpm.getDeviceOwnerNameOnAnyUser()));
        } else  {
            // Consumer case.  Show scary warning.
            builder.setIcon(android.R.drawable.stat_notify_error);
            builder.setMessage(R.string.ssl_ca_cert_warning_message);
        }

        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = new Intent(android.provider.Settings.ACTION_TRUSTED_CREDENTIALS_USER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(TrustedCredentialsSettings.ARG_SHOW_NEW_FOR_USER, mUserId);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }
}
