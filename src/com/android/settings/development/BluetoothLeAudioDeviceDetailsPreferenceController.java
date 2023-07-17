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
import android.provider.DeviceConfig;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control whether display Bluetooth LE audio toggle in device detail
 * settings page or not.
 */
public class BluetoothLeAudioDeviceDetailsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_show_leaudio_device_details";
    private static final String CONFIG_LE_AUDIO_ENABLED_BY_DEFAULT = "le_audio_enabled_by_default";
    private static final boolean LE_AUDIO_DEVICE_DETAIL_DEFAULT_VALUE = true;
    static int sLeAudioSupportedStateCache = BluetoothStatusCodes.ERROR_UNKNOWN;

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    public BluetoothLeAudioDeviceDetailsPreferenceController(Context context) {
        super(context);
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
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
        return (sLeAudioSupportedStateCache == BluetoothStatusCodes.FEATURE_SUPPORTED);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_LE_AUDIO_DEVICE_DETAIL_ENABLED,
                isEnabled ? "true" : "false", LE_AUDIO_DEVICE_DETAIL_DEFAULT_VALUE);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (!isAvailable()) {
            return;
        }

        final boolean leAudioDeviceDetailEnabled = DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_LE_AUDIO_DEVICE_DETAIL_ENABLED,
                LE_AUDIO_DEVICE_DETAIL_DEFAULT_VALUE);
        final boolean leAudioEnabledByDefault = DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_BLUETOOTH, CONFIG_LE_AUDIO_ENABLED_BY_DEFAULT, false);

        mPreference.setEnabled(!leAudioEnabledByDefault);
        ((SwitchPreference) mPreference).setChecked(leAudioDeviceDetailEnabled
                || leAudioEnabledByDefault);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        // Reset the toggle to null when the developer option is disabled
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_LE_AUDIO_DEVICE_DETAIL_ENABLED, "null",
                LE_AUDIO_DEVICE_DETAIL_DEFAULT_VALUE);
    }
}
