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

package com.android.settings.accessibility;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HapClientProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A helper class to get and check hearing aids and its status.
 */
public class HearingAidHelper {

    private final BluetoothAdapter mBluetoothAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private final CachedBluetoothDeviceManager mCachedDeviceManager;

    public HearingAidHelper(Context context) {
        final LocalBluetoothManager localBluetoothManager =
                com.android.settings.bluetooth.Utils.getLocalBluetoothManager(context);
        mProfileManager = localBluetoothManager.getProfileManager();
        mCachedDeviceManager = localBluetoothManager.getCachedDeviceManager();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Gets the connected hearing aids device whose profiles are
     * {@link BluetoothProfile#HEARING_AID} or {@link BluetoothProfile#HAP_CLIENT}.
     *
     * @return a list of hearing aids {@link BluetoothDevice} objects
     */
    public List<BluetoothDevice> getConnectedHearingAidDeviceList() {
        if (!isHearingAidSupported()) {
            return new ArrayList<>();
        }
        final List<BluetoothDevice> deviceList = new ArrayList<>();
        final HapClientProfile hapClientProfile = mProfileManager.getHapClientProfile();
        if (hapClientProfile != null) {
            deviceList.addAll(hapClientProfile.getConnectedDevices());
        }
        final HearingAidProfile hearingAidProfile = mProfileManager.getHearingAidProfile();
        if (hearingAidProfile != null) {
            deviceList.addAll(hearingAidProfile.getConnectedDevices());
        }
        return deviceList.stream()
                .distinct()
                .filter(d -> !mCachedDeviceManager.isSubDevice(d)).collect(Collectors.toList());
    }

    /**
     * Gets the first connected hearing aids device.
     *
     * @return a {@link CachedBluetoothDevice} that is hearing aids device
     */
    public CachedBluetoothDevice getConnectedHearingAidDevice() {
        final List<BluetoothDevice> deviceList = getConnectedHearingAidDeviceList();
        return deviceList.isEmpty() ? null : mCachedDeviceManager.findDevice(deviceList.get(0));
    }

    /**
     * Checks if {@link BluetoothProfile#HEARING_AID} or {@link BluetoothProfile#HAP_CLIENT}
     * supported.
     */
    public boolean isHearingAidSupported() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return false;
        }
        final List<Integer> supportedList = mBluetoothAdapter.getSupportedProfiles();
        return supportedList.contains(BluetoothProfile.HEARING_AID)
                || supportedList.contains(BluetoothProfile.HAP_CLIENT);
    }

    /**
     * Checks if {@link BluetoothProfile#HEARING_AID} or {@link BluetoothProfile#HAP_CLIENT}
     * profiles all ready.
     */
    public boolean isAllHearingAidRelatedProfilesReady() {
        HearingAidProfile hearingAidProfile = mProfileManager.getHearingAidProfile();
        if (hearingAidProfile != null && !hearingAidProfile.isProfileReady()) {
            return false;
        }
        HapClientProfile hapClientProfile = mProfileManager.getHapClientProfile();
        if (hapClientProfile != null && !hapClientProfile.isProfileReady()) {
            return false;
        }
        return true;
    }
}
