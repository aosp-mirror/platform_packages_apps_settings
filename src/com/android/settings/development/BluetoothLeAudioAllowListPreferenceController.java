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
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control Bluetooth LE audio feature
 */
public class BluetoothLeAudioAllowListPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_enable_leaudio_allow_list";

    private static final String LE_AUDIO_ALLOW_LIST_SWITCH_SUPPORT_PROPERTY =
            "ro.bluetooth.leaudio_allow_list.supported";
    @VisibleForTesting
    static final String LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY =
            "persist.bluetooth.leaudio.enable_allow_list";

    private static final String LE_AUDIO_DYNAMIC_SWITCH_PROPERTY =
            "ro.bluetooth.leaudio_switcher.supported";
    @VisibleForTesting
    static final String LE_AUDIO_DYNAMIC_ENABLED_PROPERTY =
            "persist.bluetooth.leaudio_switcher.enabled";

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    private final DevelopmentSettingsDashboardFragment mFragment;

    @VisibleForTesting
    boolean mChanged = false;

    public BluetoothLeAudioAllowListPreferenceController(Context context,
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

        int leAudioSupportedState = mBluetoothAdapter.isLeAudioSupported();
        boolean leAudioEnabled = false;

        if ((leAudioSupportedState == BluetoothStatusCodes.FEATURE_SUPPORTED)
                || (leAudioSupportedState == BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED
                && SystemProperties.getBoolean(LE_AUDIO_DYNAMIC_SWITCH_PROPERTY, false)
                && SystemProperties.getBoolean(LE_AUDIO_DYNAMIC_ENABLED_PROPERTY, false))) {
            leAudioEnabled = true;
        }

        final boolean leAudioAllowListSupport =
                SystemProperties.getBoolean(LE_AUDIO_ALLOW_LIST_SWITCH_SUPPORT_PROPERTY, false);

        if (leAudioEnabled && leAudioAllowListSupport) {
            final boolean leAudioAllowListEnabled =
                    SystemProperties.getBoolean(LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, false);
            ((SwitchPreference) mPreference).setChecked(leAudioAllowListEnabled);
        } else {
            mPreference.setEnabled(false);
            ((SwitchPreference) mPreference).setChecked(false);
        }
    }

    /**
     * Called when the RebootDialog confirm is clicked.
     */
    public void onRebootDialogConfirmed() {
        if (!mChanged) {
            return;
        }

        final boolean leAudioAllowListEnabled =
                SystemProperties.getBoolean(LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, false);
        SystemProperties.set(LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY,
                Boolean.toString(!leAudioAllowListEnabled));
    }

    /**
     * Called when the RebootDialog cancel is clicked.
     */
    public void onRebootDialogCanceled() {
        mChanged = false;
    }
}
