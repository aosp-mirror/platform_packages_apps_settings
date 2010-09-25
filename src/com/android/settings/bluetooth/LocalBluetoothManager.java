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

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Config;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO: have some notion of shutting down.  Maybe a minute after they leave BT settings?
/**
 * LocalBluetoothManager provides a simplified interface on top of a subset of
 * the Bluetooth API.
 */
public class LocalBluetoothManager {
    private static final String TAG = "LocalBluetoothManager";
    static final boolean V = Config.LOGV;
    static final boolean D = Config.LOGD;

    private static final String SHARED_PREFERENCES_NAME = "bluetooth_settings";

    /** Singleton instance. */
    private static LocalBluetoothManager INSTANCE;
    private boolean mInitialized;

    private Context mContext;
    /** If a BT-related activity is in the foreground, this will be it. */
    private Activity mForegroundActivity;
    private AlertDialog mErrorDialog = null;

    private BluetoothAdapter mAdapter;

    private CachedBluetoothDeviceManager mCachedDeviceManager;
    private BluetoothEventRedirector mEventRedirector;
    private BluetoothA2dp mBluetoothA2dp;

    private int mState = BluetoothAdapter.ERROR;

    private List<Callback> mCallbacks = new ArrayList<Callback>();

    private static final int SCAN_EXPIRATION_MS = 5 * 60 * 1000; // 5 mins

    // If a device was picked from the device picker or was in discoverable mode
    // in the last 60 seconds, show the pairing dialogs in foreground instead
    // of raising notifications
    private static long GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND = 60 * 1000;

    public static final String SHARED_PREFERENCES_KEY_DISCOVERING_TIMESTAMP =
        "last_discovering_time";

    private static final String SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE =
        "last_selected_device";

    private static final String SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE_TIME =
        "last_selected_device_time";

    private static final String SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT = "auto_connect_to_dock";

    private long mLastScan;

    public static LocalBluetoothManager getInstance(Context context) {
        synchronized (LocalBluetoothManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new LocalBluetoothManager();
            }

            if (!INSTANCE.init(context)) {
                return null;
            }

            LocalBluetoothProfileManager.init(INSTANCE);

            return INSTANCE;
        }
    }

    private boolean init(Context context) {
        if (mInitialized) return true;
        mInitialized = true;

        // This will be around as long as this process is
        mContext = context.getApplicationContext();

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            return false;
        }

        mCachedDeviceManager = new CachedBluetoothDeviceManager(this);

        mEventRedirector = new BluetoothEventRedirector(this);
        mEventRedirector.start();

        mBluetoothA2dp = new BluetoothA2dp(context);

        return true;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mAdapter;
    }

    public Context getContext() {
        return mContext;
    }

    public Activity getForegroundActivity() {
        return mForegroundActivity;
    }

    public void setForegroundActivity(Activity activity) {
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
        mForegroundActivity = activity;
    }

    public SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public CachedBluetoothDeviceManager getCachedDeviceManager() {
        return mCachedDeviceManager;
    }

    List<Callback> getCallbacks() {
        return mCallbacks;
    }

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    public void startScanning(boolean force) {
        if (mAdapter.isDiscovering()) {
            /*
             * Already discovering, but give the callback that information.
             * Note: we only call the callbacks, not the same path as if the
             * scanning state had really changed (in that case the device
             * manager would clear its list of unpaired scanned devices).
             */
            dispatchScanningStateChanged(true);
        } else {
            if (!force) {
                // Don't scan more than frequently than SCAN_EXPIRATION_MS,
                // unless forced
                if (mLastScan + SCAN_EXPIRATION_MS > System.currentTimeMillis()) {
                    return;
                }

                // If we are playing music, don't scan unless forced.
                Set<BluetoothDevice> sinks = mBluetoothA2dp.getConnectedSinks();
                if (sinks != null) {
                    for (BluetoothDevice sink : sinks) {
                        if (mBluetoothA2dp.getSinkState(sink) == BluetoothA2dp.STATE_PLAYING) {
                            return;
                        }
                    }
                }
            }

            if (mAdapter.startDiscovery()) {
                mLastScan = System.currentTimeMillis();
            }
        }
    }

    public void stopScanning() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    public int getBluetoothState() {

        if (mState == BluetoothAdapter.ERROR) {
            syncBluetoothState();
        }

        return mState;
    }

    void setBluetoothStateInt(int state) {
        mState = state;
        if (state == BluetoothAdapter.STATE_ON ||
            state == BluetoothAdapter.STATE_OFF) {
            mCachedDeviceManager.onBluetoothStateChanged(state ==
                    BluetoothAdapter.STATE_ON);
        }
    }

    private void syncBluetoothState() {
        int bluetoothState;

        if (mAdapter != null) {
            bluetoothState = mAdapter.isEnabled()
                    ? BluetoothAdapter.STATE_ON
                    : BluetoothAdapter.STATE_OFF;
        } else {
            bluetoothState = BluetoothAdapter.ERROR;
        }

        setBluetoothStateInt(bluetoothState);
    }

    public void setBluetoothEnabled(boolean enabled) {
        boolean wasSetStateSuccessful = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (wasSetStateSuccessful) {
            setBluetoothStateInt(enabled
                ? BluetoothAdapter.STATE_TURNING_ON
                : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
            if (V) {
                Log.v(TAG,
                        "setBluetoothEnabled call, manager didn't return success for enabled: "
                                + enabled);
            }

            syncBluetoothState();
        }
    }

    /**
     * @param started True if scanning started, false if scanning finished.
     */
    void onScanningStateChanged(boolean started) {
        // TODO: have it be a callback (once we switch bluetooth state changed to callback)
        mCachedDeviceManager.onScanningStateChanged(started);
        dispatchScanningStateChanged(started);
    }

    private void dispatchScanningStateChanged(boolean started) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onScanningStateChanged(started);
            }
        }
    }

    public void showError(BluetoothDevice device, int titleResId, int messageResId) {
        CachedBluetoothDevice cachedDevice = mCachedDeviceManager.findDevice(device);
        String name = null;
        if (cachedDevice == null) {
            if (device != null) name = device.getName();

            if (name == null) {
                name = mContext.getString(R.string.bluetooth_remote_device);
            }
        } else {
            name = cachedDevice.getName();
        }
        String message = mContext.getString(messageResId, name);

        if (mForegroundActivity != null) {
            // Need an activity context to show a dialog
            mErrorDialog = new AlertDialog.Builder(mForegroundActivity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(titleResId)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        } else {
            // Fallback on a toast
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }
    }

    public interface Callback {
        void onScanningStateChanged(boolean started);
        void onDeviceAdded(CachedBluetoothDevice cachedDevice);
        void onDeviceDeleted(CachedBluetoothDevice cachedDevice);
    }

    public boolean shouldShowDialogInForeground(String deviceAddress) {
        // If Bluetooth Settings is visible
        if (mForegroundActivity != null) return true;

        long currentTimeMillis = System.currentTimeMillis();
        SharedPreferences sharedPreferences = getSharedPreferences();

        // If the device was in discoverABLE mode recently
        long lastDiscoverableEndTime = sharedPreferences.getLong(
                BluetoothDiscoverableEnabler.SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, 0);
        if ((lastDiscoverableEndTime + GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND)
                > currentTimeMillis) {
            return true;
        }

        // If the device was discoverING recently
        if (mAdapter != null && mAdapter.isDiscovering()) {
            return true;
        } else if ((sharedPreferences.getLong(SHARED_PREFERENCES_KEY_DISCOVERING_TIMESTAMP, 0) +
                GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND) > currentTimeMillis) {
            return true;
        }

        // If the device was picked in the device picker recently
        if (deviceAddress != null) {
            String lastSelectedDevice = sharedPreferences.getString(
                    SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE, null);

            if (deviceAddress.equals(lastSelectedDevice)) {
                long lastDeviceSelectedTime = sharedPreferences.getLong(
                        SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE_TIME, 0);
                if ((lastDeviceSelectedTime + GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND)
                        > currentTimeMillis) {
                    return true;
                }
            }
        }
        return false;
    }

    void persistSelectedDeviceInPicker(String deviceAddress) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(LocalBluetoothManager.SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE,
                deviceAddress);
        editor.putLong(LocalBluetoothManager.SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE_TIME,
                System.currentTimeMillis());
        editor.apply();
    }

    public boolean hasDockAutoConnectSetting(String addr) {
        return getSharedPreferences().contains(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr);
    }

    public boolean getDockAutoConnectSetting(String addr) {
        return getSharedPreferences().getBoolean(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr,
                false);
    }

    public void saveDockAutoConnectSetting(String addr, boolean autoConnect) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr, autoConnect);
        editor.apply();
    }

    public void removeDockAutoConnectSetting(String addr) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr);
        editor.apply();
    }
}
