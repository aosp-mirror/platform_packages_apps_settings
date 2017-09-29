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

import android.content.Context;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class BluetoothAbsoluteVolumePreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_KEY =
            "bluetooth_disable_absolute_volume";
    @VisibleForTesting
    static final String BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY =
            "persist.bluetooth.disableabsvol";

    private SwitchPreference mPreference;

    public BluetoothAbsoluteVolumePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY,
                isEnabled ? "true" : "false");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = SystemProperties.getBoolean(
                BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY, false /* default */);
        mPreference.setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        SystemProperties.set(BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY, "false");
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }
}
