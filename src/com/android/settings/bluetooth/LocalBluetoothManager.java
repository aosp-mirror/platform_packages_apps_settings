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

import android.content.Context;
import android.util.Log;

/**
 * LocalBluetoothManager provides a simplified interface on top of a subset of
 * the Bluetooth API. Note that {@link #getInstance} will return null
 * if there is no Bluetooth adapter on this device, and callers must be
 * prepared to handle this case.
 */
public final class LocalBluetoothManager {
    private static final String TAG = "LocalBluetoothManager";

    /** Singleton instance. */
    private static LocalBluetoothManager sInstance;

    private final Context mContext;

    /** If a BT-related activity is in the foreground, this will be it. */
    private Context mForegroundActivity;

    private BluetoothDiscoverableEnabler mDiscoverableEnabler;

    private final LocalBluetoothAdapter mLocalAdapter;

    private final CachedBluetoothDeviceManager mCachedDeviceManager;

    /** The Bluetooth profile manager. */
    private final LocalBluetoothProfileManager mProfileManager;

    /** The broadcast receiver event manager. */
    private final BluetoothEventManager mEventManager;

    public static synchronized LocalBluetoothManager getInstance(Context context) {
        if (sInstance == null) {
            LocalBluetoothAdapter adapter = LocalBluetoothAdapter.getInstance();
            if (adapter == null) {
                return null;
            }
            // This will be around as long as this process is
            Context appContext = context.getApplicationContext();
            sInstance = new LocalBluetoothManager(adapter, appContext);
        }

        return sInstance;
    }

    public void setDiscoverableEnabler(BluetoothDiscoverableEnabler discoverableEnabler) {
        mDiscoverableEnabler = discoverableEnabler;
    }

    public BluetoothDiscoverableEnabler getDiscoverableEnabler() {
        return mDiscoverableEnabler;
    }

    private LocalBluetoothManager(LocalBluetoothAdapter adapter, Context context) {
        mContext = context;
        mLocalAdapter = adapter;

        mCachedDeviceManager = new CachedBluetoothDeviceManager(context);
        mEventManager = new BluetoothEventManager(mLocalAdapter,
                mCachedDeviceManager, context);
        mProfileManager = new LocalBluetoothProfileManager(context,
                mLocalAdapter, mCachedDeviceManager, mEventManager);
    }

    public LocalBluetoothAdapter getBluetoothAdapter() {
        return mLocalAdapter;
    }

    public Context getContext() {
        return mContext;
    }

    public Context getForegroundActivity() {
        return mForegroundActivity;
    }

    boolean isForegroundActivity() {
        return mForegroundActivity != null;
    }

    synchronized void setForegroundActivity(Context context) {
        if (context != null) {
            Log.d(TAG, "setting foreground activity to non-null context");
            mForegroundActivity = context;
        } else {
            if (mForegroundActivity != null) {
                Log.d(TAG, "setting foreground activity to null");
                mForegroundActivity = null;
            }
        }
    }

    CachedBluetoothDeviceManager getCachedDeviceManager() {
        return mCachedDeviceManager;
    }

    BluetoothEventManager getEventManager() {
        return mEventManager;
    }

    LocalBluetoothProfileManager getProfileManager() {
        return mProfileManager;
    }
}
