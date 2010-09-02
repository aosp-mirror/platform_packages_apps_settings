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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.os.PowerManager;

/**
 * BluetoothPairingRequest is a receiver for any Bluetooth pairing request. It
 * checks if the Bluetooth Settings is currently visible and brings up the PIN, the passkey or a
 * confirmation entry dialog. Otherwise it puts a Notification in the status bar, which can
 * be clicked to bring up the Pairing entry dialog.
 */
public class BluetoothPairingRequest extends BroadcastReceiver {

    public static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {

            LocalBluetoothManager localManager = LocalBluetoothManager.getInstance(context);

            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.ERROR);
            Intent pairingIntent = new Intent();
            pairingIntent.setClass(context, BluetoothPairingDialog.class);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, type);
            if (type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION ||
                    type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
                int passkey = intent.getIntExtra(BluetoothDevice.EXTRA_PASSKEY, BluetoothDevice.ERROR);
                pairingIntent.putExtra(BluetoothDevice.EXTRA_PASSKEY, passkey);
            }
            pairingIntent.setAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PowerManager powerManager =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            String deviceAddress = device != null ? device.getAddress() : null;
            if (powerManager.isScreenOn() &&
                localManager.shouldShowDialogInForeground(deviceAddress)) {
                // Since the screen is on and the BT-related activity is in the foreground,
                // just open the dialog
                context.startActivity(pairingIntent);

            } else {

                // Put up a notification that leads to the dialog
                Resources res = context.getResources();
                Notification notification = new Notification(
                        android.R.drawable.stat_sys_data_bluetooth,
                        res.getString(R.string.bluetooth_notif_ticker),
                        System.currentTimeMillis());

                PendingIntent pending = PendingIntent.getActivity(context, 0,
                        pairingIntent, PendingIntent.FLAG_ONE_SHOT);

                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (TextUtils.isEmpty(name)) {
                    name = device.getName();
                }

                notification.setLatestEventInfo(context,
                        res.getString(R.string.bluetooth_notif_title),
                        res.getString(R.string.bluetooth_notif_message) + name,
                        pending);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.defaults |= Notification.DEFAULT_SOUND;

                NotificationManager manager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, notification);
            }

        } else if (action.equals(BluetoothDevice.ACTION_PAIRING_CANCEL)) {

            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
