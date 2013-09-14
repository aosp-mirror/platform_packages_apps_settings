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

import android.app.QueuedWork;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

/**
 * LocalBluetoothPreferences provides an interface to the preferences
 * related to Bluetooth.
 */
final class LocalBluetoothPreferences {
    private static final String TAG = "LocalBluetoothPreferences";
    private static final boolean DEBUG = Utils.D;
    private static final String SHARED_PREFERENCES_NAME = "bluetooth_settings";

    // If a device was picked from the device picker or was in discoverable mode
    // in the last 60 seconds, show the pairing dialogs in foreground instead
    // of raising notifications
    private static final int GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND = 60 * 1000;

    private static final String KEY_DISCOVERING_TIMESTAMP = "last_discovering_time";

    private static final String KEY_LAST_SELECTED_DEVICE = "last_selected_device";

    private static final String KEY_LAST_SELECTED_DEVICE_TIME = "last_selected_device_time";

    private static final String KEY_DOCK_AUTO_CONNECT = "auto_connect_to_dock";

    private static final String KEY_DISCOVERABLE_END_TIMESTAMP = "discoverable_end_timestamp";

    private LocalBluetoothPreferences() {
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    static long getDiscoverableEndTimestamp(Context context) {
        return getSharedPreferences(context).getLong(
                KEY_DISCOVERABLE_END_TIMESTAMP, 0);
    }

    static boolean shouldShowDialogInForeground(Context context,
            String deviceAddress) {
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
        if (manager == null) {
            if(DEBUG) Log.v(TAG, "manager == null - do not show dialog.");
            return false;
        }

        // If Bluetooth Settings is visible
        if (manager.isForegroundActivity()) {
            return true;
        }

        // If in appliance mode, do not show dialog in foreground.
        if ((context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_TYPE_APPLIANCE) == Configuration.UI_MODE_TYPE_APPLIANCE) {
            if (DEBUG) Log.v(TAG, "in appliance mode - do not show dialog.");
            return false;
        }

        long currentTimeMillis = System.currentTimeMillis();
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        // If the device was in discoverABLE mode recently
        long lastDiscoverableEndTime = sharedPreferences.getLong(
                KEY_DISCOVERABLE_END_TIMESTAMP, 0);
        if ((lastDiscoverableEndTime + GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND)
                > currentTimeMillis) {
            return true;
        }

        // If the device was discoverING recently
        LocalBluetoothAdapter adapter = manager.getBluetoothAdapter();
        if (adapter != null && adapter.isDiscovering()) {
            return true;
        } else if ((sharedPreferences.getLong(KEY_DISCOVERING_TIMESTAMP, 0) +
                GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND) > currentTimeMillis) {
            return true;
        }

        // If the device was picked in the device picker recently
        if (deviceAddress != null) {
            String lastSelectedDevice = sharedPreferences.getString(
                    KEY_LAST_SELECTED_DEVICE, null);

            if (deviceAddress.equals(lastSelectedDevice)) {
                long lastDeviceSelectedTime = sharedPreferences.getLong(
                        KEY_LAST_SELECTED_DEVICE_TIME, 0);
                if ((lastDeviceSelectedTime + GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND)
                        > currentTimeMillis) {
                    return true;
                }
            }
        }
        if (DEBUG) Log.v(TAG, "Found no reason to show the dialog - do not show dialog.");
        return false;
    }

    static void persistSelectedDeviceInPicker(Context context, String deviceAddress) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(KEY_LAST_SELECTED_DEVICE,
                deviceAddress);
        editor.putLong(KEY_LAST_SELECTED_DEVICE_TIME,
                System.currentTimeMillis());
        editor.apply();
    }

    static void persistDiscoverableEndTimestamp(Context context, long endTimestamp) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putLong(KEY_DISCOVERABLE_END_TIMESTAMP, endTimestamp);
        editor.apply();
    }

    static void persistDiscoveringTimestamp(final Context context) {
        // Load the shared preferences and edit it on a background
        // thread (but serialized!).
        QueuedWork.singleThreadExecutor().submit(new Runnable() {
                public void run() {
                    SharedPreferences.Editor editor = getSharedPreferences(context).edit();
                    editor.putLong(
                            KEY_DISCOVERING_TIMESTAMP,
                        System.currentTimeMillis());
                    editor.apply();
                }
            });
    }

    static boolean hasDockAutoConnectSetting(Context context, String addr) {
        return getSharedPreferences(context).contains(KEY_DOCK_AUTO_CONNECT + addr);
    }

    static boolean getDockAutoConnectSetting(Context context, String addr) {
        return getSharedPreferences(context).getBoolean(KEY_DOCK_AUTO_CONNECT + addr,
                false);
    }

    static void saveDockAutoConnectSetting(Context context, String addr, boolean autoConnect) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(KEY_DOCK_AUTO_CONNECT + addr, autoConnect);
        editor.apply();
    }

    static void removeDockAutoConnectSetting(Context context, String addr) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.remove(KEY_DOCK_AUTO_CONNECT + addr);
        editor.apply();
    }
}
