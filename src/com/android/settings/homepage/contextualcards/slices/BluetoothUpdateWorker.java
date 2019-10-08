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
import android.util.Log;

import com.android.settings.bluetooth.Utils;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class BluetoothUpdateWorker extends SliceBackgroundWorker implements BluetoothCallback {

    private static final String TAG = "BluetoothUpdateWorker";

    private final LocalBluetoothManager mLocalBluetoothManager;

    public BluetoothUpdateWorker(Context context, Uri uri) {
        super(context, uri);
        mLocalBluetoothManager = Utils.getLocalBtManager(context);
    }

    @Override
    protected void onSlicePinned() {
        if (mLocalBluetoothManager == null) {
            Log.i(TAG, "onSlicePinned() Bluetooth is unsupported.");
            return;
        }
        mLocalBluetoothManager.getEventManager().registerCallback(this);
    }

    @Override
    protected void onSliceUnpinned() {
        if (mLocalBluetoothManager == null) {
            Log.i(TAG, "onSliceUnpinned() Bluetooth is unsupported.");
            return;
        }
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
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
}