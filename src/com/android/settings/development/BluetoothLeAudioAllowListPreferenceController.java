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
import android.sysprop.BluetoothProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control Bluetooth LE audio feature
 */
public class BluetoothLeAudioAllowListPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_bypass_leaudio_allowlist";

    static final String LE_AUDIO_CONNECTION_BY_DEFAULT_PROPERTY =
            "ro.bluetooth.leaudio.le_audio_connection_by_default";
    @VisibleForTesting
    static final String BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY =
            "persist.bluetooth.leaudio.bypass_allow_list";

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting boolean mLeAudioConnectionByDefault;

    private final DevelopmentSettingsDashboardFragment mFragment;

    public BluetoothLeAudioAllowListPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
        mLeAudioConnectionByDefault =
                SystemProperties.getBoolean(LE_AUDIO_CONNECTION_BY_DEFAULT_PROPERTY, true);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean isAvailable() {
        return BluetoothProperties.isProfileBapUnicastClientEnabled().orElse(false)
                && mLeAudioConnectionByDefault;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isBypassed = (Boolean) newValue;
        SystemProperties.set(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY,
                isBypassed ? "true" : "false");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (mBluetoothAdapter == null) {
            mPreference.setEnabled(false);
            return;
        }

        final boolean isLeAudioSupported =
                (mBluetoothAdapter.isLeAudioSupported() == BluetoothStatusCodes.FEATURE_SUPPORTED);
        if (!isLeAudioSupported) {
            mPreference.setEnabled(false);
            ((TwoStatePreference) mPreference).setChecked(false);
            return;
        }

        mPreference.setEnabled(true);
        final boolean isLeAudioAllowlistBypassed =
                SystemProperties.getBoolean(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY, false);
        ((TwoStatePreference) mPreference).setChecked(isLeAudioAllowlistBypassed);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        final boolean isBypassed =
                SystemProperties.getBoolean(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY, false);
        if (isBypassed) {
            SystemProperties.set(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY, Boolean.toString(false));
            ((TwoStatePreference) mPreference).setChecked(false);
        }
    }
}
