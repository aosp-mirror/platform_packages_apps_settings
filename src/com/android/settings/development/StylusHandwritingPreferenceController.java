/*
 * Copyright (C) 2022 The Android Open Source Project
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

class StylusHandwritingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    static final int SETTING_VALUE_ON = 1;
    static final int SETTING_VALUE_OFF = 0;

    private static final String STYLUS_HANDWRITING_OPTIONS_KEY = "stylus_handwriting";

    StylusHandwritingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return STYLUS_HANDWRITING_OPTIONS_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);

        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int enable = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                Settings.Secure.STYLUS_HANDWRITING_DEFAULT_VALUE);
        ((TwoStatePreference) mPreference).setChecked(enable != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                Settings.Secure.STYLUS_HANDWRITING_DEFAULT_VALUE);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
