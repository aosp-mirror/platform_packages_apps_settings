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

import static android.app.admin.DevicePolicyResources.Strings.Settings.DEVICE_OWNER_INSTALLED_CERTIFICATE_AUTHORITY_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_INSTALLED_CERTIFICATE_AUTHORITY_WARNING;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.utils.StringUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
                ? R.string.ssl_ca_cert_settings_button // Check certificate
                : R.string.ssl_ca_cert_dialog_title; // Trust or remove certificate
        final CharSequence title = StringUtil.getIcuPluralsString(this, numberOfCertificates,
                titleId);
        setTitle(title);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton(StringUtil.getIcuPluralsString(this, numberOfCertificates,
                R.string.ssl_ca_cert_settings_button) , this);
        builder.setNeutralButton(R.string.cancel, null);
        builder.setOnDismissListener(this);

        if (dpm.getProfileOwnerAsUser(mUserId) != null) {
            MessageFormat msgFormat = new MessageFormat(
                    dpm.getResources().getString(
                            WORK_PROFILE_INSTALLED_CERTIFICATE_AUTHORITY_WARNING,
                            () -> getString(R.string.ssl_ca_cert_info_message)),
                    Locale.getDefault());

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("numberOfCertificates", numberOfCertificates);
            arguments.put("orgName", dpm.getProfileOwnerNameAsUser(mUserId));

            builder.setMessage(msgFormat.format(arguments));
        } else if (dpm.getDeviceOwnerComponentOnCallingUser() != null) {
            MessageFormat msgFormat = new MessageFormat(
                    dpm.getResources()
                            .getString(DEVICE_OWNER_INSTALLED_CERTIFICATE_AUTHORITY_WARNING,
                                    () -> getResources().getString(
                                            R.string.ssl_ca_cert_info_message_device_owner)),
                    Locale.getDefault());

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("numberOfCertificates", numberOfCertificates);
            arguments.put("orgName", dpm.getDeviceOwnerNameOnAnyUser());

            builder.setMessage(msgFormat.format(arguments));
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
