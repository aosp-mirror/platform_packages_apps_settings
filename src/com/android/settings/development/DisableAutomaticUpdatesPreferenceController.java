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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DisableAutomaticUpdatesPreferenceController extends
        DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String OTA_DISABLE_AUTOMATIC_UPDATE_KEY = "ota_disable_automatic_update";

    // We use the "disabled status" in code, but show the opposite text
    // "Automatic system updates" on screen. So a value 0 indicates the
    // automatic update is enabled.
    @VisibleForTesting
    final static int DISABLE_UPDATES_SETTING = 1;
    @VisibleForTesting
    final static int ENABLE_UPDATES_SETTING = 0;

    public DisableAutomaticUpdatesPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return OTA_DISABLE_AUTOMATIC_UPDATE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean updatesEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                updatesEnabled ? ENABLE_UPDATES_SETTING : DISABLE_UPDATES_SETTING);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int updatesEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE, 0 /* default */);

        ((TwoStatePreference) mPreference).setChecked(updatesEnabled != DISABLE_UPDATES_SETTING);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE, DISABLE_UPDATES_SETTING);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
