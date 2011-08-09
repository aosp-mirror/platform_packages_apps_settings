/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.android.settings.R;

/**
 * BluetoothPermissionRequest is a receiver to receive Bluetooth connection
 * access request.
 */
public final class BluetoothPermissionRequest extends BroadcastReceiver {

    private static final String TAG = "BluetoothPermissionRequest";
    private static final boolean DEBUG = Utils.V;
    public static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;

    Context mContext;
    int mRequestType;
    BluetoothDevice mDevice;
    String mReturnPackage = null;
    String mReturnClass = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();

        if (DEBUG) Log.d(TAG, "onReceive");

        if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_REQUEST)) {
            // convert broadcast intent into activity intent (same action string)
            mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            mRequestType = intent.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                                 BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION);
            mReturnPackage = intent.getStringExtra(BluetoothDevice.EXTRA_PACKAGE_NAME);
            mReturnClass = intent.getStringExtra(BluetoothDevice.EXTRA_CLASS_NAME);

            Intent connectionAccessIntent = new Intent(action);
            connectionAccessIntent.setClass(context, BluetoothPermissionActivity.class);
            connectionAccessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                            mRequestType);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_PACKAGE_NAME, mReturnPackage);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_CLASS_NAME, mReturnClass);

            // Check if user had made decisions on accepting or rejecting the phonebook access
            // request. If there is, reply the request and return, no need to start permission
            // activity dialog or notification.
            if (checkUserChoice()) {
                return;
            }

            String deviceAddress = mDevice != null ? mDevice.getAddress() : null;

            PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (powerManager.isScreenOn() &&
                LocalBluetoothPreferences.shouldShowDialogInForeground(context, deviceAddress) ) {
                context.startActivity(connectionAccessIntent);
            } else {
                // Put up a notification that leads to the dialog

                // Create an intent triggered by clicking on the
                // "Clear All Notifications" button

                Intent deleteIntent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        BluetoothDevice.CONNECTION_ACCESS_NO);

                Notification notification = new Notification(
                    android.R.drawable.stat_sys_data_bluetooth,
                    context.getString(R.string.bluetooth_connection_permission_request),
                    System.currentTimeMillis());
                String deviceName = mDevice != null ? mDevice.getAliasName() : null;
                notification.setLatestEventInfo(context,
                    context.getString(R.string.bluetooth_connection_permission_request),
                    context.getString(R.string.bluetooth_connection_notif_message, deviceName),
                    PendingIntent.getActivity(context, 0, connectionAccessIntent, 0));
                notification.flags = Notification.FLAG_AUTO_CANCEL |
                                     Notification.FLAG_ONLY_ALERT_ONCE;
                notification.defaults = Notification.DEFAULT_SOUND;
                notification.deleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

                NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        } else if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL)) {
            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);
        }
    }

    /**
     * @return true user had made a choice, this method replies to the request according
     *              to user's previous decision
     *         false user hadnot made any choice on this device
     */
    private boolean checkUserChoice() {
        boolean processed = false;

        // we only remember PHONEBOOK permission
        if (mRequestType != BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS) {
            return processed;
        }

        LocalBluetoothManager bluetoothManager = LocalBluetoothManager.getInstance(mContext);
        CachedBluetoothDeviceManager cachedDeviceManager =
            bluetoothManager.getCachedDeviceManager();
        CachedBluetoothDevice cachedDevice = cachedDeviceManager.findDevice(mDevice);

        if (cachedDevice == null) {
            cachedDevice = cachedDeviceManager.addDevice(bluetoothManager.getBluetoothAdapter(),
                bluetoothManager.getProfileManager(), mDevice);
        }

        int phonebookPermission = cachedDevice.getPhonebookPermissionChoice();

        if (phonebookPermission == CachedBluetoothDevice.PHONEBOOK_ACCESS_UNKNOWN) {
            return processed;
        }

        String intentName = BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY;
        if (phonebookPermission == CachedBluetoothDevice.PHONEBOOK_ACCESS_ALLOWED) {
            sendIntentToReceiver(intentName, true, BluetoothDevice.EXTRA_ALWAYS_ALLOWED, true);
            processed = true;
        } else if (phonebookPermission == CachedBluetoothDevice.PHONEBOOK_ACCESS_REJECTED) {
            sendIntentToReceiver(intentName, false,
                                 null, false // dummy value, no effect since previous param is null
                                 );
            processed = true;
        } else {
            Log.e(TAG, "Bad phonebookPermission: " + phonebookPermission);
        }
        return processed;
    }

    private void sendIntentToReceiver(final String intentName, final boolean allowed,
                                      final String extraName, final boolean extraValue) {
        Intent intent = new Intent(intentName);

        if (mReturnPackage != null && mReturnClass != null) {
            intent.setClassName(mReturnPackage, mReturnClass);
        }

        intent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        allowed ? BluetoothDevice.CONNECTION_ACCESS_YES :
                        BluetoothDevice.CONNECTION_ACCESS_NO);

        if (extraName != null) {
            intent.putExtra(extraName, extraValue);
        }
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH_ADMIN);
    }
}
