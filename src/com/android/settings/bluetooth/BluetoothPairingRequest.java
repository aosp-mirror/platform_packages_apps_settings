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
public final class BluetoothPairingRequest extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
            // convert broadcast intent into activity intent (same action string)
            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.ERROR);
            Intent pairingIntent = new Intent();
            pairingIntent.setClass(context, BluetoothPairingDialog.class);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, type);
            if (type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION ||
                    type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY ||
                    type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
                int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                        BluetoothDevice.ERROR);
                pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_KEY, pairingKey);
            }
            pairingIntent.setAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PowerManager powerManager =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            String deviceAddress = device != null ? device.getAddress() : null;
            if (powerManager.isScreenOn() &&
                    LocalBluetoothPreferences.shouldShowDialogInForeground(context, deviceAddress)) {
                // Since the screen is on and the BT-related activity is in the foreground,
                // just open the dialog
                context.startActivity(pairingIntent);
            } else {
                // Put up a notification that leads to the dialog
                Resources res = context.getResources();
                Notification.Builder builder = new Notification.Builder(context)
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .setTicker(res.getString(R.string.bluetooth_notif_ticker));

                PendingIntent pending = PendingIntent.getActivity(context, 0,
                        pairingIntent, PendingIntent.FLAG_ONE_SHOT);

                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (TextUtils.isEmpty(name)) {
                    name = device != null ? device.getAliasName() :
                            context.getString(android.R.string.unknownName);
                }

                builder.setContentTitle(res.getString(R.string.bluetooth_notif_title))
                        .setContentText(res.getString(R.string.bluetooth_notif_message, name))
                        .setContentIntent(pending)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setColor(res.getColor(
                                com.android.internal.R.color.system_notification_accent_color));

                NotificationManager manager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, builder.getNotification());
            }

        } else if (action.equals(BluetoothDevice.ACTION_PAIRING_CANCEL)) {

            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);

        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);
            int oldState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                    BluetoothDevice.ERROR);
            if((oldState == BluetoothDevice.BOND_BONDING) &&
                    (bondState == BluetoothDevice.BOND_NONE)) {
                // Remove the notification
                NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(NOTIFICATION_ID);
            }
        }
    }
}
