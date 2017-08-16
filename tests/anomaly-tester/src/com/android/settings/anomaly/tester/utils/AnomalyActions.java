/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.anomaly.tester.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.util.List;

/**
 * Actions to generate anomaly.
 */
public class AnomalyActions {
    private static final String TAG = AnomalyActions.class.getSimpleName();

    public static final String KEY_ACTION = "action";
    public static final String KEY_DURATION_MS = "duration_ms";
    public static final String KEY_RESULT_RECEIVER = "result_receiver";

    public static final String ACTION_BLE_SCAN_UNOPTIMIZED = "action.ble_scan_unoptimized";
    public static final String ACTION_WAKE_LOCK = "action.wake_lock";

    public static void doAction(Context ctx, String actionCode, long durationMs) {
        if (actionCode == null) {
            Log.e(TAG, "Intent was missing action.");
            return;
        }
        switch (actionCode) {
            case ACTION_BLE_SCAN_UNOPTIMIZED:
                doUnoptimizedBleScan(ctx, durationMs);
                break;
            case ACTION_WAKE_LOCK:
                doHoldWakelock(ctx, durationMs);
            default:
                Log.e(TAG, "Intent had invalid action");
        }
    }

    private static void doUnoptimizedBleScan(Context ctx, long durationMs) {
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        // perform ble scanning
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() ) {
            Log.e(TAG, "Device does not support Bluetooth or Bluetooth not enabled");
            return;
        }
        BluetoothLeScanner bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Log.e(TAG, "Cannot access BLE scanner");
            return;
        }

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.v(TAG, "called onScanResult");
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.v(TAG, "called onScanFailed");
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.v(TAG, "called onBatchScanResults");
            }
        };

        bleScanner.startScan(null, scanSettings, scanCallback);
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread couldn't sleep for " + durationMs, e);
        }
        bleScanner.stopScan(scanCallback);
    }

    private static void doHoldWakelock(Context ctx, long durationMs) {
        PowerManager powerManager = ctx.getSystemService(PowerManager.class);
        PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "AnomalyWakeLock");
        wl.acquire();
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread couldn't sleep for " + durationMs, e);
        }
        wl.release();
    }
}
