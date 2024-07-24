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
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Base controller for Switch preference that maps to a specific value in Settings.Global.
 */
public abstract class GlobalSettingSwitchPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;

    private final String mSettingsKey;
    private final int mOn;
    private final int mOff;
    private final int mDefault;

    public GlobalSettingSwitchPreferenceController(Context context, String globalSettingsKey) {
        this(context, globalSettingsKey, SETTING_VALUE_ON, SETTING_VALUE_OFF, SETTING_VALUE_OFF);
    }

    /**
     * Use different on/off/default vaules other than the standard 1/0/0.
     */
    public GlobalSettingSwitchPreferenceController(Context context, String globalSettingsKey,
            int valueOn, int valueOff, int valueDefault) {
        super(context);
        mSettingsKey = globalSettingsKey;
        mOn = valueOn;
        mOff = valueOff;
        mDefault = valueDefault;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(), mSettingsKey, isEnabled ? mOn : mOff);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode =
            Settings.Global.getInt(mContext.getContentResolver(), mSettingsKey, mDefault);
        ((TwoStatePreference) mPreference).setChecked(mode != mOff);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(), mSettingsKey, mOff);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
