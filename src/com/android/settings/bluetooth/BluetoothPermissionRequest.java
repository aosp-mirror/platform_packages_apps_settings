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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.R;

/**
 * BluetoothPermissionRequest is a receiver to receive Bluetooth connection
 * access request.
 */
public final class BluetoothPermissionRequest extends BroadcastReceiver {

    private static final String TAG = "BluetoothPermissionRequest";
    private static final boolean DEBUG = Utils.V;
    private static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;

    private static final String NOTIFICATION_TAG_PBAP = "Phonebook Access" ;
    private static final String NOTIFICATION_TAG_MAP = "Message Access";
    private static final String NOTIFICATION_TAG_SAP = "SIM Access";
    /* TODO: Consolidate this multiple defined but common channel ID with other
     * handlers that declare and use the same channel ID */
    private static final String BLUETOOTH_NOTIFICATION_CHANNEL =
        "bluetooth_notification_channel";

    private NotificationChannel mNotificationChannel = null;

    Context mContext;
    int mRequestType;
    BluetoothDevice mDevice;
    String mReturnPackage = null;
    String mReturnClass = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();

        if (DEBUG) Log.d(TAG, "onReceive" + action);

        if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_REQUEST)) {
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            // skip the notification for managed profiles.
            if (um.isManagedProfile()) {
                if (DEBUG) Log.d(TAG, "Blocking notification for managed profile.");
                return;
            }
            // convert broadcast intent into activity intent (same action string)
            mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            mRequestType = intent.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                                 BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION);
            mReturnPackage = intent.getStringExtra(BluetoothDevice.EXTRA_PACKAGE_NAME);
            mReturnClass = intent.getStringExtra(BluetoothDevice.EXTRA_CLASS_NAME);

            if (DEBUG) Log.d(TAG, "onReceive request type: " + mRequestType + " return "
                    + mReturnPackage + "," + mReturnClass);

            Intent connectionAccessIntent = new Intent(action);
            connectionAccessIntent.setClass(context, BluetoothPermissionActivity.class);
            // We use the FLAG_ACTIVITY_MULTIPLE_TASK since we can have multiple concurrent access
            // requests.
            connectionAccessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            // This is needed to create two pending intents to the same activity. The value is not
            // used in the activity.
            connectionAccessIntent.setType(Integer.toString(mRequestType));
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                            mRequestType);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_PACKAGE_NAME, mReturnPackage);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_CLASS_NAME, mReturnClass);

            String deviceAddress = mDevice != null ? mDevice.getAddress() : null;
            String deviceName = mDevice != null ? mDevice.getName() : null;
            String title = null;
            String message = null;
            PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (powerManager.isScreenOn()
                    && LocalBluetoothPreferences.shouldShowDialogInForeground(
                            context, deviceAddress, deviceName)) {
                context.startActivity(connectionAccessIntent);
            } else {
                // Put up a notification that leads to the dialog

                // Create an intent triggered by clicking on the
                // "Clear All Notifications" button

                Intent deleteIntent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);
                deleteIntent.setPackage("com.android.bluetooth");
                deleteIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        BluetoothDevice.CONNECTION_ACCESS_NO);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE, mRequestType);
                String deviceAlias = Utils.createRemoteName(context, mDevice);
                switch (mRequestType) {
                    case BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS:
                        title = context.getString(R.string.bluetooth_phonebook_request);
                        message = context.getString(
                                R.string.bluetooth_phonebook_access_notification_content);
                        break;
                    case BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS:
                        title = context.getString(R.string.bluetooth_map_request);
                        message = context.getString(
                                R.string.bluetooth_message_access_notification_content);
                        break;
                    case BluetoothDevice.REQUEST_TYPE_SIM_ACCESS:
                        title = context.getString(R.string.bluetooth_sap_request);
                        message = context.getString(R.string.bluetooth_sap_acceptance_dialog_text,
                                deviceAlias, deviceAlias);
                        break;
                    default:
                        title = context.getString(R.string.bluetooth_connection_permission_request);
                        message = context.getString(R.string.bluetooth_connection_dialog_text,
                                deviceAlias, deviceAlias);
                        break;
                }
                NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (mNotificationChannel == null) {
                    mNotificationChannel = new NotificationChannel(BLUETOOTH_NOTIFICATION_CHANNEL,
                            context.getString(R.string.bluetooth),
                            NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(mNotificationChannel);
                }
                Notification notification = new Notification.Builder(context,
                        BLUETOOTH_NOTIFICATION_CHANNEL)
                        .setContentTitle(title)
                        .setTicker(message)
                        .setContentText(message)
                        .setStyle(new Notification.BigTextStyle().bigText(message))
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOnlyAlertOnce(false)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentIntent(PendingIntent.getActivity(context, 0,
                                connectionAccessIntent, PendingIntent.FLAG_IMMUTABLE))
                        .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent,
                                PendingIntent.FLAG_IMMUTABLE))
                        .setColor(context.getColor(
                                com.android.internal.R.color.system_notification_accent_color))
                        .setLocalOnly(true)
                        .build();

                notification.flags |= Notification.FLAG_NO_CLEAR; // Cannot be set with the builder.

                notificationManager.notify(getNotificationTag(mRequestType), NOTIFICATION_ID,
                        notification);
            }
        } else if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL)) {
            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            mRequestType = intent.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                        BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS);
            manager.cancel(getNotificationTag(mRequestType), NOTIFICATION_ID);
        }
    }

    private String getNotificationTag(int requestType) {
        if(requestType == BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS) {
            return NOTIFICATION_TAG_PBAP;
        } else if(mRequestType == BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS) {
            return NOTIFICATION_TAG_MAP;
        } else if(mRequestType == BluetoothDevice.REQUEST_TYPE_SIM_ACCESS) {
            return NOTIFICATION_TAG_SAP;
        }
        return null;
    }
}
