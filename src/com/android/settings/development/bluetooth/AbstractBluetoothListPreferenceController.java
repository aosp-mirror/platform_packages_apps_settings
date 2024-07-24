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

import static android.bluetooth.BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.List;

/** Abstract class for Bluetooth A2DP config list controller in developer option. */
public abstract class AbstractBluetoothListPreferenceController
        extends AbstractBluetoothPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "AbstrBtListPrefCtrl";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected static final int DEFAULT_VALUE_INT = 1000;

    @Nullable protected ListPreference mListPreference;

    protected String mDefaultEntry;
    protected String mDefaultValue;

    @Nullable protected final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;

    public AbstractBluetoothListPreferenceController(
            @NonNull Context context,
            @Nullable Lifecycle lifecycle,
            @Nullable BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);

        mDefaultEntry = mContext.getString(R.string.bluetooth_audio_codec_default_selection);
        mDefaultValue = String.valueOf(DEFAULT_VALUE_INT);

        mBluetoothA2dpConfigStore = store;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mListPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(@Nullable Preference preference, @NonNull Object newValue) {
        if (DEBUG) {
            Log.d(TAG, "onPreferenceChange: newValue=" + (String) newValue);
        }
        if (mListPreference == null) {
            Log.e(TAG, "onPreferenceChange: List preference is null");
            return false;
        }
        updateState(mListPreference);
        return true;
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        setupDefaultListPreference();
    }

    @Override
    public void onBluetoothServiceConnected(@NonNull BluetoothA2dp bluetoothA2dp) {
        super.onBluetoothServiceConnected(bluetoothA2dp);
        initConfigStore();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        if (DEBUG) {
            Log.d(TAG, "onDeveloperOptionsSwitchDisabled");
        }
        if (mListPreference == null) {
            Log.e(TAG, "onDeveloperOptionsSwitchDisabled: List preference is null");
            return;
        }
        updateState(mListPreference);
    }

    /**
     * Method to notify controller when the HD audio(optional codec) state is changed.
     *
     * @param enabled Is {@code true} when the setting is enabled.
     */
    public void onHDAudioEnabled(boolean enabled) {}

    /**
     * Updates the new value to the {@link BluetoothA2dpConfigStore}.
     *
     * @param entryValue the new setting entry value
     */
    protected abstract void writeConfigurationValues(String entryValue);

    /**
     * Gets the current bluetooth codec status.
     *
     * @return {@link BluetoothCodecStatus}.
     */
    @Nullable
    protected BluetoothCodecStatus getBluetoothCodecStatus() {
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

    /**
     * Gets the current bluetooth codec config.
     *
     * @return {@link BluetoothCodecConfig}.
     */
    @Nullable
    protected BluetoothCodecConfig getCurrentCodecConfig() {
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

    /**
     * Sets the {@link ListPreference}. This method adds the default entry and the entry value
     * automatically.
     *
     * @param entries list of String entries for the {@link ListPreference}.
     * @param entryValues list of String entry values for the {@link ListPreference}.
     * @param selectedEntry currently selected entry.
     * @param selectedValue currently selected entry value.
     */
    protected void setupListPreference(
            List<String> entries,
            List<String> entryValues,
            String selectedEntry,
            String selectedValue) {
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
        entries.add(0, mDefaultEntry);
        entryValues.add(0, mDefaultValue);

        if (mListPreference == null) {
            Log.e(TAG, "setupListPreference: List preference is null");
            return;
        }
        mListPreference.setEntries(entries.toArray(new String[entries.size()]));
        mListPreference.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
        mListPreference.setValue(selectedValue);
        mListPreference.setSummary(selectedEntry);
    }

    /**
     * Check HD Audio enabled.
     *
     * @return true if HD Audio is enabled.
     */
    protected boolean isHDAudioEnabled() {
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

    private void setupDefaultListPreference() {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "setupDefaultListPreference: mDefaultEntry="
                            + mDefaultEntry
                            + ", mDefaultValue="
                            + mDefaultValue);
        }
        if (mListPreference == null) {
            Log.e(TAG, "setupListPreference: List preference is null");
            return;
        }
        mListPreference.setEntries(new String[] {mDefaultEntry});
        mListPreference.setEntryValues(new String[] {mDefaultValue});
        mListPreference.setValue(mDefaultValue);
        mListPreference.setSummary(mDefaultEntry);
    }

    private void initConfigStore() {
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
        mBluetoothA2dpConfigStore.setCodecPriority(CODEC_PRIORITY_HIGHEST);
        mBluetoothA2dpConfigStore.setCodecSpecific1Value(config.getCodecSpecific1());
    }
}
