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
            if (com.android.settings.Utils.isManagedProfile(um)) {
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

            // Even if the user has already made the choice, Bluetooth still may not know that if
            // the user preference data have not been migrated from Settings app's shared
            // preferences to Bluetooth app's. In that case, Bluetooth app broadcasts an
            // ACTION_CONNECTION_ACCESS_REQUEST intent to ask to Settings app.
            //
            // If that happens, 'checkUserChoice()' here will do migration because it finds or
            // creates a 'CachedBluetoothDevice' object for the device.
            //
            // After migration is done, 'checkUserChoice()' replies to the request by sending an
            // ACTION_CONNECTION_ACCESS_REPLY intent. And we don't need to start permission activity
            // dialog or notification.
            if (checkUserChoice()) {
                return;
            }

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
            String title = null;
            String message = null;
            PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (powerManager.isScreenOn()
                    && LocalBluetoothPreferences.shouldShowDialogInForeground(
                            context, deviceAddress)) {
                context.startActivity(connectionAccessIntent);
            } else {
                // Put up a notification that leads to the dialog

                // Create an intent triggered by clicking on the
                // "Clear All Notifications" button

                Intent deleteIntent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        BluetoothDevice.CONNECTION_ACCESS_NO);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE, mRequestType);
                String deviceName = mDevice != null ? mDevice.getAliasName() : null;
                switch (mRequestType) {
                    case BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS:
                        title = context.getString(R.string.bluetooth_phonebook_request);
                        message = context.getString(R.string.bluetooth_pb_acceptance_dialog_text,
                                deviceName, deviceName);
                        break;
                    case BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS:
                        title = context.getString(R.string.bluetooth_map_request);
                        message = context.getString(R.string.bluetooth_map_acceptance_dialog_text,
                                deviceName, deviceName);
                        break;
                    default:
                        title = context.getString(R.string.bluetooth_connection_permission_request);
                        message = context.getString(R.string.bluetooth_connection_dialog_text,
                                deviceName, deviceName);
                        break;
                }
                Notification notification = new Notification.Builder(context)
                        .setContentTitle(title)
                        .setTicker(message)
                        .setContentText(message)
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOnlyAlertOnce(false)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentIntent(PendingIntent.getActivity(context, 0,
                                connectionAccessIntent, 0))
                        .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0))
                        .setColor(context.getResources().getColor(
                                com.android.internal.R.color.system_notification_accent_color))
                        .build();

                notification.flags |= Notification.FLAG_NO_CLEAR; // Cannot be set with the builder.

                NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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
        }
        return null;
    }

    /**
     * @return true user had made a choice, this method replies to the request according
     *              to user's previous decision
     *         false user hadnot made any choice on this device
     */
    private boolean checkUserChoice() {
        boolean processed = false;

        // ignore if it is something else than phonebook/message settings it wants us to remember
        if (mRequestType != BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS
                && mRequestType != BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS) {
            if (DEBUG) Log.d(TAG, "checkUserChoice(): Unknown RequestType " + mRequestType);
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

        String intentName = BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY;

        if (mRequestType == BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS) {
            int phonebookPermission = cachedDevice.getPhonebookPermissionChoice();

            if (phonebookPermission == CachedBluetoothDevice.ACCESS_UNKNOWN) {
                // Leave 'processed' as false.
            } else if (phonebookPermission == CachedBluetoothDevice.ACCESS_ALLOWED) {
                sendReplyIntentToReceiver(true);
                processed = true;
            } else if (phonebookPermission == CachedBluetoothDevice.ACCESS_REJECTED) {
                sendReplyIntentToReceiver(false);
                processed = true;
            } else {
                Log.e(TAG, "Bad phonebookPermission: " + phonebookPermission);
            }
        } else if (mRequestType == BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS) {
            int messagePermission = cachedDevice.getMessagePermissionChoice();

            if (messagePermission == CachedBluetoothDevice.ACCESS_UNKNOWN) {
                // Leave 'processed' as false.
            } else if (messagePermission == CachedBluetoothDevice.ACCESS_ALLOWED) {
                sendReplyIntentToReceiver(true);
                processed = true;
            } else if (messagePermission == CachedBluetoothDevice.ACCESS_REJECTED) {
                sendReplyIntentToReceiver(false);
                processed = true;
            } else {
                Log.e(TAG, "Bad messagePermission: " + messagePermission);
            }
        }
        if (DEBUG) Log.d(TAG,"checkUserChoice(): returning " + processed);
        return processed;
    }

    private void sendReplyIntentToReceiver(final boolean allowed) {
        Intent intent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);

        if (mReturnPackage != null && mReturnClass != null) {
            intent.setClassName(mReturnPackage, mReturnClass);
        }

        intent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        allowed ? BluetoothDevice.CONNECTION_ACCESS_YES
                                : BluetoothDevice.CONNECTION_ACCESS_NO);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE, mRequestType);
        mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH_ADMIN);
    }
}
