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

package com.android.settings.development.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothCodecType;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settings.development.Flags;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** List preference controller to set the Bluetooth A2DP codec */
public class BluetoothCodecListPreferenceController
        extends AbstractBluetoothListPreferenceController {

    private static final String KEY = "bluetooth_audio_codec_settings_list";
    private static final String TAG = "BtExtCodecCtr";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @Nullable private final Callback mCallback;

    public BluetoothCodecListPreferenceController(
            @NonNull Context context,
            @Nullable Lifecycle lifecycle,
            @Nullable BluetoothA2dpConfigStore store,
            @Nullable Callback callback) {
        super(context, lifecycle, store);
        mCallback = callback;
    }

    @Override
    public boolean isAvailable() {
        boolean available = Flags.a2dpOffloadCodecExtensibilitySettings();
        if (DEBUG) {
            Log.d(TAG, "isAvailable: " + available);
        }
        return available;
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mListPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(@Nullable Preference preference, @NonNull Object newValue) {
        if (!Flags.a2dpOffloadCodecExtensibilitySettings()) {
            return false;
        }

        if (DEBUG) {
            Log.d(TAG, "onPreferenceChange: newValue=" + (String) newValue);
        }
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            Log.e(TAG, "onPreferenceChange: bluetoothA2dp is null");
            return false;
        }

        writeConfigurationValues((String) newValue);

        if (mBluetoothA2dpConfigStore == null) {
            Log.e(TAG, "onPreferenceChange: Bluetooth A2dp Config Store is null");
            return false;
        }
        BluetoothCodecConfig codecConfig;
        if (Flags.a2dpOffloadCodecExtensibilitySettings()) {
            codecConfig = mBluetoothA2dpConfigStore.createCodecConfigFromCodecType();
        } else {
            codecConfig = mBluetoothA2dpConfigStore.createCodecConfig();
        }

        final BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.e(TAG, "onPreferenceChange: active device is null");
            return false;
        }

        if (DEBUG) {
            Log.d(TAG, "onPreferenceChange: setCodecConfigPreference: " + codecConfig.toString());
        }
        bluetoothA2dp.setCodecConfigPreference(activeDevice, codecConfig);
        if (mCallback != null) {
            mCallback.onBluetoothCodecChanged();
        }

        return true;
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        super.updateState(preference);
        if (!Flags.a2dpOffloadCodecExtensibilitySettings()) {
            return;
        }

        final List<String> codecIds = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        String selectedCodecId = mDefaultValue;
        String selectedLabel = mDefaultEntry;

        if (isHDAudioEnabled()) {
            final BluetoothCodecStatus codecStatus = getBluetoothCodecStatus();
            if (codecStatus == null) {
                Log.e(TAG, "updateState: Bluetooth Codec Status is null");
                return;
            }

            final BluetoothCodecConfig currentCodecConfig = codecStatus.getCodecConfig();
            if (currentCodecConfig == null) {
                Log.e(TAG, "updateState: currentCodecConfig is null");
                return;
            }

            final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
            if (bluetoothA2dp == null) {
                Log.e(TAG, "updateState: bluetoothA2dp is null");
                return;
            }

            final Collection<BluetoothCodecType> codecTypes =
                    bluetoothA2dp.getSupportedCodecTypes();
            for (BluetoothCodecType codecType : codecTypes) {
                labels.add(codecType.getCodecName());
                codecIds.add(String.valueOf(codecType.getCodecId()));
                if (currentCodecConfig != null
                        && currentCodecConfig.getExtendedCodecType().equals(codecType)) {
                    selectedCodecId = codecIds.get(codecIds.size() - 1);
                    selectedLabel = labels.get(labels.size() - 1);
                    if (DEBUG) {
                        Log.d(
                                TAG,
                                "updateState: Current config: "
                                        + selectedLabel
                                        + ", id: "
                                        + selectedCodecId);
                    }
                }
            }

            setupListPreference(labels, codecIds, selectedLabel, selectedCodecId);
        }
    }

    @Override
    public void onHDAudioEnabled(boolean enabled) {
        if (DEBUG) {
            Log.d(TAG, "onHDAudioEnabled: enabled=" + enabled);
        }
        if (mListPreference == null) {
            Log.e(TAG, "onHDAudioEnabled: List preference is null");
            return;
        }
        mListPreference.setEnabled(enabled);
    }

    @Override
    protected void writeConfigurationValues(String entryValue) {
        long codecIdValue = getCodecIdFromEntryValue(entryValue);
        BluetoothCodecType selectedCodecType = null;
        BluetoothCodecConfig selectedCodecConfig = null;

        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            Log.e(TAG, "writeConfigurationValues: bluetoothA2dp is null");
            return;
        }

        final Collection<BluetoothCodecType> codecTypes = bluetoothA2dp.getSupportedCodecTypes();
        for (BluetoothCodecType codecType : codecTypes) {
            if (codecType.getCodecId() == codecIdValue) {
                selectedCodecType = codecType;
            }
        }

        if (selectedCodecType == null) {
            Log.e(
                    TAG,
                    "writeConfigurationValues: No selectable codec ID: "
                            + codecIdValue
                            + " found. Unable to change codec");
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "writeConfigurationValues: Selected codec: " + selectedCodecType.toString());
        }
        final BluetoothCodecStatus codecStatus = getBluetoothCodecStatus();
        if (codecStatus == null) {
            Log.e(TAG, "writeConfigurationValues: Bluetooth Codec Status is null");
            return;
        }

        final List<BluetoothCodecConfig> codecConfigs =
                codecStatus.getCodecsSelectableCapabilities();
        for (BluetoothCodecConfig config : codecConfigs) {
            BluetoothCodecType codecType = config.getExtendedCodecType();
            if (codecType == null) {
                Log.e(TAG, "codec type for config:" + config + " is null");
            }
            if (codecType != null && codecType.equals(selectedCodecType)) {
                selectedCodecConfig = config;
            }
        }

        if (selectedCodecConfig == null) {
            Log.e(
                    TAG,
                    "writeConfigurationValues: No selectable codec config for codec: "
                            + selectedCodecType.toString());
            return;
        }

        if (mBluetoothA2dpConfigStore == null) {
            Log.e(TAG, "writeConfigurationValues: Bluetooth A2dp Config Store is null");
            return;
        }

        mBluetoothA2dpConfigStore.setCodecType(selectedCodecType);
        mBluetoothA2dpConfigStore.setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
        mBluetoothA2dpConfigStore.setSampleRate(
                AbstractBluetoothDialogPreferenceController.getHighestSampleRate(
                        selectedCodecConfig));
        mBluetoothA2dpConfigStore.setBitsPerSample(
                AbstractBluetoothDialogPreferenceController.getHighestBitsPerSample(
                        selectedCodecConfig));
        mBluetoothA2dpConfigStore.setChannelMode(
                AbstractBluetoothDialogPreferenceController.getHighestChannelMode(
                        selectedCodecConfig));
    }

    private long getCodecIdFromEntryValue(String entryValue) {
        long codecType = BluetoothCodecType.CODEC_ID_SBC;
        if (entryValue.isEmpty() || Long.valueOf(entryValue) == DEFAULT_VALUE_INT) {
            return codecType;
        }
        return Long.valueOf(entryValue);
    }
}
