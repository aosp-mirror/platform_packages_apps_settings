/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * The a2dp/LE audio offload and LE audio feature switch should reboot the device to take effect,
 * the dialog is to ask the user to reboot the device after a2dp/LE audio offload and LE audio
 * feature user preference changed
 */
public class BluetoothRebootDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "BluetoothReboot";

    /**
     * The function to show the Reboot Dialog.
     */
    public static void show(DevelopmentSettingsDashboardFragment host) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final BluetoothRebootDialog dialog =
                    new BluetoothRebootDialog();
            dialog.setTargetFragment(host, 0 /* requestCode */);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_BLUETOOTH_DISABLE_A2DP_HW_OFFLOAD;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
        final OnRebootDialogListener host =
                (OnRebootDialogListener) getTargetFragment();
        if (host == null) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onRebootDialogConfirmed();
            PowerManager pm = getContext().getSystemService(PowerManager.class);
            pm.reboot(null);
        } else {
            host.onRebootDialogCanceled();
        }
    }

    /**
     * The interface for the RebootDialogListener to provide the action as the
     * confirmed or canceled clicked.
     */
    public interface OnRebootDialogListener {
        /**
         * Called when the user presses reboot on the warning dialog.
         */
        void onRebootDialogConfirmed();

        /**
         * Called when the user presses cancel on the warning dialog.
         */
        void onRebootDialogCanceled();
    }
}
