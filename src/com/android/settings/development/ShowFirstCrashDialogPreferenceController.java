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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ShowFirstCrashDialogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String SHOW_FIRST_CRASH_DIALOG_KEY = "show_first_crash_dialog";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    public ShowFirstCrashDialogPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return SHOW_FIRST_CRASH_DIALOG_KEY;
    }

    @Override
    public boolean isAvailable() {
        // If the global setting is on, hide this preference since the global overrides
        // any user preference.
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SHOW_FIRST_CRASH_DIALOG, SETTING_VALUE_OFF) == SETTING_VALUE_OFF;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
