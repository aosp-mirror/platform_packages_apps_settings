/*
 * Copyright 2022 The Android Open Source Project
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
 * The NFC log type switch should reboot the device to take effect,
 * the dialog is to ask the user to reboot the device.
 */
public class NfcRebootDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "NfcRebootDialog";

    /**
     * The function to show the Dialog.
     */
    public static void show(DevelopmentSettingsDashboardFragment host) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final NfcRebootDialog dialog = new NfcRebootDialog();
            dialog.setTargetFragment(host, 0 /* requestCode */);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_NFC_ENABLE_DETAIL_LOG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.nfc_reboot_dialog_message)
                .setTitle(R.string.nfc_reboot_dialog_title)
                .setPositiveButton(
                        R.string.nfc_reboot_dialog_confirm, this)
                .setNegativeButton(
                        android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final OnNfcRebootDialogConfirmedListener host =
                (OnNfcRebootDialogConfirmedListener) getTargetFragment();
        if (host == null) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onNfcRebootDialogConfirmed();
            PowerManager pm = getContext().getSystemService(PowerManager.class);
            pm.reboot(null);
        } else {
            host.onNfcRebootDialogCanceled();
        }
    }

    /**
     * Interface for EnableAdbWarningDialog callbacks.
     */
    public interface OnNfcRebootDialogConfirmedListener {
        /**
         * Called when the user presses enable on the warning dialog.
         */
        void onNfcRebootDialogConfirmed();

        /**
         * Called when the user presses cancel on the warning dialog.
         */
        void onNfcRebootDialogCanceled();
    }
}
