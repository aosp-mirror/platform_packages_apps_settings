/*
 * Copyright 2022 The Android Open Source Project
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

import static com.android.settings.development.BluetoothA2dpHwOffloadPreferenceController.A2DP_OFFLOAD_DISABLED_PROPERTY;

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
 * Preference controller to control Bluetooth LE audio offload
 */
public class BluetoothLeAudioHwOffloadPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_disable_le_audio_hw_offload";
    private final DevelopmentSettingsDashboardFragment mFragment;

    static final String LE_AUDIO_OFFLOAD_DISABLED_PROPERTY =
            "persist.bluetooth.leaudio_offload.disabled";
    static final String LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY =
            "ro.bluetooth.leaudio_offload.supported";

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    @VisibleForTesting
    boolean mChanged = false;

    public BluetoothLeAudioHwOffloadPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        BluetoothRebootDialog.show(mFragment);
        mChanged = true;
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        if (mBluetoothAdapter == null) {
            return;
        }

        final boolean leAudioEnabled =
                (mBluetoothAdapter.isLeAudioSupported() == BluetoothStatusCodes.FEATURE_SUPPORTED);
        final boolean leAudioOffloadSupported =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, false);
        final boolean a2dpOffloadDisabled =
                SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, false);
        if (leAudioEnabled && leAudioOffloadSupported && !a2dpOffloadDisabled) {
            final boolean offloadDisabled =
                    SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, true);
            ((TwoStatePreference) mPreference).setChecked(offloadDisabled);
        } else {
            mPreference.setEnabled(false);
            ((TwoStatePreference) mPreference).setChecked(true);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        if (mBluetoothAdapter == null) {
            return;
        }

        final boolean leAudioEnabled =
                (mBluetoothAdapter.isLeAudioSupported() == BluetoothStatusCodes.FEATURE_SUPPORTED);
        final boolean leAudioOffloadSupported =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, false);
        final boolean a2dpOffloadDisabled =
                SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, false);
        if (leAudioEnabled && leAudioOffloadSupported && !a2dpOffloadDisabled) {
            ((TwoStatePreference) mPreference).setChecked(true);
            SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, "true");
        } else {
            mPreference.setEnabled(false);
        }
    }

    /**
     * Check if the le audio offload setting is default value.
     */
    public boolean isDefaultValue() {
        final boolean offloadSupported =
                !SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, false)
                && SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, false);
        final boolean offloadDisabled =
                    SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        return offloadSupported ? offloadDisabled : true;
    }

    /**
     * Called when the RebootDialog confirm is clicked.
     */
    public void onRebootDialogConfirmed() {
        if (!mChanged) {
            return;
        }

        final boolean leaudioOffloadDisabled =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY,
                false);
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY,
                Boolean.toString(!leaudioOffloadDisabled));
    }

    /**
     * Called when the RebootDialog cancel is clicked.
     */
    public void onRebootDialogCanceled() {
        mChanged = false;
    }
}
