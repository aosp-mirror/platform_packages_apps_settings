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
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.R;
import com.android.settings.accessibility.ToggleSwitch.OnBeforeCheckedChangeListener;

public class ToggleContrastPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_CONTRAST_ENABLED;
    private static final String QUICK_SETTING_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_CONTRAST_QUICK_SETTING_ENABLED;

    private CheckBoxPreference mEnableQuickSetting;
    private SeekBarPreference mBrightness;
    private SeekBarPreference mContrast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.accessibility_contrast_settings);

        mEnableQuickSetting = (CheckBoxPreference) findPreference("enable_quick_setting");
        mBrightness = (SeekBarPreference) findPreference("brightness");
        mBrightness.setMax(1000);
        mContrast = (SeekBarPreference) findPreference("contrast");
        mContrast.setMax(1000);

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
        } else if (preference == mBrightness) {
            final int progress = (Integer) newValue;
            final float value = progress / 1000f - 0.5f;
            Settings.Secure.putFloat(
                    getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_BRIGHTNESS, value);
        } else if (preference == mContrast) {
            final int progress = (Integer) newValue;
            final float value = progress / 1000f * 10f + 1f;
            Settings.Secure.putFloat(
                    getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_CONTRAST, value);
        }

        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(getString(R.string.accessibility_display_contrast_preference_title));
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

        final float brightness = Settings.Secure.getFloat(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_BRIGHTNESS, 0);
        final float contrast = Settings.Secure.getFloat(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_CONTRAST, 2);

        // Available brightness range is -0.5 to 0.5.
        mBrightness.setProgress((int) (1000 * (brightness + 0.5f)));
        mBrightness.setOnPreferenceChangeListener(this);

        // Available contrast range is 1 to 10.
        mContrast.setProgress((int) (1000 * (contrast - 1f) / 10f));
        mContrast.setOnPreferenceChangeListener(this);
    }
}
