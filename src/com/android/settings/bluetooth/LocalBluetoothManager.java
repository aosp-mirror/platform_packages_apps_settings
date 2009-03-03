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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

// TODO: have some notion of shutting down.  Maybe a minute after they leave BT settings?
/**
 * LocalBluetoothManager provides a simplified interface on top of a subset of
 * the Bluetooth API.
 */
public class LocalBluetoothManager {
    private static final String TAG = "LocalBluetoothManager";
    static final boolean V = true;
    
    public static final String EXTENDED_BLUETOOTH_STATE_CHANGED_ACTION =
        "com.android.settings.bluetooth.intent.action.EXTENDED_BLUETOOTH_STATE_CHANGED";
    private static final String SHARED_PREFERENCES_NAME = "bluetooth_settings";
    
    private static LocalBluetoothManager INSTANCE;
    /** Used when obtaining a reference to the singleton instance. */
    private static Object INSTANCE_LOCK = new Object();
    private boolean mInitialized;
    
    private Context mContext;
    /** If a BT-related activity is in the foreground, this will be it. */
    private Activity mForegroundActivity;
    private AlertDialog mErrorDialog = null;

    private BluetoothDevice mManager;

    private LocalBluetoothDeviceManager mLocalDeviceManager;
    private BluetoothEventRedirector mEventRedirector;
    private BluetoothA2dp mBluetoothA2dp;
    
    public static enum ExtendedBluetoothState { ENABLED, ENABLING, DISABLED, DISABLING, UNKNOWN }
    private ExtendedBluetoothState mState = ExtendedBluetoothState.UNKNOWN;

    private List<Callback> mCallbacks = new ArrayList<Callback>();
    
    private static final int SCAN_EXPIRATION_MS = 5 * 60 * 1000; // 5 mins
    private long mLastScan;
    
    public static LocalBluetoothManager getInstance(Context context) {
        synchronized (INSTANCE_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new LocalBluetoothManager();
            }
            
            if (!INSTANCE.init(context)) {
                return null;
            }
            
            return INSTANCE;
        }
    }

    private boolean init(Context context) {
        if (mInitialized) return true;
        mInitialized = true;
        
        // This will be around as long as this process is
        mContext = context.getApplicationContext();
        
        mManager = (BluetoothDevice) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mManager == null) {
            return false;
        }
        
        mLocalDeviceManager = new LocalBluetoothDeviceManager(this);

        mEventRedirector = new BluetoothEventRedirector(this);
        mEventRedirector.start();

        mBluetoothA2dp = new BluetoothA2dp(context);

        return true;
    }
    
    public BluetoothDevice getBluetoothManager() {
        return mManager;
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
    
    public LocalBluetoothDeviceManager getLocalDeviceManager() {
        return mLocalDeviceManager;
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
        if (mManager.isDiscovering()) {
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
                List<String> sinks = mBluetoothA2dp.listConnectedSinks();
                if (sinks != null) {
                    for (String address : sinks) {
                        if (mBluetoothA2dp.getSinkState(address) == BluetoothA2dp.STATE_PLAYING) {
                            return;
                        }
                    }
                }
            }
            
            if (mManager.startDiscovery(true)) {
                mLastScan = System.currentTimeMillis();
            }
        }
    }
    
    public ExtendedBluetoothState getBluetoothState() {
        
        if (mState == ExtendedBluetoothState.UNKNOWN) {
            syncBluetoothState();
        }
            
        return mState;
    }
    
    void setBluetoothStateInt(ExtendedBluetoothState state) {
        mState = state;
        
        /*
         * TODO: change to callback method. originally it was broadcast to
         * parallel the framework's method, but it just complicates things here.
         */
        // If this were a real API, I'd add as an extra
        mContext.sendBroadcast(new Intent(EXTENDED_BLUETOOTH_STATE_CHANGED_ACTION));
        
        if (state == ExtendedBluetoothState.ENABLED || state == ExtendedBluetoothState.DISABLED) {
            mLocalDeviceManager.onBluetoothStateChanged(state == ExtendedBluetoothState.ENABLED);
        }
    }
    
    private void syncBluetoothState() {
        setBluetoothStateInt(mManager.isEnabled()
                ? ExtendedBluetoothState.ENABLED
                : ExtendedBluetoothState.DISABLED);
    }

    public void setBluetoothEnabled(boolean enabled) {
        boolean wasSetStateSuccessful = enabled
                ? mManager.enable()
                : mManager.disable();
                
        if (wasSetStateSuccessful) {
            setBluetoothStateInt(enabled
                    ? ExtendedBluetoothState.ENABLING
                    : ExtendedBluetoothState.DISABLING);
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
        mLocalDeviceManager.onScanningStateChanged(started);
        dispatchScanningStateChanged(started);
    }
    
    private void dispatchScanningStateChanged(boolean started) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onScanningStateChanged(started);
            }
        }
    }

    public void showError(String address, int titleResId, int messageResId) {
        LocalBluetoothDevice device = mLocalDeviceManager.findDevice(address);
        if (device == null) return;

        String name = device.getName();
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
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    public interface Callback {
        void onScanningStateChanged(boolean started);
        void onDeviceAdded(LocalBluetoothDevice device);
        void onDeviceDeleted(LocalBluetoothDevice device);
    }
    
}
