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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.logging.MetricsProto;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

/**
 * Activity with the accessibility settings specific to Setup Wizard.
 */
public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment
        implements DialogCreatable, Preference.OnPreferenceChangeListener {

    // Preferences.
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE =
            "screen_magnification_preference";
    private static final String TALKBACK_PREFERENCE = "talkback_preference";
    private static final String FONT_SIZE_PREFERENCE = "font_size_preference";

    private static final String TALKBACK_NAME = "Talkback";

    // Preference controls.
    private Preference mDisplayMagnificationPreference;
    private Preference mFontSizePreference;
    private Preference mTalkbackPreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ACCESSIBILITY;
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
        mFontSizePreference = findPreference(FONT_SIZE_PREFERENCE);

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

    /**
     * Returns a semicolon-delimited string containing a list of all the
     * installed {@link AccessibilityService}s that provide at least one
     * required feedback type.
     *
     * @param context The {@link android.app.Activity} context.
     * @param requiredFeedbackTypes An integer mask containing the required
     *            feedback types.
     * @return A semicolon-delimited string containing a list of accessibility services.
     */
    private String getAccessibilityServicesFiltered(
            Context context, int requiredFeedbackTypes) {
        final AccessibilityManager manager = context.getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> accessibilityServices = manager
                .getInstalledAccessibilityServiceList();
        final StringBuilder servicesToEnable = new StringBuilder();

        for (AccessibilityServiceInfo accessibilityService : accessibilityServices) {
            if ((accessibilityService.feedbackType & requiredFeedbackTypes) == 0) {
                continue;
            }

            final ServiceInfo serviceInfo = accessibilityService.getResolveInfo().serviceInfo;
            final ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            servicesToEnable.append(componentName.flattenToString());
            servicesToEnable.append(':');
        }

        if (servicesToEnable.length() > 0) {
            servicesToEnable.deleteCharAt(servicesToEnable.length() - 1);
        }

        return servicesToEnable.toString();
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
        } else if (mTalkbackPreference == preference) {
            // Toggle Talkback. The tutorial will automatically start when Talkback is first
            //  activated.
            final ContentResolver resolver = getContentResolver();

            final boolean enable =
                    Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 0;
            final String servicesToEnable = getAccessibilityServicesFiltered(
                    getActivity(), AccessibilityServiceInfo.FEEDBACK_SPOKEN);

            // Enable all accessibility services with spoken feedback type.
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    enable ? servicesToEnable : "");

            // Allow the services we just enabled to toggle touch exploration.
            Settings.Secure.putString(resolver,
                    Settings.Secure.TOUCH_EXPLORATION_GRANTED_ACCESSIBILITY_SERVICES,
                    enable ? servicesToEnable : "");

            // Enable touch exploration.
            Settings.Secure.putInt(resolver, Settings.Secure.TOUCH_EXPLORATION_ENABLED,
                    enable ? 1 : 0);

            // Turn on accessibility mode last, since enabling accessibility with no
            // services has no effect.
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, enable ? 1 : 0);
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void updatePreferences() {
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                mDisplayMagnificationPreference);
        updateFontSizeSummary(mFontSizePreference);
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        final boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
        pref.setSummary(enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off);
    }

    private void updateFontSizeSummary(Preference pref) {
        final Resources res = getContext().getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = ToggleFontSizePreferenceFragment.floatToIndex(
                res.getConfiguration().fontScale, strEntryValues);
        pref.setSummary(entries[index]);
    }
}
