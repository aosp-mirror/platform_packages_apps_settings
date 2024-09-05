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
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settings.development.Flags;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** List preference controller to set the Bluetooth A2DP codec */
public class BluetoothCodecListPreferenceController extends AbstractBluetoothPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "bluetooth_audio_codec_settings_list";
    private static final String TAG = "BtExtCodecCtr";

    @Nullable private final Callback mCallback;
    @Nullable protected final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    @Nullable protected ListPreference mListPreference;

    public BluetoothCodecListPreferenceController(
            @NonNull Context context,
            @Nullable Lifecycle lifecycle,
            @Nullable BluetoothA2dpConfigStore store,
            @Nullable Callback callback) {
        super(context, lifecycle, store);
        mCallback = callback;
        mBluetoothA2dpConfigStore = store;
    }

    @Override
    public boolean isAvailable() {
        boolean available = Flags.a2dpOffloadCodecExtensibilitySettings();
        Log.d(TAG, "isAvailable: " + available);
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

        if (mListPreference == null) {
            Log.e(TAG, "onPreferenceChange: List preference is null");
            return false;
        }

        Log.d(TAG, "onPreferenceChange: newValue=" + (String) newValue);
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            Log.e(TAG, "onPreferenceChange: bluetoothA2dp is null");
            setListPreferenceEnabled(false);
            return false;
        }

        if (!writeConfigurationValues((String) newValue)) {
            Log.e(TAG, "onPreferenceChange: Configuration failed");
            return false;
        }

        if (mBluetoothA2dpConfigStore == null) {
            Log.e(TAG, "onPreferenceChange: Bluetooth A2dp Config Store is null");
            return false;
        }

        final BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.e(TAG, "onPreferenceChange: active device is null");
            setListPreferenceEnabled(false);
            return false;
        }

        BluetoothCodecConfig codecConfig =
                mBluetoothA2dpConfigStore.createCodecConfigFromCodecType();
        Log.d(TAG, "onPreferenceChange: setCodecConfigPreference: " + codecConfig.toString());
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

        if (!isHDAudioEnabled()) {
            Log.d(TAG, "updateState: HD Audio is disabled");
            setListPreferenceEnabled(false);
            return;
        }

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

        final List<String> codecIds = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        String selectedCodecId = null;
        String selectedLabel = null;
        final List<BluetoothCodecConfig> codecConfigs =
                codecStatus.getCodecsSelectableCapabilities();
        for (BluetoothCodecConfig config : codecConfigs) {
            BluetoothCodecType codecType = config.getExtendedCodecType();
            if (codecType == null) {
                Log.e(TAG, "codec type for config:" + config + " is null");
                continue;
            }
            labels.add(codecType.getCodecName());
            codecIds.add(String.valueOf(codecType.getCodecId()));
            if (currentCodecConfig != null
                    && currentCodecConfig.getExtendedCodecType().equals(codecType)) {
                selectedCodecId = codecIds.get(codecIds.size() - 1);
                selectedLabel = labels.get(labels.size() - 1);
                Log.d(
                        TAG,
                        "updateState: Selecting codec: "
                                + selectedLabel
                                + ", id: "
                                + selectedCodecId);
            }
        }
        setupListPreference(labels, codecIds, selectedLabel, selectedCodecId);
    }

    @Override
    public void onBluetoothServiceConnected(@NonNull BluetoothA2dp bluetoothA2dp) {
        super.onBluetoothServiceConnected(bluetoothA2dp);
        initConfigStore();
    }

    public void onHDAudioEnabled(boolean enabled) {
        Log.d(TAG, "onHDAudioEnabled: enabled=" + enabled);
        if (mListPreference == null) {
            Log.e(TAG, "onHDAudioEnabled: List preference is null");
            return;
        }
        setListPreferenceEnabled(enabled);
        if (!enabled) {
            mListPreference.setValue(null);
            mListPreference.setSummary(null);
        }
    }

    @VisibleForTesting
    boolean writeConfigurationValues(String entryValue) {
        long codecIdValue = getCodecIdFromEntryValue(entryValue);
        BluetoothCodecType selectedCodecType = null;
        BluetoothCodecConfig selectedCodecConfig = null;

        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            Log.e(TAG, "writeConfigurationValues: bluetoothA2dp is null");
            return false;
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
            return false;
        }

        Log.d(TAG, "writeConfigurationValues: Selected codec: " + selectedCodecType.toString());
        final BluetoothCodecStatus codecStatus = getBluetoothCodecStatus();
        if (codecStatus == null) {
            Log.e(TAG, "writeConfigurationValues: Bluetooth Codec Status is null");
            return false;
        }

        final List<BluetoothCodecConfig> codecConfigs =
                codecStatus.getCodecsSelectableCapabilities();
        for (BluetoothCodecConfig config : codecConfigs) {
            BluetoothCodecType codecType = config.getExtendedCodecType();
            if (codecType == null) {
                Log.e(TAG, "codec type for config:" + config + " is null");
                continue;
            }
            if (codecType.equals(selectedCodecType)) {
                selectedCodecConfig = config;
            }
        }

        if (selectedCodecConfig == null) {
            Log.e(
                    TAG,
                    "writeConfigurationValues: No selectable codec config for codec: "
                            + selectedCodecType.toString());
            return false;
        }

        if (mBluetoothA2dpConfigStore == null) {
            Log.e(TAG, "writeConfigurationValues: Bluetooth A2dp Config Store is null");
            return false;
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
        return true;
    }

    private long getCodecIdFromEntryValue(String entryValue) {
        long codecType = BluetoothCodecType.CODEC_ID_SBC;
        if (entryValue.isEmpty()) {
            return codecType;
        }
        return Long.valueOf(entryValue);
    }

    private void setListPreferenceEnabled(boolean enable) {
        if (mListPreference != null) {
            mListPreference.setEnabled(enable);
        }
    }

    @Nullable
    @VisibleForTesting
    BluetoothCodecStatus getBluetoothCodecStatus() {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            Log.e(
                    TAG,
                    "getBluetoothCodecStatus: Unable to get codec status. Bluetooth A2dp is null.");
            return null;
        }
        final BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.e(TAG, "getBluetoothCodecStatus: Unable to get codec status. No active device.");
            return null;
        }
        final BluetoothCodecStatus codecStatus = bluetoothA2dp.getCodecStatus(activeDevice);
        if (codecStatus == null) {
            Log.e(TAG, "getBluetoothCodecStatus: Codec status is null");
            return null;
        }
        return codecStatus;
    }

    @Nullable
    @VisibleForTesting
    BluetoothCodecConfig getCurrentCodecConfig() {
        final BluetoothCodecStatus codecStatus = getBluetoothCodecStatus();
        if (codecStatus == null) {
            Log.e(
                    TAG,
                    "getCurrentCodecConfig: Unable to get current codec config. Codec status is"
                            + " null");
            return null;
        }

        return codecStatus.getCodecConfig();
    }

    @VisibleForTesting
    boolean isHDAudioEnabled() {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            Log.e(TAG, "isHDAudioEnabled: Unable to get codec status. BluetoothA2dp is null.");
            return false;
        }
        BluetoothDevice activeDevice = getA2dpActiveDevice();
        if (activeDevice == null) {
            Log.e(TAG, "isHDAudioEnabled: Unable to get codec status. No active device.");
            return false;
        }
        return (bluetoothA2dp.isOptionalCodecsEnabled(activeDevice)
                == BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
    }

    @VisibleForTesting
    void initConfigStore() {
        final BluetoothCodecConfig config = getCurrentCodecConfig();
        if (config == null) {
            Log.e(TAG, "initConfigStore: Current codec config is null.");
            return;
        }
        if (mBluetoothA2dpConfigStore == null) {
            Log.e(TAG, "initConfigStore: Bluetooth A2dp Config Store is null.");
            return;
        }
        mBluetoothA2dpConfigStore.setCodecType(config.getExtendedCodecType());
        mBluetoothA2dpConfigStore.setSampleRate(config.getSampleRate());
        mBluetoothA2dpConfigStore.setBitsPerSample(config.getBitsPerSample());
        mBluetoothA2dpConfigStore.setChannelMode(config.getChannelMode());
        mBluetoothA2dpConfigStore.setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
        mBluetoothA2dpConfigStore.setCodecSpecific1Value(config.getCodecSpecific1());
    }

    @VisibleForTesting
    void setupDefaultListPreference() {
        Log.d(TAG, "setupDefaultListPreference");
        if (mListPreference == null) {
            Log.e(TAG, "setupDefaultListPreference: List preference is null");
            return;
        }
        mListPreference.setValue(null);
        mListPreference.setSummary(null);
        setListPreferenceEnabled(false);
    }

    /**
     * Sets the {@link ListPreference}.
     *
     * @param entries list of String entries for the {@link ListPreference}.
     * @param entryValues list of String entry values for the {@link ListPreference}.
     * @param selectedEntry currently selected entry.
     * @param selectedValue currently selected entry value.
     */
    @VisibleForTesting
    void setupListPreference(
            List<String> entries,
            List<String> entryValues,
            @Nullable String selectedEntry,
            @Nullable String selectedValue) {
        if (mListPreference == null) {
            Log.e(TAG, "setupListPreference: List preference is null");
            return;
        }

        if (entries.size() != entryValues.size()) {
            Log.e(
                    TAG,
                    ("setupListPreference: size of entries: " + entries.size())
                            + (", size of entryValues" + entryValues.size()));
            setupDefaultListPreference();
            return;
        }
        if (entries.isEmpty() || entryValues.isEmpty()) {
            Log.e(TAG, "setupListPreference: entries or entryValues empty");
            setupDefaultListPreference();
            return;
        }

        mListPreference.setEntries(entries.toArray(new String[entries.size()]));
        mListPreference.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
        mListPreference.setValue(selectedValue);
        mListPreference.setSummary(selectedEntry);
        setListPreferenceEnabled(true);
    }
}
