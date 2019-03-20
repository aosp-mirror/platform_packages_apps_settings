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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for background change from {@code BluetoothCallback} to update media output indicator.
 */
public class MediaOutputIndicatorWorker extends SliceBackgroundWorker implements BluetoothCallback {

    private static final String TAG = "MediaOutputIndicatorWorker";

    private LocalBluetoothManager mLocalBluetoothManager;
    private LocalBluetoothProfileManager mProfileManager;

    public MediaOutputIndicatorWorker(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected void onSlicePinned() {
        LocalBluetoothManager mLocalBluetoothManager = Utils.getLocalBtManager(getContext());
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mProfileManager = mLocalBluetoothManager.getProfileManager();
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
        mProfileManager = null;
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

    /**
     * To decide Slice's visibility.
     *
     * @return true if device is connected or previously connected, false for other cases.
     */
    public boolean isVisible() {
        return !CollectionUtils.isEmpty(getConnectableA2dpDevices())
                || !CollectionUtils.isEmpty(getConnectableHearingAidDevices());
    }

    private List<BluetoothDevice> getConnectableA2dpDevices() {
        // get A2dp devices on all states
        // (STATE_DISCONNECTED, STATE_CONNECTING, STATE_CONNECTED,  STATE_DISCONNECTING)
        final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        if (a2dpProfile == null) {
            return new ArrayList<>();
        }
        return a2dpProfile.getConnectableDevices();
    }

    private List<BluetoothDevice> getConnectableHearingAidDevices() {
        // get hearing aid profile devices on all states
        // (STATE_DISCONNECTED, STATE_CONNECTING, STATE_CONNECTED,  STATE_DISCONNECTING)
        final HearingAidProfile hapProfile = mProfileManager.getHearingAidProfile();
        if (hapProfile == null) {
            return new ArrayList<>();
        }

        return hapProfile.getConnectableDevices();
    }

    /**
     * Get active devices name.
     *
     * @return active Bluetooth device alias, or default summary if no active device.
     */
    public CharSequence findActiveDeviceName() {
        // Return Hearing Aid device name if it is active
        BluetoothDevice activeDevice = findActiveHearingAidDevice();
        if (activeDevice != null) {
            return activeDevice.getAliasName();
        }
        // Return A2DP device name if it is active
        final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        if (a2dpProfile != null) {
            activeDevice = a2dpProfile.getActiveDevice();
            if (activeDevice != null) {
                return activeDevice.getAliasName();
            }
        }
        // No active device, return default summary
        return getContext().getText(R.string.media_output_default_summary);
    }

    private BluetoothDevice findActiveHearingAidDevice() {
        final HearingAidProfile hearingAidProfile = mProfileManager.getHearingAidProfile();
        if (hearingAidProfile == null) {
            return null;
        }

        final List<BluetoothDevice> activeDevices = hearingAidProfile.getActiveDevices();
        for (BluetoothDevice btDevice : activeDevices) {
            if (btDevice != null) {
                return btDevice;
            }
        }
        return null;
    }
}
