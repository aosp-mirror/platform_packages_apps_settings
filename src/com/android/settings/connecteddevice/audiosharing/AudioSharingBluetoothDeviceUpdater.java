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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class AudioSharingBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "AudioSharingBluetoothDeviceUpdater";

    @VisibleForTesting
    static final String PREF_KEY_PREFIX = "audio_sharing_bt_";

    @Nullable private LocalBluetoothManager mLocalBtManager;

    public AudioSharingBluetoothDeviceUpdater(
            Context context,
            DevicePreferenceCallback devicePreferenceCallback,
            int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
        mLocalBtManager = Utils.getLocalBluetoothManager(context);
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice) && isDeviceInCachedDevicesList(cachedDevice)) {
            // If device is LE audio device and has a broadcast source,
            // it would show in audio sharing devices group.
            if (BluetoothUtils.isAudioSharingEnabled()
                    && cachedDevice.isConnectedLeAudioDevice()
                    && BluetoothUtils.hasConnectedBroadcastSource(cachedDevice, mLocalBtManager)) {
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
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_AUDIO_SHARING_DEVICE_CLICK);
        return true;
    }

    @Override
    protected String getPreferenceKeyPrefix() {
        return PREF_KEY_PREFIX;
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
