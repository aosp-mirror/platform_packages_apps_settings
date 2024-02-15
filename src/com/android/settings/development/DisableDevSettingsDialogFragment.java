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

package com.android.settings.development;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DisableDevSettingsDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "DisableDevSettingDlg";

    @VisibleForTesting
    static DisableDevSettingsDialogFragment newInstance() {
        final DisableDevSettingsDialogFragment dialog = new DisableDevSettingsDialogFragment();
        return dialog;
    }

    public static void show(SettingsPreferenceFragment host) {
        final DisableDevSettingsDialogFragment dialog = new DisableDevSettingsDialogFragment();
        dialog.setTargetFragment(host, 0 /* requestCode */);
        // We need to handle data changes and switch state based on which button user clicks,
        // therefore we should enforce user to click one of the buttons
        // by disallowing dialog dismiss through tapping outside of dialog bounds.
        dialog.setCancelable(false);
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        dialog.show(manager, TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_DISABLE_DEVELOPMENT_OPTIONS;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Reuse the same text of disable_hw_offload_dialog.
        // The text is generic enough to be used for turning off Dev options.
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.bluetooth_disable_hw_offload_dialog_message)
                .setTitle(R.string.bluetooth_disable_hw_offload_dialog_title)
                .setPositiveButton(
                        R.string.bluetooth_disable_hw_offload_dialog_confirm, this)
                .setNegativeButton(
                        R.string.bluetooth_disable_hw_offload_dialog_cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Fragment fragment = getTargetFragment();
        if (!(fragment instanceof DevelopmentSettingsDashboardFragment)
                && !(fragment instanceof ProcessStatsSummary)) {
            Log.e(TAG, "getTargetFragment return unexpected type");
        }

        if (fragment instanceof DevelopmentSettingsDashboardFragment) {
            final DevelopmentSettingsDashboardFragment host =
                    (DevelopmentSettingsDashboardFragment) fragment;
            if (which == DialogInterface.BUTTON_POSITIVE) {
                host.onDisableDevelopmentOptionsConfirmed();
                PowerManager pm = getContext().getSystemService(PowerManager.class);
                pm.reboot(null);
            } else {
                host.onDisableDevelopmentOptionsRejected();
            }
        } else if (fragment instanceof ProcessStatsSummary) {
            final ProcessStatsSummary host =
                    (ProcessStatsSummary) fragment;
            if (which == DialogInterface.BUTTON_POSITIVE) {
                host.onRebootDialogConfirmed();
                PowerManager pm = getContext().getSystemService(PowerManager.class);
                pm.reboot(null);
            } else {
                host.onRebootDialogCanceled();
            }
        }
    }
}
