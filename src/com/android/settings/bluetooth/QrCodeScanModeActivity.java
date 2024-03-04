/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import static com.android.settingslib.bluetooth.BluetoothBroadcastUtils.EXTRA_BLUETOOTH_DEVICE_SINK;
import static com.android.settingslib.bluetooth.BluetoothBroadcastUtils.EXTRA_BLUETOOTH_SINK_IS_GROUP;
import static com.android.settingslib.flags.Flags.legacyLeAudioSharing;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothBroadcastUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;

/**
 * Finding a broadcast through QR code.
 *
 * To use intent action {@link BluetoothBroadcastUtils#ACTION_BLUETOOTH_LE_AUDIO_QR_CODE_SCANNER},
 * specify the bluetooth device sink of the broadcast to be provisioned in
 * {@link BluetoothBroadcastUtils#EXTRA_BLUETOOTH_DEVICE_SINK} and check the operation for all
 * coordinated set members throughout one session or not by
 * {@link BluetoothBroadcastUtils#EXTRA_BLUETOOTH_SINK_IS_GROUP}.
 */
public class QrCodeScanModeActivity extends QrCodeScanModeBaseActivity {
    private static final boolean DEBUG = BluetoothUtils.D;
    private static final String TAG = "QrCodeScanModeActivity";

    private boolean mIsGroupOp;
    private BluetoothDevice mSink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void handleIntent(Intent intent) {
        if (!legacyLeAudioSharing()) {
            finish();
        }

        String action = intent != null ? intent.getAction() : null;
        if (DEBUG) {
            Log.d(TAG, "handleIntent(), action = " + action);
        }

        if (action == null) {
            finish();
            return;
        }

        switch (action) {
            case BluetoothBroadcastUtils.ACTION_BLUETOOTH_LE_AUDIO_QR_CODE_SCANNER:
                showQrCodeScannerFragment(intent);
                break;
            default:
                if (DEBUG) {
                    Log.e(TAG, "Launch with an invalid action");
                }
                finish();
        }
    }

    protected void showQrCodeScannerFragment(Intent intent) {
        if (intent == null) {
            if (DEBUG) {
                Log.d(TAG, "intent is null, can not get bluetooth information from intent.");
            }
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "showQrCodeScannerFragment");
        }

        mSink = intent.getParcelableExtra(EXTRA_BLUETOOTH_DEVICE_SINK);
        mIsGroupOp = intent.getBooleanExtra(EXTRA_BLUETOOTH_SINK_IS_GROUP, false);
        if (DEBUG) {
            Log.d(TAG, "get extra from intent");
        }

        QrCodeScanModeFragment fragment =
                (QrCodeScanModeFragment) mFragmentManager.findFragmentByTag(
                        BluetoothBroadcastUtils.TAG_FRAGMENT_QR_CODE_SCANNER);

        if (fragment == null) {
            fragment = new QrCodeScanModeFragment();
        } else {
            if (fragment.isVisible()) {
                return;
            }

            // When the fragment in back stack but not on top of the stack, we can simply pop
            // stack because current fragment transactions are arranged in an order
            mFragmentManager.popBackStackImmediate();
            return;
        }
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                BluetoothBroadcastUtils.TAG_FRAGMENT_QR_CODE_SCANNER);
        fragmentTransaction.commit();
    }
}

