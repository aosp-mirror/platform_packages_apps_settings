/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.settings.development;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control whether display Bluetooth LE audio toggle in device detail
 * settings page or not.
 */
public class BluetoothLeAudioDeviceDetailsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_show_leaudio_device_details";
    private static final String LE_AUDIO_CONNECTION_BY_DEFAULT_PROPERTY =
            "ro.bluetooth.leaudio.le_audio_connection_by_default";
    private static final boolean LE_AUDIO_TOGGLE_VISIBLE_DEFAULT_VALUE = true;
    static int sLeAudioSupportedStateCache = BluetoothStatusCodes.ERROR_UNKNOWN;

    static final String LE_AUDIO_TOGGLE_VISIBLE_PROPERTY =
            "persist.bluetooth.leaudio.toggle_visible";

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting boolean mLeAudioEnabledByDefault;

    public BluetoothLeAudioDeviceDetailsPreferenceController(Context context) {
        super(context);
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
        mLeAudioEnabledByDefault =
                SystemProperties.getBoolean(LE_AUDIO_CONNECTION_BY_DEFAULT_PROPERTY, true);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean isAvailable() {
        if (sLeAudioSupportedStateCache == BluetoothStatusCodes.ERROR_UNKNOWN
                && mBluetoothAdapter != null) {
            int isLeAudioSupported = mBluetoothAdapter.isLeAudioSupported();
            if (isLeAudioSupported != BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED) {
                sLeAudioSupportedStateCache = isLeAudioSupported;
            }
        }

        // Display the option only if LE Audio is supported
        return !mLeAudioEnabledByDefault
                && (sLeAudioSupportedStateCache == BluetoothStatusCodes.FEATURE_SUPPORTED);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(LE_AUDIO_TOGGLE_VISIBLE_PROPERTY, Boolean.toString(isEnabled));
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (!isAvailable()) {
            return;
        }

        final boolean isLeAudioToggleVisible = SystemProperties.getBoolean(
                LE_AUDIO_TOGGLE_VISIBLE_PROPERTY, LE_AUDIO_TOGGLE_VISIBLE_DEFAULT_VALUE);

        ((TwoStatePreference) mPreference).setChecked(isLeAudioToggleVisible);
    }
}
