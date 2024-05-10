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
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class CoolColorTemperaturePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String COLOR_TEMPERATURE_KEY = "color_temperature";

    @VisibleForTesting
    static final String COLOR_TEMPERATURE_PROPERTY = "persist.sys.debug.color_temp";

    public CoolColorTemperaturePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_enableColorTemperature);
    }

    @Override
    public String getPreferenceKey() {
        return COLOR_TEMPERATURE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isColorTemperatureEnabled = (Boolean) newValue;
        SystemProperties.set(COLOR_TEMPERATURE_PROPERTY,
                Boolean.toString(isColorTemperatureEnabled));
        SystemPropPoker.getInstance().poke();
        displayColorTemperatureToast();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enableColorTemperature = SystemProperties.getBoolean(
                COLOR_TEMPERATURE_PROPERTY, false /* default */);
        ((TwoStatePreference) mPreference).setChecked(enableColorTemperature);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(COLOR_TEMPERATURE_PROPERTY, Boolean.toString(false));
        ((TwoStatePreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    void displayColorTemperatureToast() {
        Toast.makeText(mContext, R.string.color_temperature_toast, Toast.LENGTH_LONG).show();
    }
}
