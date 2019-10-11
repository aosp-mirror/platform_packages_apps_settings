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
import android.provider.Settings;
import android.sysprop.DisplayProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.internal.app.LocalePicker;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class RtlLayoutPreferenceController extends DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String FORCE_RTL_LAYOUT_KEY = "force_rtl_layout_all_locales";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    public RtlLayoutPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return FORCE_RTL_LAYOUT_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        writeToForceRtlLayoutSetting(isEnabled);
        updateLocales();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int rtlLayoutMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL, SETTING_VALUE_OFF);
        ((SwitchPreference) mPreference).setChecked(rtlLayoutMode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeToForceRtlLayoutSetting(false);
        updateLocales();
        ((SwitchPreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    void updateLocales() {
        LocalePicker.updateLocales(mContext.getResources().getConfiguration().getLocales());
    }

    private void writeToForceRtlLayoutSetting(boolean isEnabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        DisplayProperties.debug_force_rtl(isEnabled);
    }
}
