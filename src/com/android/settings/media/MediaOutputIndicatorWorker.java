/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.media;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.settings.bluetooth.Utils;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.io.IOException;

/**
 * Listener for background change from {@code BluetoothCallback} to update media output indicator.
 */
public class MediaOutputIndicatorWorker extends SliceBackgroundWorker implements BluetoothCallback {

    private static final String TAG = "MediaOutputIndicatorWorker";

    private LocalBluetoothManager mLocalBluetoothManager;

    public MediaOutputIndicatorWorker(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected void onSlicePinned() {
        mLocalBluetoothManager = Utils.getLocalBtManager(getContext());
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalBluetoothManager.getEventManager().registerCallback(this);
    }

    @Override
    protected void onSliceUnpinned() {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void close() throws IOException {
        mLocalBluetoothManager = null;
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        // To handle the case that Bluetooth on and no connected devices
        notifySliceChange();
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (bluetoothProfile == BluetoothProfile.A2DP) {
            notifySliceChange();
        }
    }

    @Override
    public void onAudioModeChanged() {
        notifySliceChange();
    }
}
