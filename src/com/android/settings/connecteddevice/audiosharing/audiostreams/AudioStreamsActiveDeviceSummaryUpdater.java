/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.annotation.Nullable;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.Optional;

public class AudioStreamsActiveDeviceSummaryUpdater extends SummaryUpdater
        implements BluetoothCallback {
    private static final String TAG = "AudioStreamsListenWithSummaryUpdater";
    private static final boolean DEBUG = BluetoothUtils.D;
    private final LocalBluetoothManager mBluetoothManager;

    public AudioStreamsActiveDeviceSummaryUpdater(
            Context context, OnSummaryChangeListener listener) {
        super(context, listener);
        mBluetoothManager = Utils.getLocalBluetoothManager(context);
    }

    @Override
    public void onActiveDeviceChanged(
            @Nullable CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onActiveDeviceChanged() with activeDevice : "
                            + (activeDevice == null ? "null" : activeDevice.getAddress())
                            + " on profile : "
                            + bluetoothProfile);
        }
        if (bluetoothProfile == BluetoothProfile.LE_AUDIO) {
            notifyChangeIfNeeded();
        }
    }

    @Override
    public void register(boolean register) {
        if (register) {
            notifyChangeIfNeeded();
            mBluetoothManager.getEventManager().registerCallback(this);
        } else {
            mBluetoothManager.getEventManager().unregisterCallback(this);
        }
    }

    @Override
    protected String getSummary() {
        var activeSink = getActiveSinkOnAssistant(mBluetoothManager);
        if (activeSink.isEmpty()) {
            return "No active LE Audio device";
        }
        return activeSink.get().getName();
    }

    private static Optional<CachedBluetoothDevice> getActiveSinkOnAssistant(
            LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(TAG, "getActiveSinksOnAssistant(): LocalBluetoothManager is null!");
            return Optional.empty();
        }
        var groupedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(manager);
        var leadDevices =
                AudioSharingUtils.buildOrderedConnectedLeadDevices(manager, groupedDevices, false);

        if (!leadDevices.isEmpty() && AudioSharingUtils.isActiveLeAudioDevice(leadDevices.get(0))) {
            return Optional.of(leadDevices.get(0));
        } else {
            Log.w(TAG, "getActiveSinksOnAssistant(): No active lead device!");
        }
        return Optional.empty();
    }
}
