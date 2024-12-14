/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.bluetooth;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * A dialogFragment used by {@link BluetoothKeyMissingDialog} to create a dialog for the
 * bluetooth device.
 */
public class BluetoothKeyMissingDialogFragment extends InstrumentedDialogFragment
        implements OnClickListener {

    private static final String TAG = "BTKeyMissingDialogFragment";

    private BluetoothDevice mBluetoothDevice;

    public BluetoothKeyMissingDialogFragment(@NonNull BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.bluetooth_key_missing, null);
        TextView keyMissingTitle = view.findViewById(R.id.bluetooth_key_missing_title);
        keyMissingTitle.setText(
                getString(R.string.bluetooth_key_missing_title, mBluetoothDevice.getName()));
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.bluetooth_key_missing_forget), this);
        builder.setNegativeButton(getString(R.string.bluetooth_key_missing_cancel), this);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!getActivity().isFinishing()) {
            getActivity().finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Log.i(
                    TAG,
                    "Positive button clicked, remove bond for "
                            + mBluetoothDevice.getAnonymizedAddress());
            mBluetoothDevice.removeBond();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            Log.i(TAG, "Negative button clicked for " + mBluetoothDevice.getAnonymizedAddress());
        }
        if (!getActivity().isFinishing()) {
            getActivity().finish();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_KEY_MISSING_DIALOG_FRAGMENT;
    }
}
