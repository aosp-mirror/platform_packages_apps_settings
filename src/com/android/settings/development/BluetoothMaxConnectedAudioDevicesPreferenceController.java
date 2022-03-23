/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothMaxConnectedAudioDevicesPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String MAX_CONNECTED_AUDIO_DEVICES_PREFERENCE_KEY =
            "bluetooth_max_connected_audio_devices";

    @VisibleForTesting
    static final String MAX_CONNECTED_AUDIO_DEVICES_PROPERTY =
            "persist.bluetooth.maxconnectedaudiodevices";

    private final int mDefaultMaxConnectedAudioDevices;

    public BluetoothMaxConnectedAudioDevicesPreferenceController(Context context) {
        super(context);
        mDefaultMaxConnectedAudioDevices = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_bluetooth_max_connected_audio_devices);
    }

    @Override
    public String getPreferenceKey() {
        return MAX_CONNECTED_AUDIO_DEVICES_PREFERENCE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final ListPreference listPreference = (ListPreference) mPreference;
        final CharSequence[] entries = listPreference.getEntries();
        entries[0] = String.format(entries[0].toString(), mDefaultMaxConnectedAudioDevices);
        listPreference.setEntries(entries);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newValueString = newValue.toString();
        final ListPreference listPreference = (ListPreference) preference;
        if (listPreference.findIndexOfValue(newValueString) <= 0) {
            // Reset property value when default is chosen or when value is illegal
            newValueString = "";
        }
        SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, newValueString);
        updateState(preference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final CharSequence[] entries = listPreference.getEntries();
        final String currentValue = SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY);
        int index = 0;
        if (!currentValue.isEmpty()) {
            index = listPreference.findIndexOfValue(currentValue);
            if (index < 0) {
                // Reset property value when value is illegal
                SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, "");
                index = 0;
            }
        }
        listPreference.setValueIndex(index);
        listPreference.setSummary(entries[index]);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        updateState(mPreference);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, "");
        updateState(mPreference);
    }
}

