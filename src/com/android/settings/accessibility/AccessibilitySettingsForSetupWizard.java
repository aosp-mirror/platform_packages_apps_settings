/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Activity with the accessibility settings specific to Setup Wizard.
 */
public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment
        implements DialogCreatable, Preference.OnPreferenceChangeListener {

    // Preferences.
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE =
            "screen_magnification_preference";
    private static final String DISPLAY_DALTONIZER_PREFERENCE = "daltonizer_preference";
    private static final String TALKBACK_PREFERENCE = "talkback_preference";

    private static final String TALKBACK_NAME = "Talkback";

    // Preference controls.
    private Preference mDisplayMagnificationPreference;
    private Preference mDisplayDaltonizerPreference;
    private Preference mTalkbackPreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.ACCESSIBILITY;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_accessibility;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings_for_setup_wizard);

        mDisplayMagnificationPreference = findPreference(DISPLAY_MAGNIFICATION_PREFERENCE);
        mDisplayDaltonizerPreference = findPreference(DISPLAY_DALTONIZER_PREFERENCE);

        mTalkbackPreference = findPreference(TALKBACK_PREFERENCE);
        mTalkbackPreference.setTitle(TALKBACK_NAME);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mDisplayMagnificationPreference == preference) {
            Bundle extras = mDisplayMagnificationPreference.getExtras();
            extras.putString(AccessibilitySettings.EXTRA_TITLE,
                    getString(R.string.accessibility_screen_magnification_title));
            extras.putCharSequence(AccessibilitySettings.EXTRA_SUMMARY,
                    getActivity().getResources().getText(
                    R.string.accessibility_screen_magnification_summary));
            extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void updatePreferences() {
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                mDisplayMagnificationPreference);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                mDisplayDaltonizerPreference);
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        final boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
        pref.setSummary(enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off);
    }
}
