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

import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class AudioSharingBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "AudioSharingBluetoothDeviceUpdater";

    private static final String PREF_KEY = "audio_sharing_bt";

    private LocalBluetoothManager mLocalBluetoothManager;

    public AudioSharingBluetoothDeviceUpdater(
            Context context,
            DevicePreferenceCallback devicePreferenceCallback,
            int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
        mLocalBluetoothManager = Utils.getLocalBluetoothManager(context);
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice) && isDeviceInCachedDevicesList(cachedDevice)) {
            // If device is LE audio device and has a broadcast source,
            // it would show in audio sharing devices group.
            if (cachedDevice.isConnectedLeAudioDevice()
                    && AudioSharingUtils.hasBroadcastSource(cachedDevice, mLocalBluetoothManager)) {
                isFilterMatched = true;
            }
        }
        Log.d(
                TAG,
                "isFilterMatched() device : "
                        + cachedDevice.getName()
                        + ", isFilterMatched : "
                        + isFilterMatched);
        return isFilterMatched;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mMetricsCategory);
        final CachedBluetoothDevice device =
                ((BluetoothDevicePreference) preference).getBluetoothDevice();
        return device.setActive();
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        super.update(cachedBluetoothDevice);
        Log.d(TAG, "Map : " + mPreferenceMap);
    }
}
