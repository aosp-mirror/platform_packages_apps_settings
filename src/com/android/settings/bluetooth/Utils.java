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
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothUtils.ErrorListener;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
                return com.android.settingslib.R.string.bluetooth_connected;
            case BluetoothProfile.STATE_CONNECTING:
                return com.android.settingslib.R.string.bluetooth_connecting;
            case BluetoothProfile.STATE_DISCONNECTED:
                return com.android.settingslib.R.string.bluetooth_disconnected;
            case BluetoothProfile.STATE_DISCONNECTING:
                return com.android.settingslib.R.string.bluetooth_disconnecting;
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
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().visible(context,
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

    /**
     * Obtains a {@link LocalBluetoothManager}.
     *
     * To avoid StrictMode ThreadPolicy violation, will get it in another thread.
     */
    public static LocalBluetoothManager getLocalBluetoothManager(Context context) {
        final FutureTask<LocalBluetoothManager> localBtManagerFutureTask = new FutureTask<>(
                // Avoid StrictMode ThreadPolicy violation
                () -> getLocalBtManager(context));
        try {
            localBtManagerFutureTask.run();
            return localBtManagerFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting LocalBluetoothManager.", e);
            return null;
        }
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

    /**
     * Returns all cachedBluetoothDevices with the same groupId.
     * @param cachedBluetoothDevice The main cachedBluetoothDevice.
     * @return all cachedBluetoothDevices with the same groupId.
     */
    public static List<CachedBluetoothDevice> getAllOfCachedBluetoothDevices(
            LocalBluetoothManager localBtMgr,
            CachedBluetoothDevice cachedBluetoothDevice) {
        List<CachedBluetoothDevice> cachedBluetoothDevices = new ArrayList<>();
        if (cachedBluetoothDevice == null) {
            Log.e(TAG, "getAllOfCachedBluetoothDevices: no cachedBluetoothDevice");
            return cachedBluetoothDevices;
        }
        int deviceGroupId = cachedBluetoothDevice.getGroupId();
        if (deviceGroupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
            cachedBluetoothDevices.add(cachedBluetoothDevice);
            return cachedBluetoothDevices;
        }

        if (localBtMgr == null) {
            Log.e(TAG, "getAllOfCachedBluetoothDevices: no LocalBluetoothManager");
            return cachedBluetoothDevices;
        }
        CachedBluetoothDevice mainDevice =
                localBtMgr.getCachedDeviceManager().getCachedDevicesCopy().stream()
                        .filter(cachedDevice -> cachedDevice.getGroupId() == deviceGroupId)
                        .findFirst().orElse(null);
        if (mainDevice == null) {
            Log.e(TAG, "getAllOfCachedBluetoothDevices: groupId = " + deviceGroupId
                    + ", no main device.");
            return cachedBluetoothDevices;
        }
        cachedBluetoothDevice = mainDevice;
        cachedBluetoothDevices.add(cachedBluetoothDevice);
        for (CachedBluetoothDevice member : cachedBluetoothDevice.getMemberDevice()) {
            cachedBluetoothDevices.add(member);
        }
        Log.d(TAG, "getAllOfCachedBluetoothDevices: groupId = " + deviceGroupId
                + " , cachedBluetoothDevice = " + cachedBluetoothDevice
                + " , deviceList = " + cachedBluetoothDevices);
        return cachedBluetoothDevices;
    }
}
