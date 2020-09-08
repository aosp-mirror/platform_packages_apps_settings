/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class BluetoothUpdateWorker extends SliceBackgroundWorker implements BluetoothCallback {

    private static final String TAG = "BluetoothUpdateWorker";

    private static LocalBluetoothManager sLocalBluetoothManager;

    private LoadBtManagerHandler mLoadBtManagerHandler;

    public BluetoothUpdateWorker(Context context, Uri uri) {
        super(context, uri);
        mLoadBtManagerHandler = LoadBtManagerHandler.getInstance(context);
        if (sLocalBluetoothManager == null) {
            mLoadBtManagerHandler.startLoadingBtManager(this);
        }
    }

    /** Initialize {@link LocalBluetoothManager} in the background */
    public static void initLocalBtManager(Context context) {
        if (sLocalBluetoothManager == null) {
            LoadBtManagerHandler.getInstance(context).startLoadingBtManager();
        }
    }

    @Nullable
    static LocalBluetoothManager getLocalBtManager() {
        return sLocalBluetoothManager;
    }

    @Override
    protected void onSlicePinned() {
        final LocalBluetoothManager localBtManager = mLoadBtManagerHandler.getLocalBtManager();
        if (localBtManager == null) {
            return;
        }
        localBtManager.getEventManager().registerCallback(this);
    }

    @Override
    protected void onSliceUnpinned() {
        final LocalBluetoothManager localBtManager = mLoadBtManagerHandler.getLocalBtManager();
        if (localBtManager == null) {
            return;
        }
        localBtManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void close() {
    }

    @Override
    public void onAclConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        notifySliceChange();
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        notifySliceChange();
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        notifySliceChange();
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        notifySliceChange();
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state,
            int bluetoothProfile) {
        notifySliceChange();
    }

    private static class LoadBtManagerHandler extends Handler {

        private static LoadBtManagerHandler sHandler;

        private final Runnable mLoadBtManagerTask;
        private final Context mContext;
        private BluetoothUpdateWorker mWorker;

        private static LoadBtManagerHandler getInstance(Context context) {
            if (sHandler == null) {
                final HandlerThread workerThread = new HandlerThread(TAG,
                        Process.THREAD_PRIORITY_BACKGROUND);
                workerThread.start();
                sHandler = new LoadBtManagerHandler(context, workerThread.getLooper());
            }
            return sHandler;
        }

        private LoadBtManagerHandler(Context context, Looper looper) {
            super(looper);
            mContext = context;
            mLoadBtManagerTask = () -> {
                Log.d(TAG, "LoadBtManagerHandler: start loading...");
                final long startTime = System.currentTimeMillis();
                sLocalBluetoothManager = getLocalBtManager();
                Log.d(TAG, "LoadBtManagerHandler took " + (System.currentTimeMillis() - startTime)
                        + " ms");
            };
        }

        private LocalBluetoothManager getLocalBtManager() {
            if (sLocalBluetoothManager != null) {
                return sLocalBluetoothManager;
            }
            return LocalBluetoothManager.getInstance(mContext,
                    (context, btManager) -> {
                        if (mWorker != null) {
                            // notify change if the worker is ready
                            mWorker.notifySliceChange();
                        }
                    });
        }

        private void startLoadingBtManager() {
            if (!hasCallbacks(mLoadBtManagerTask)) {
                post(mLoadBtManagerTask);
            }
        }

        private void startLoadingBtManager(BluetoothUpdateWorker worker) {
            mWorker = worker;
            startLoadingBtManager();
        }
    }
}