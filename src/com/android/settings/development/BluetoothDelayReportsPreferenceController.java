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
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothDelayReportsPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String BLUETOOTH_ENABLE_AVDTP_DELAY_REPORT_KEY =
            "bluetooth_enable_avdtp_delay_reports";
    @VisibleForTesting
    static final String BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY =
            "persist.bluetooth.enabledelayreports";

    private SwitchPreference mPreference;

    public BluetoothDelayReportsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_ENABLE_AVDTP_DELAY_REPORT_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY,
                isEnabled ? "true" : "false");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = SystemProperties.getBoolean(
                BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY, false /* default */);
        mPreference.setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPreference.setEnabled(false);
        // the default setting for this preference is the disabled state
        mPreference.setChecked(false);
        SystemProperties.set(BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY, "false");
    }

}
