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
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

/**
 * Activity with the accessibility settings specific to Setup Wizard.
 */
public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment
        implements DialogCreatable, Preference.OnPreferenceChangeListener {

    private static final String TAG = AccessibilitySettingsForSetupWizard.class.getSimpleName();

    // Preferences.
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE =
            "screen_magnification_preference";
    private static final String SCREEN_READER_PREFERENCE = "screen_reader_preference";
    private static final String FONT_SIZE_PREFERENCE = "font_size_preference";

    // Preference controls.
    private Preference mDisplayMagnificationPreference;
    private Preference mScreenReaderPreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.SUW_ACCESSIBILITY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings_for_setup_wizard);

        mDisplayMagnificationPreference = findPreference(DISPLAY_MAGNIFICATION_PREFERENCE);
        mScreenReaderPreference = findPreference(SCREEN_READER_PREFERENCE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateScreenReaderPreference();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
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
                    getText(R.string.accessibility_screen_magnification_summary));
            extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        }

        return super.onPreferenceTreeClick(preference);
    }

    private AccessibilityServiceInfo findFirstServiceWithSpokenFeedback() {
        final AccessibilityManager manager =
                getActivity().getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> accessibilityServices =
                manager.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if ((info.feedbackType & AccessibilityServiceInfo.FEEDBACK_SPOKEN) != 0) {
                return info;
            }
        }

        return null;
    }

    private void updateScreenReaderPreference() {
        // Find a screen reader.
        AccessibilityServiceInfo info = findFirstServiceWithSpokenFeedback();
        if (info == null) {
            mScreenReaderPreference.setEnabled(false);
        } else {
            mScreenReaderPreference.setEnabled(true);
        }

        ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
        String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();
        mScreenReaderPreference.setTitle(title);

        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        mScreenReaderPreference.setKey(componentName.flattenToString());

        // Update the extras.
        Bundle extras = mScreenReaderPreference.getExtras();
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);

        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                mScreenReaderPreference.getKey());
        extras.putString(AccessibilitySettings.EXTRA_TITLE, title);

        String description = info.loadDescription(getPackageManager());
        if (TextUtils.isEmpty(description)) {
            description = getString(R.string.accessibility_service_default_description);
        }
        extras.putString(AccessibilitySettings.EXTRA_SUMMARY, description);
    }
}
