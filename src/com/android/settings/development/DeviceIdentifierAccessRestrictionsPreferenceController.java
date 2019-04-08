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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DeviceIdentifierAccessRestrictionsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_KEY =
            "device_identifier_access_restrictions";

    // The settings that should be set when the new device identifier access restrictions are
    // disabled.
    private static final String[] RELAX_DEVICE_IDENTIFIER_CHECK_SETTINGS = {
            Settings.Global.PRIVILEGED_DEVICE_IDENTIFIER_3P_CHECK_RELAXED,
            Settings.Global.PRIVILEGED_DEVICE_IDENTIFIER_NON_PRIV_CHECK_RELAXED,
            Settings.Global.PRIVILEGED_DEVICE_IDENTIFIER_PRIV_CHECK_RELAXED
    };

    private ContentResolver mContentResolver;

    public DeviceIdentifierAccessRestrictionsPreferenceController(Context context) {
        super(context);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public boolean isAvailable() {
        // If the new access restrictions have been disabled from the server side then do not
        // display the option.
        boolean disabledFromServerSide = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                        Utils.PROPERTY_DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_DISABLED, false);
        return !disabledFromServerSide;
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
        for (String relaxCheckSetting : RELAX_DEVICE_IDENTIFIER_CHECK_SETTINGS) {
            Settings.Global.putInt(mContentResolver, relaxCheckSetting, isEnabled ? 1 : 0);
        }
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = true;
        for (String relaxCheckSetting : RELAX_DEVICE_IDENTIFIER_CHECK_SETTINGS) {
            if (Settings.Global.getInt(mContentResolver, relaxCheckSetting, 0) == 0) {
                isEnabled = false;
                break;
            }
        }
        ((SwitchPreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeSetting(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
