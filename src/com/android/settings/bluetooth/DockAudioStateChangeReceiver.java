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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DockAudioStateChangeReceiver extends BroadcastReceiver {

    private static final boolean DBG = true;
    private static final String TAG = "DockAudioStateChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        if (DBG) {
            Log.e(TAG, "Action:" + intent.getAction()
                    + " State:" + intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                            Intent.EXTRA_DOCK_STATE_UNDOCKED));
        }

        if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                if (DBG) Log.e(TAG, "Device is missing");
                return;
            }

            LocalBluetoothManager localManager = LocalBluetoothManager.getInstance(context);

            int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                    Intent.EXTRA_DOCK_STATE_UNDOCKED);

            switch (state) {
                case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                    DockSettingsActivity.handleUndocked(context, localManager, device);
                    break;
                case Intent.EXTRA_DOCK_STATE_CAR:
                case Intent.EXTRA_DOCK_STATE_DESK:
                    if (DockSettingsActivity.getAutoConnectSetting(localManager)) {
                        // Auto connect
                        DockSettingsActivity.handleDocked(context, localManager, device, state);
                    } else {
                        // Don't auto connect. Show dialog.
                        Intent i = new Intent(intent);
                        i.setClass(context, DockSettingsActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown state");
                    break;
            }
        }
    }
}
