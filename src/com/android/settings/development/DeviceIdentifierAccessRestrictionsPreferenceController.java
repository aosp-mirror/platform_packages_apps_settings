/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.provider.DeviceConfig;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DeviceIdentifierAccessRestrictionsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_KEY =
            "device_identifier_access_restrictions";

    public DeviceIdentifierAccessRestrictionsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSetting((boolean) newValue);
        return true;
    }

    private void writeSetting(boolean isEnabled) {
        DeviceConfig.setProperty(DeviceConfig.Privacy.NAMESPACE,
                DeviceConfig.Privacy.PROPERTY_DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_DISABLED,
                String.valueOf(isEnabled), false);
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = Boolean.parseBoolean(
                DeviceConfig.getProperty(DeviceConfig.Privacy.NAMESPACE,
                        DeviceConfig.Privacy.PROPERTY_DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_DISABLED));
        ((SwitchPreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeSetting(false);
        ((SwitchPreference) mPreference).setChecked(true);
    }
}
