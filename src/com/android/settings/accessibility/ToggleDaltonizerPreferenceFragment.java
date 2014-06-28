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
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.widget.SwitchBar;

public class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String TYPE = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER;
    private static final int DEFAULT_TYPE = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY;

    private ListPreference mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.accessibility_daltonizer_settings);

        mType = (ListPreference) findPreference("type");

        initPreferences();
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? 1 : 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mType) {
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
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();

        mSwitchBar.setCheckedInternal(
                Settings.Secure.getInt(getContentResolver(), ENABLED, 0) == 1);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    private void initPreferences() {
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

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled(mPreferenceKey, isChecked);
    }
}
