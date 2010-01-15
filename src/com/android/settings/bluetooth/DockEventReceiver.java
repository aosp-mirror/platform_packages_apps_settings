/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class DockEventReceiver extends BroadcastReceiver {

    private static final boolean DEBUG = DockService.DEBUG;

    private static final String TAG = "DockEventReceiver";

    public static final String ACTION_DOCK_SHOW_UI =
        "com.android.settings.bluetooth.action.DOCK_SHOW_UI";

    private static final int EXTRA_INVALID = -1234;

    private static final Object mStartingServiceSync = new Object();

    private static final long WAKELOCK_TIMEOUT = 5000;

    private static PowerManager.WakeLock mStartingService;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE, EXTRA_INVALID));
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (DEBUG) {
            Log.d(TAG, "Action: " + intent.getAction() + " State:" + state + " Device: "
                    + (device == null ? "null" : device.getName()));
        }

        if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())
                || ACTION_DOCK_SHOW_UI.endsWith(intent.getAction())) {
            if (device == null) {
                if (DEBUG) Log.d(TAG, "Device is missing");
                return;
            }

            switch (state) {
                case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                case Intent.EXTRA_DOCK_STATE_CAR:
                case Intent.EXTRA_DOCK_STATE_DESK:
                    Intent i = new Intent(intent);
                    i.setClass(context, DockService.class);
                    beginStartingService(context, i);
                    break;
                default:
                    if (DEBUG) Log.e(TAG, "Unknown state");
                    break;
            }
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (btState != BluetoothAdapter.STATE_TURNING_ON) {
                Intent i = new Intent(intent);
                i.setClass(context, DockService.class);
                beginStartingService(context, i);
            }
        }
    }

    /**
     * Start the service to process the current event notifications, acquiring
     * the wake lock before returning to ensure that the service will run.
     */
    public static void beginStartingService(Context context, Intent intent) {
        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingDockService");
            }

            mStartingService.acquire(WAKELOCK_TIMEOUT);

            if (context.startService(intent) == null) {
                Log.e(TAG, "Can't start DockService");
            }
        }
    }

    /**
     * Called back by the service when it has finished processing notifications,
     * releasing the wake lock if the service is now stopping.
     */
    public static void finishStartingService(Service service, int startId) {
        synchronized (mStartingServiceSync) {
            if (mStartingService != null) {
                if (DEBUG) Log.d(TAG, "stopSelf id = "+ startId);
                service.stopSelfResult(startId);
            }
        }
    }
}
