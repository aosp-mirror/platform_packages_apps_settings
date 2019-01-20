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

package com.android.settings.deviceinfo.aboutphone;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Warning dialog to let the user know where the device name will be shown before setting it.
 */
public class DeviceNameWarningDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "DeviceNameWarningDlg";

    public static void show(MyDeviceInfoFragment host) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }

        final DeviceNameWarningDialog dialog = new DeviceNameWarningDialog();
        dialog.setTargetFragment(host, 0 /* requestCode */);
        dialog.show(manager, TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ENABLE_DEVELOPMENT_OPTIONS;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.my_device_info_device_name_preference_title)
                .setMessage(R.string.about_phone_device_name_warning)
                .setCancelable(false)
                .setPositiveButton(com.android.internal.R.string.ok, this)
                .setNegativeButton(com.android.internal.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final MyDeviceInfoFragment host = (MyDeviceInfoFragment) getTargetFragment();
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onSetDeviceNameConfirm(true);
        } else {
            host.onSetDeviceNameConfirm(false);
        }
    }
}
