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

package com.android.settings.connecteddevice.audiosharing;

import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.util.Log;

import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioSharingUtils {
    private static final String TAG = "AudioSharingUtils";

    /**
     * Fetch {@link CachedBluetoothDevice}s connected to the broadcast assistant. The devices are
     * grouped by CSIP group id.
     *
     * @param localBtManager The BT manager to provide BT functions.
     * @return A map of connected devices grouped by CSIP group id.
     */
    public static Map<Integer, List<CachedBluetoothDevice>> fetchConnectedDevicesByGroupId(
            LocalBluetoothManager localBtManager) {
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices = new HashMap<>();
        LocalBluetoothLeBroadcastAssistant assistant =
                localBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        if (assistant == null) return groupedDevices;
        // TODO: filter out devices with le audio disabled.
        List<BluetoothDevice> connectedDevices = assistant.getConnectedDevices();
        CachedBluetoothDeviceManager cacheManager = localBtManager.getCachedDeviceManager();
        for (BluetoothDevice device : connectedDevices) {
            CachedBluetoothDevice cachedDevice = cacheManager.findDevice(device);
            if (cachedDevice == null) {
                Log.d(TAG, "Skip device due to not being cached: " + device.getAnonymizedAddress());
                continue;
            }
            int groupId = cachedDevice.getGroupId();
            if (groupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
                Log.d(
                        TAG,
                        "Skip device due to no valid group id: " + device.getAnonymizedAddress());
                continue;
            }
            if (!groupedDevices.containsKey(groupId)) {
                groupedDevices.put(groupId, new ArrayList<>());
            }
            groupedDevices.get(groupId).add(cachedDevice);
        }
        return groupedDevices;
    }

    /**
     * Fetch a list of {@link AudioSharingDeviceItem}s in the audio sharing session.
     *
     * @param groupedConnectedDevices devices connected to broadcast assistant grouped by CSIP group
     *     id.
     * @param localBtManager The BT manager to provide BT functions.
     * @return A list of connected devices in the audio sharing session.
     */
    public static ArrayList<AudioSharingDeviceItem> buildOrderedDeviceItemsInSharingSession(
            Map<Integer, List<CachedBluetoothDevice>> groupedConnectedDevices,
            LocalBluetoothManager localBtManager) {
        ArrayList<AudioSharingDeviceItem> deviceItems = new ArrayList<>();
        LocalBluetoothLeBroadcastAssistant assistant =
                localBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        if (assistant == null) return deviceItems;
        CachedBluetoothDevice activeDevice = null;
        List<CachedBluetoothDevice> inactiveDevices = new ArrayList<>();
        for (List<CachedBluetoothDevice> devices : groupedConnectedDevices.values()) {
            for (CachedBluetoothDevice device : devices) {
                List<BluetoothLeBroadcastReceiveState> sourceList =
                        assistant.getAllSources(device.getDevice());
                if (!sourceList.isEmpty()) {
                    // Use random device in the group within the sharing session to
                    // represent the group.
                    if (BluetoothUtils.isActiveLeAudioDevice(device)) {
                        activeDevice = device;
                    } else {
                        inactiveDevices.add(device);
                    }
                    break;
                }
            }
        }
        if (activeDevice != null) {
            deviceItems.add(buildAudioSharingDeviceItem(activeDevice));
        }
        inactiveDevices.stream()
                .sorted(CachedBluetoothDevice::compareTo)
                .forEach(
                        device -> {
                            deviceItems.add(buildAudioSharingDeviceItem(device));
                        });
        return deviceItems;
    }

    /** Build {@link AudioSharingDeviceItem} from {@link CachedBluetoothDevice}. */
    public static AudioSharingDeviceItem buildAudioSharingDeviceItem(
            CachedBluetoothDevice cachedDevice) {
        return new AudioSharingDeviceItem(
                cachedDevice.getName(),
                cachedDevice.getGroupId(),
                BluetoothUtils.isActiveLeAudioDevice(cachedDevice));
    }
}
