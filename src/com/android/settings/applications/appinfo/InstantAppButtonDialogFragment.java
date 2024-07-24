/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.android.settings.applications.appinfo;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;

/**
 * Fragment to show the dialog for clearing the instant app.
 */
public class InstantAppButtonDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    private static final String ARG_PACKAGE_NAME = "packageName";

    private String mPackageName;

    public static InstantAppButtonDialogFragment newInstance(String packageName) {
        final InstantAppButtonDialogFragment dialogFragment = new InstantAppButtonDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_PACKAGE_NAME, packageName);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_INSTANT_APP_INFO_ACTION;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        mPackageName = arguments.getString(ARG_PACKAGE_NAME);
        return createDialog();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Context context = getContext();
        final PackageManager packageManager = context.getPackageManager();
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
            .action(context, SettingsEnums.ACTION_SETTINGS_CLEAR_INSTANT_APP, mPackageName);
        packageManager.deletePackageAsUser(mPackageName, null, 0, UserHandle.myUserId());
    }

    private AlertDialog createDialog() {
        AlertDialog confirmDialog = new AlertDialog.Builder(getContext())
            .setPositiveButton(R.string.clear_instant_app_data, this)
            .setNegativeButton(R.string.cancel, null)
            .setTitle(R.string.clear_instant_app_data)
            .setMessage(R.string.clear_instant_app_confirmation)
            .create();
        return confirmDialog;
    }

}

