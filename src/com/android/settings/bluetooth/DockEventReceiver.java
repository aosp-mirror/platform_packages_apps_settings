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
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public final class DockEventReceiver extends BroadcastReceiver {

    private static final boolean DEBUG = DockService.DEBUG;

    private static final String TAG = "DockEventReceiver";

    public static final String ACTION_DOCK_SHOW_UI =
        "com.android.settings.bluetooth.action.DOCK_SHOW_UI";

    private static final int EXTRA_INVALID = -1234;

    private static final Object sStartingServiceSync = new Object();

    private static PowerManager.WakeLock sStartingService;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE, EXTRA_INVALID));
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (DEBUG) {
            Log.d(TAG, "Action: " + intent.getAction() + " State:" + state + " Device: "
                    + (device == null ? "null" : device.getAliasName()));
        }

        if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())
                || ACTION_DOCK_SHOW_UI.endsWith(intent.getAction())) {
            if ((device == null) && (ACTION_DOCK_SHOW_UI.endsWith(intent.getAction()) ||
                    ((state != Intent.EXTRA_DOCK_STATE_UNDOCKED) &&
                     (state != Intent.EXTRA_DOCK_STATE_LE_DESK)))) {
                if (DEBUG) Log.d(TAG,
                        "Wrong state: "+state+" or intent: "+intent.toString()+" with null device");
                return;
            }

            switch (state) {
                case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                case Intent.EXTRA_DOCK_STATE_CAR:
                case Intent.EXTRA_DOCK_STATE_DESK:
                case Intent.EXTRA_DOCK_STATE_LE_DESK:
                case Intent.EXTRA_DOCK_STATE_HE_DESK:
                    Intent i = new Intent(intent);
                    i.setClass(context, DockService.class);
                    beginStartingService(context, i);
                    break;
                default:
                    Log.e(TAG, "Unknown state: " + state);
                    break;
            }
        } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction()) ||
                   BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_CONNECTED);
            int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);

            /*
             *  Reconnect to the dock if:
             *  1) it is a dock
             *  2) it is disconnected
             *  3) the disconnect is initiated remotely
             *  4) the dock is still docked (check can only be done in the Service)
             */
            if (device == null) {
                if (DEBUG) Log.d(TAG, "Device is missing");
                return;
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED &&
                    oldState != BluetoothProfile.STATE_DISCONNECTING) {
                // Too bad, the dock state can't be checked from a BroadcastReceiver.
                Intent i = new Intent(intent);
                i.setClass(context, DockService.class);
                beginStartingService(context, i);
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
    private static void beginStartingService(Context context, Intent intent) {
        synchronized (sStartingServiceSync) {
            if (sStartingService == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                sStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingDockService");
            }

            sStartingService.acquire();

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
        synchronized (sStartingServiceSync) {
            if (sStartingService != null) {
                if (DEBUG) Log.d(TAG, "stopSelf id = " + startId);
                if (service.stopSelfResult(startId)) {
                    Log.d(TAG, "finishStartingService: stopping service");
                    sStartingService.release();
                }
            }
        }
    }
}
