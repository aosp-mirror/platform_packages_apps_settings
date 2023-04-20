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

import static android.os.Process.BLUETOOTH_UID;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothUtils.ErrorListener;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;

/**
 * Utils is a helper class that contains constants for various
 * Android resource IDs, debug logging flags, and static methods
 * for creating dialogs.
 */
public final class Utils {

    private static final String TAG = "BluetoothUtils";

    static final boolean V = BluetoothUtils.V; // verbose logging
    static final boolean D = BluetoothUtils.D;  // regular logging

    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case BluetoothProfile.STATE_CONNECTED:
                return R.string.bluetooth_connected;
            case BluetoothProfile.STATE_CONNECTING:
                return R.string.bluetooth_connecting;
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_disconnected;
            case BluetoothProfile.STATE_DISCONNECTING:
                return R.string.bluetooth_disconnecting;
            default:
                return 0;
        }
    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context,
            AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener,
            CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    @VisibleForTesting
    static void showConnectingError(Context context, String name, LocalBluetoothManager manager) {
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().visible(context,
                SettingsEnums.PAGE_UNKNOWN, SettingsEnums.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR,
                0);
        showError(context, name, R.string.bluetooth_connecting_error_message, manager);
    }

    static void showError(Context context, String name, int messageResId) {
        showError(context, name, messageResId, getLocalBtManager(context));
    }

    private static void showError(Context context, String name, int messageResId,
            LocalBluetoothManager manager) {
        String message = context.getString(messageResId, name);
        Context activity = manager.getForegroundActivity();
        if (manager.isForegroundActivity()) {
            try {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.bluetooth_error_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "Cannot show error dialog.", e);
            }
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }

    public static String createRemoteName(Context context, BluetoothDevice device) {
        String mRemoteName = device != null ? device.getAlias() : null;

        if (mRemoteName == null) {
            mRemoteName = context.getString(R.string.unknown);
        }
        return mRemoteName;
    }

    private static final ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onShowError(Context context, String name, int messageResId) {
            showError(context, name, messageResId);
        }
    };

    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
        @Override
        public void onBluetoothManagerInitialized(Context appContext,
                LocalBluetoothManager bluetoothManager) {
            BluetoothUtils.setErrorListener(mErrorListener);
        }
    };

    public static boolean isBluetoothScanningEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1;
    }

    /**
     * Check if the Bluetooth device supports advanced details header
     *
     * @param bluetoothDevice the BluetoothDevice to get metadata
     * @return true if it supports advanced details header, false otherwise.
     */
    public static boolean isAdvancedDetailsHeader(@NonNull BluetoothDevice bluetoothDevice) {
        final boolean advancedEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, true);
        if (!advancedEnabled) {
            Log.d(TAG, "isAdvancedDetailsHeader: advancedEnabled is false");
            return false;
        }
        // The metadata is for Android R
        final boolean untetheredHeadset = BluetoothUtils.getBooleanMetaData(bluetoothDevice,
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET);
        if (untetheredHeadset) {
            Log.d(TAG, "isAdvancedDetailsHeader: untetheredHeadset is true");
            return true;
        }
        // The metadata is for Android S
        final String deviceType = BluetoothUtils.getStringMetaData(bluetoothDevice,
                BluetoothDevice.METADATA_DEVICE_TYPE);
        if (TextUtils.equals(deviceType, BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET)
                || TextUtils.equals(deviceType, BluetoothDevice.DEVICE_TYPE_WATCH)
                || TextUtils.equals(deviceType, BluetoothDevice.DEVICE_TYPE_DEFAULT)) {
            Log.d(TAG, "isAdvancedDetailsHeader: deviceType is " + deviceType);
            return true;
        }
        return false;
    }

    /**
     * Returns the Bluetooth Package name
     */
    public static String findBluetoothPackageName(Context context)
            throws NameNotFoundException {
        // this activity will always be in the package where the rest of Bluetooth lives
        final String sentinelActivity = "com.android.bluetooth.opp.BluetoothOppLauncherActivity";
        PackageManager packageManager = context.createContextAsUser(UserHandle.SYSTEM, 0)
                .getPackageManager();
        String[] allPackages = packageManager.getPackagesForUid(BLUETOOTH_UID);
        String matchedPackage = null;
        for (String candidatePackage : allPackages) {
            PackageInfo packageInfo;
            try {
                packageInfo =
                        packageManager.getPackageInfo(
                                candidatePackage,
                                PackageManager.GET_ACTIVITIES
                                        | PackageManager.MATCH_ANY_USER
                                        | PackageManager.MATCH_UNINSTALLED_PACKAGES
                                        | PackageManager.MATCH_DISABLED_COMPONENTS);
            } catch (NameNotFoundException e) {
                // rethrow
                throw e;
            }
            if (packageInfo.activities == null) {
                continue;
            }
            for (ActivityInfo activity : packageInfo.activities) {
                if (sentinelActivity.equals(activity.name)) {
                    if (matchedPackage == null) {
                        matchedPackage = candidatePackage;
                    } else {
                        throw new NameNotFoundException("multiple main bluetooth packages found");
                    }
                }
            }
        }
        if (matchedPackage != null) {
            return matchedPackage;
        }
        throw new NameNotFoundException("Could not find main bluetooth package");
    }
}
