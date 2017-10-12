/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothAudioSampleRatePreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin, BluetoothServiceConnectionListener, LifecycleObserver,
        OnDestroy {

    private static final String BLUETOOTH_SELECT_A2DP_SAMPLE_RATE_KEY =
            "bluetooth_select_a2dp_sample_rate";

    @VisibleForTesting
    static final int STREAMING_LABEL_ID = R.string.bluetooth_select_a2dp_codec_streaming_label;

    private final String[] mListValues;
    private final String[] mListSummaries;
    private final Object mBluetoothA2dpLock;
    private final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private ListPreference mPreference;
    private BluetoothA2dp mBluetoothA2dp;

    public BluetoothAudioSampleRatePreferenceController(Context context, Lifecycle lifecycle,
            Object bluetoothA2dpLock, BluetoothA2dpConfigStore store) {
        super(context);

        mBluetoothA2dpLock = bluetoothA2dpLock;
        mBluetoothA2dpConfigStore = store;
        mListValues = context.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_sample_rate_values);
        mListSummaries = context.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_sample_rate_summaries);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_A2DP_SAMPLE_RATE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (ListPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBluetoothA2dp == null) {
            return false;
        }

        final int sampleRate = mapPreferenceValueToSampleRate(newValue.toString());
        mBluetoothA2dpConfigStore.setSampleRate(sampleRate);

        // get values from shared store
        BluetoothCodecConfig codecConfig = mBluetoothA2dpConfigStore.createCodecConfig();

        synchronized (mBluetoothA2dpLock) {
            if (mBluetoothA2dp != null) {
                setCodecConfigPreference(codecConfig);
            }
        }
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (getCodecConfig() == null || mPreference == null) {
            return;
        }

        BluetoothCodecConfig codecConfig;
        synchronized (mBluetoothA2dpLock) {
            codecConfig = getCodecConfig();
        }
        final int sampleRate = codecConfig.getSampleRate();
        final int index = mapSampleRateToIndex(sampleRate);

        mPreference.setValue(mListValues[index]);
        mPreference.setSummary(
                mContext.getResources().getString(STREAMING_LABEL_ID, mListSummaries[index]));

        // write value to shared store
        mBluetoothA2dpConfigStore.setSampleRate(sampleRate);
    }

    @Override
    public void onBluetoothServiceConnected(BluetoothA2dp bluetoothA2dp) {
        mBluetoothA2dp = bluetoothA2dp;
        updateState(mPreference);
    }

    @Override
    public void onBluetoothCodecUpdated() {
        updateState(mPreference);
    }

    @Override
    public void onBluetoothServiceDisconnected() {
        mBluetoothA2dp = null;
    }

    @Override
    public void onDestroy() {
        mBluetoothA2dp = null;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPreference.setEnabled(false);
    }

    private int mapSampleRateToIndex(int sampleRate) {
        int index = 0;
        switch (sampleRate) {
            case BluetoothCodecConfig.SAMPLE_RATE_44100:
                index = 1;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_48000:
                index = 2;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_88200:
                index = 3;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_96000:
                index = 4;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_176400:
            case BluetoothCodecConfig.SAMPLE_RATE_192000:
            case BluetoothCodecConfig.SAMPLE_RATE_NONE:
            default:
                break;
        }
        return index;
    }

    private int mapPreferenceValueToSampleRate(String value) {
        final int index = mPreference.findIndexOfValue(value);
        int sampleRateValue = 0;
        switch (index) {
            case 0:
                // Reset to default
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_NONE;
                break;
            case 1:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_44100;
                break;
            case 2:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_48000;
                break;
            case 3:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_88200;
                break;
            case 4:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_96000;
                break;
            default:
                break;
        }
        return sampleRateValue;
    }

    @VisibleForTesting
    void setCodecConfigPreference(BluetoothCodecConfig config) {
        mBluetoothA2dp.setCodecConfigPreference(config);
    }

    @VisibleForTesting
    BluetoothCodecConfig getCodecConfig() {
        if (mBluetoothA2dp == null || mBluetoothA2dp.getCodecStatus() == null) {
            return null;
        }

        return mBluetoothA2dp.getCodecStatus().getCodecConfig();
    }
}
