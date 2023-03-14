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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Preference for requiring authorization to use home controls for certain devices.
 */
public class ControlsTrivialPrivacyPreferenceController extends TogglePreferenceController {

    private static final String SETTING_KEY = Settings.Secure.LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS;
    private static final String DEPENDENCY_SETTING_KEY = Settings.Secure.LOCKSCREEN_SHOW_CONTROLS;

    public ControlsTrivialPrivacyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY,
                isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        if (!CustomizableLockScreenUtils.isFeatureEnabled(mContext)
                && getAvailabilityStatus() == DISABLED_DEPENDENT_SETTING) {
            return mContext.getText(R.string.lockscreen_trivial_disabled_controls_summary);
        }

        return mContext.getText(R.string.lockscreen_trivial_controls_summary);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() != DISABLED_DEPENDENT_SETTING);
        refreshSummary(preference);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public int getAvailabilityStatus() {
        return showDeviceControlsSettingsEnabled() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    private boolean showDeviceControlsSettingsEnabled() {
        return CustomizableLockScreenUtils.isFeatureEnabled(mContext)
                || Settings.Secure.getInt(
                        mContext.getContentResolver(), DEPENDENCY_SETTING_KEY, 0) != 0;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (CustomizableLockScreenUtils.isFeatureEnabled(mContext)) {
            return;
        }

        Preference currentPreference = screen.findPreference(getPreferenceKey());
        currentPreference.setDependency("lockscreen_privacy_controls_switch");
    }
}
