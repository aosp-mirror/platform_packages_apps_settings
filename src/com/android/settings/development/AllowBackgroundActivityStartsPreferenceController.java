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
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class AllowBackgroundActivityStartsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String BACKGROUND_ACTIVITY_STARTS_ENABLED_KEY
            = "allow_background_activity_starts";

    /** Key in DeviceConfig that stores the default for the preference (as a boolean). */
    @VisibleForTesting
    static final String KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED =
            "default_background_activity_starts_enabled";

    public AllowBackgroundActivityStartsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return BACKGROUND_ACTIVITY_STARTS_ENABLED_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSetting((boolean) newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.BACKGROUND_ACTIVITY_STARTS_ENABLED, -1);

        boolean isEnabled = mode < 0 ? isDefaultEnabled() : mode != 0;
        ((SwitchPreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        clearSetting();
        updateState(mPreference);
    }

    private void writeSetting(boolean isEnabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BACKGROUND_ACTIVITY_STARTS_ENABLED, isEnabled ? 1 : 0);
    }

    private void clearSetting() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BACKGROUND_ACTIVITY_STARTS_ENABLED, -1);
    }

    private boolean isDefaultEnabled() {
        // The default in the absence of user preference is settable via DeviceConfig.
        // Note that the default default is disabled.
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_ACTIVITY_MANAGER,
                KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED,
                /*defaultValue*/ false);
    }
}
