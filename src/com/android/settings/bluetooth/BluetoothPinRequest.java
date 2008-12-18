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

package com.android.settings.bluetooth;

import com.android.settings.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;

/**
 * BluetoothPinRequest is a receiver for any Bluetooth pairing PIN request. It
 * checks if the Bluetooth Settings is currently visible and brings up the PIN
 * entry dialog. Otherwise it puts a Notification in the status bar, which can
 * be clicked to bring up the PIN entry dialog.
 */
public class BluetoothPinRequest extends BroadcastReceiver {

    public static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothIntent.PAIRING_REQUEST_ACTION)) {

            LocalBluetoothManager localManager = LocalBluetoothManager.getInstance(context);        
        
            String address = intent.getStringExtra(BluetoothIntent.ADDRESS);
            Intent pinIntent = new Intent();
            pinIntent.setClass(context, BluetoothPinDialog.class);
            pinIntent.putExtra(BluetoothIntent.ADDRESS, address); 
            pinIntent.setAction(BluetoothIntent.PAIRING_REQUEST_ACTION);
            pinIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (localManager.getForegroundActivity() != null) {
                // Since the BT-related activity is in the foreground, just open the dialog
                context.startActivity(pinIntent);
                
            } else {
                
                // Put up a notification that leads to the dialog
                Resources res = context.getResources();
                Notification notification = new Notification(
                        android.R.drawable.stat_sys_data_bluetooth,
                        res.getString(R.string.bluetooth_notif_ticker),
                        System.currentTimeMillis());

                PendingIntent pending = PendingIntent.getActivity(context, 0, 
                        pinIntent, PendingIntent.FLAG_ONE_SHOT);
                
                String name = intent.getStringExtra(BluetoothIntent.NAME);
                if (TextUtils.isEmpty(name)) {
                    name = localManager.getLocalDeviceManager().getName(address);
                }
                
                notification.setLatestEventInfo(context, 
                        res.getString(R.string.bluetooth_notif_title), 
                        res.getString(R.string.bluetooth_notif_message) + name, 
                        pending);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                
                NotificationManager manager = (NotificationManager) 
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, notification);
            }
            
        } else if (action.equals(BluetoothIntent.PAIRING_CANCEL_ACTION)) {
            
            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
