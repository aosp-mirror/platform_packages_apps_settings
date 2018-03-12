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

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothDelayReportsPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String BLUETOOTH_DISABLE_AVDTP_DELAY_REPORT_KEY =
            "bluetooth_disable_avdtp_delay_reports";
    @VisibleForTesting
    static final String BLUETOOTH_DISABLE_AVDTP_DELAY_REPORTS_PROPERTY =
            "persist.bluetooth.disabledelayreports";

    public BluetoothDelayReportsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_DISABLE_AVDTP_DELAY_REPORT_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isDisabled = (Boolean) newValue;
        SystemProperties.set(BLUETOOTH_DISABLE_AVDTP_DELAY_REPORTS_PROPERTY,
                isDisabled ? "true" : "false");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isDisabled = SystemProperties.getBoolean(
                BLUETOOTH_DISABLE_AVDTP_DELAY_REPORTS_PROPERTY, false /* default */);
        ((SwitchPreference) mPreference).setChecked(isDisabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        // the default setting for this preference is the disabled state
        ((SwitchPreference) mPreference).setChecked(false);
        SystemProperties.set(BLUETOOTH_DISABLE_AVDTP_DELAY_REPORTS_PROPERTY, "false");
    }

}
