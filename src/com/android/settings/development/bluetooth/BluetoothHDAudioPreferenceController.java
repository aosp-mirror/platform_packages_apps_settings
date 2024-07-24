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

package com.android.settings.development.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Switch preference controller for HD audio(optional codec)
 */
public class BluetoothHDAudioPreferenceController extends AbstractBluetoothPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "bluetooth_hd_audio_settings";
    private static final String TAG = "BtHDAudioCtr";

    private final Callback mCallback;

    public BluetoothHDAudioPreferenceController(Context context, Lifecycle lifecycle,
                                                BluetoothA2dpConfigStore store,
                                                Callback callback) {
        super(context, lifecycle, store);
        mCallback = callback;
    }

    @Override
    public void updateState(Preference preference) {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            mPreference.setEnabled(false);
            return;
        }
        final BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.e(TAG, "Active device is null. To disable HD audio button");
            mPreference.setEnabled(false);
            return;
        }
        final boolean supported = (bluetoothA2dp.isOptionalCodecsSupported(activeDevice)
                == BluetoothA2dp.OPTIONAL_CODECS_SUPPORTED);
        mPreference.setEnabled(supported);
        if (supported) {
            final boolean isEnabled = bluetoothA2dp.isOptionalCodecsEnabled(activeDevice)
                    == BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED;
            ((TwoStatePreference) mPreference).setChecked(isEnabled);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            mPreference.setEnabled(false);
            return true;
        }
        final boolean enabled = (Boolean) newValue;
        final int prefValue = enabled
                ? BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED
                : BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED;
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            mPreference.setEnabled(false);
            return true;
        }
        bluetoothA2dp.setOptionalCodecsEnabled(activeDevice, prefValue);
        if (enabled) {
            bluetoothA2dp.enableOptionalCodecs(activeDevice);
        } else {
            bluetoothA2dp.disableOptionalCodecs(activeDevice);
        }
        mCallback.onBluetoothHDAudioEnabled(enabled);
        return true;
    }
}
