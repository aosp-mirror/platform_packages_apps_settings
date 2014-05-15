/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.R;

public class ToggleInversionPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;
    private static final String QUICK_SETTING_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_QUICK_SETTING_ENABLED;

    private CheckBoxPreference mEnableQuickSetting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.accessibility_inversion_settings);

        mEnableQuickSetting = (CheckBoxPreference) findPreference("enable_quick_setting");

        initPreferences();
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? 1 : 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnableQuickSetting) {
            Settings.Secure.putInt(
                    getContentResolver(), QUICK_SETTING_ENABLED, ((Boolean) newValue) ? 1 : 0);
        }

        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(getString(R.string.accessibility_display_inversion_preference_title));
    }

    @Override
    protected void onInstallActionBarToggleSwitch() {
        super.onInstallActionBarToggleSwitch();

        mToggleSwitch.setCheckedInternal(
                Settings.Secure.getInt(getContentResolver(), ENABLED, 0) == 1);
        mToggleSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                onPreferenceToggled(mPreferenceKey, checked);
            }
        });
    }

    private void initPreferences() {
        mEnableQuickSetting.setChecked(
                Settings.Secure.getInt(getContentResolver(), QUICK_SETTING_ENABLED, 0) == 1);
        mEnableQuickSetting.setOnPreferenceChangeListener(this);
    }
}
