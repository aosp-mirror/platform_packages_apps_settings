/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

/**
 * This class handles the Bluetooth pairing PIN request from the bluetooth service
 * It checks if the BluetoothSettings activity is currently visible and lets that
 * activity handle the request. Otherwise it puts a Notification in the status bar,
 * which can be clicked to bring up the PIN entry dialog. 
 */
public class BluetoothPinRequest extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BluetoothIntent.PAIRING_REQUEST_ACTION)) {
            if (BluetoothSettings.isRunning()) {
                // Let the BluetoothSettings activity handle it
                return;
            } else {
                Resources res = context.getResources();
                String address = intent.getStringExtra(BluetoothIntent.ADDRESS);
                Notification pair = new Notification(
                        android.R.drawable.stat_sys_data_bluetooth,
                        res.getString(R.string.bluetooth_notif_ticker),
                        System.currentTimeMillis());

                Intent pinIntent = new Intent();
                pinIntent.setClass(context, BluetoothPINEntry.class);
                pinIntent.putExtra(BluetoothIntent.ADDRESS, address); 
                pinIntent.setAction(BluetoothIntent.PAIRING_REQUEST_ACTION);
                PendingIntent pending = PendingIntent.getActivity(context, 0, 
                        pinIntent, PendingIntent.FLAG_ONE_SHOT);
                
                String name = intent.getStringExtra(BluetoothIntent.NAME);
                
                if (name == null) {
                    BluetoothDevice bluetooth = 
                        (BluetoothDevice)context.getSystemService(Context.BLUETOOTH_SERVICE);
                    name = bluetooth.getRemoteName(address);
                    if (name == null) {
                        name = address;
                    }
                }
                
                pair.setLatestEventInfo(context, 
                        res.getString(R.string.bluetooth_notif_title), 
                        res.getString(R.string.bluetooth_notif_message) + name, 
                        pending);
                
                NotificationManager manager = (NotificationManager) 
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(0xb100ceee, pair);
            }
        }
    }
}
