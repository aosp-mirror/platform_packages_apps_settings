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

package com.android.settings.bluetooth;

import android.annotation.Nullable;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

/**
 * BluetoothPairingDialog asks the user to enter a PIN / Passkey / simple confirmation
 * for pairing with a remote Bluetooth device. It is an activity that appears as a dialog.
 */
public class BluetoothPairingDialog extends FragmentActivity {
    public static final String FRAGMENT_TAG = "bluetooth.pairing.fragment";

    private BluetoothPairingController mBluetoothPairingController;
    private boolean mReceiverRegistered = false;

    /**
     * Dismiss the dialog if the bond state changes to bonded or none,
     * or if pairing was canceled for {@link BluetoothPairingController#mDevice}.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED ||
                        bondState == BluetoothDevice.BOND_NONE) {
                    dismiss();
                }
            } else if (BluetoothDevice.ACTION_PAIRING_CANCEL.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null || mBluetoothPairingController.deviceEquals(device)) {
                    dismiss();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        Intent intent = getIntent();
        mBluetoothPairingController = new BluetoothPairingController(intent, this);
        // build the dialog fragment
        boolean fragmentFound = true;
        // check if the fragment has been preloaded
        BluetoothPairingDialogFragment bluetoothFragment =
            (BluetoothPairingDialogFragment) getSupportFragmentManager().
                    findFragmentByTag(FRAGMENT_TAG);
        // dismiss the fragment if it is already used
        if (bluetoothFragment != null && (bluetoothFragment.isPairingControllerSet()
            || bluetoothFragment.isPairingDialogActivitySet())) {
            bluetoothFragment.dismiss();
            bluetoothFragment = null;
        }
        // build a new fragment if it is null
        if (bluetoothFragment == null) {
            fragmentFound = false;
            bluetoothFragment = new BluetoothPairingDialogFragment();
        }
        bluetoothFragment.setPairingController(mBluetoothPairingController);
        bluetoothFragment.setPairingDialogActivity(this);
        // pass the fragment to the manager when it is created from scratch
        if (!fragmentFound) {
            bluetoothFragment.show(getSupportFragmentManager(), FRAGMENT_TAG);
        }
        /*
         * Leave this registered through pause/resume since we still want to
         * finish the activity in the background if pairing is canceled.
         */
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_CANCEL));
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mReceiverRegistered = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverRegistered) {
            mReceiverRegistered = false;
            unregisterReceiver(mReceiver);
        }
    }

    @VisibleForTesting
    void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }
}
