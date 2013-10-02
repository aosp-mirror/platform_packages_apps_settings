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

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.R;
import com.android.settings.accessibility.ToggleSwitch.OnBeforeCheckedChangeListener;

public class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String TYPE = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER;
    private static final String QUICK_SETTING_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_QUICK_SETTING_ENABLED;
    private static final int DEFAULT_TYPE = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY;

    private CheckBoxPreference mEnableQuickSetting;
    private ListPreference mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.accessibility_daltonizer_settings);

        mEnableQuickSetting = (CheckBoxPreference) findPreference("enable_quick_setting");
        mType = (ListPreference) findPreference("type");

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
        } else if (preference == mType) {
            Settings.Secure.putInt(getContentResolver(), TYPE, Integer.parseInt((String) newValue));
            preference.setSummary("%s");
        }

        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(getString(R.string.accessibility_display_daltonizer_preference_title));
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

        final String value = Integer.toString(
                Settings.Secure.getInt(getContentResolver(), TYPE, DEFAULT_TYPE));
        mType.setValue(value);
        mType.setOnPreferenceChangeListener(this);
        final int index = mType.findIndexOfValue(value);
        if (index < 0) {
            // We're using a mode controlled by developer preferences.
            mType.setSummary(getString(R.string.daltonizer_type_overridden,
                    getString(R.string.simulate_color_space)));
        }
    }
}
