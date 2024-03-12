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
 * Preference controller to control Bluetooth LE audio feature
 */
public class BluetoothLeAudioPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_disable_leaudio";

    private static final String LE_AUDIO_DYNAMIC_SWITCH_PROPERTY =
            "ro.bluetooth.leaudio_switcher.supported";
    @VisibleForTesting
    static final String LE_AUDIO_SWITCHER_DISABLED_PROPERTY =
            "persist.bluetooth.leaudio_switcher.disabled";

    private final DevelopmentSettingsDashboardFragment mFragment;

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    @VisibleForTesting
    boolean mChanged = false;

    public BluetoothLeAudioPreferenceController(Context context,
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

        final boolean leAudioSwitchSupported =
                SystemProperties.getBoolean(LE_AUDIO_DYNAMIC_SWITCH_PROPERTY, false);

        final int isLeAudioSupportedStatus = mBluetoothAdapter.isLeAudioSupported();
        final boolean leAudioEnabled =
                (isLeAudioSupportedStatus == BluetoothStatusCodes.FEATURE_SUPPORTED);

        ((TwoStatePreference) mPreference).setChecked(!leAudioEnabled);

        // Disable option if Bluetooth is disabled or if switch is not supported
        if (isLeAudioSupportedStatus == BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED
                || !leAudioSwitchSupported) {
            mPreference.setEnabled(false);
        }
    }

    /**
     * Called when the RebootDialog confirm is clicked.
     */
    public void onRebootDialogConfirmed() {
        if (!mChanged || mBluetoothAdapter == null) {
            return;
        }

        final boolean leAudioDisabled =
                (mBluetoothAdapter.isLeAudioSupported() != BluetoothStatusCodes.FEATURE_SUPPORTED);
        SystemProperties.set(LE_AUDIO_SWITCHER_DISABLED_PROPERTY,
                Boolean.toString(!leAudioDisabled));
    }

    /**
     * Called when the RebootDialog cancel is clicked.
     */
    public void onRebootDialogCanceled() {
        mChanged = false;
    }
}
