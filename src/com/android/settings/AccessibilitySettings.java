/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity with the accessibility settings.
 */
public class AccessibilitySettings extends PreferenceActivity {
    private final String TOGGLE_ACCESSIBILITY_SERVICE_CHECKBOX =
        "toggle_accessibility_service_checkbox";

    private static final String ACCESSIBILITY_SERVICES_CATEGORY =
        "accessibility_services_category";

    private CheckBoxPreference mToggleCheckBox;

    private Map<String, ServiceInfo> mAccessibilityServices =
        new LinkedHashMap<String, ServiceInfo>();

    private TextUtils.SimpleStringSplitter mStringColonSplitter =
        new TextUtils.SimpleStringSplitter(':');

    private PreferenceGroup mAccessibilityServicesCategory;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings);

        mToggleCheckBox = (CheckBoxPreference) findPreference(
            TOGGLE_ACCESSIBILITY_SERVICE_CHECKBOX);

        addAccessibilitServicePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final HashSet<String> enabled = new HashSet<String>();
        String settingValue = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                enabled.add(splitter.next());
            }
        }

        Map<String, ServiceInfo> accessibilityServices = mAccessibilityServices;

        for (String key : accessibilityServices.keySet()) {
            CheckBoxPreference preference = (CheckBoxPreference) findPreference(key);
            if (preference != null) {
                preference.setChecked(enabled.contains(key));
            }
        }

        int serviceState = Settings.Secure.getInt(getContentResolver(),
            Settings.Secure.ACCESSIBILITY_ENABLED, 0);

        if (!accessibilityServices.isEmpty()) {
            if (serviceState == 1) {
                mToggleCheckBox.setChecked(true);
            } else {
                setAccessibilityServicePreferencesState(false);
            }
            mToggleCheckBox.setEnabled(true);
        } else {
            if (serviceState == 1) {
                // no service and accessibility is enabled => disable
                Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                setAccessibilityServicePreferencesState(false);
            }
            mToggleCheckBox.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        persistEnabledAccessibilityServices();
    }

    /**
     * Sets the state of the preferences for enabling/disabling AccessibilityServices.
     *
     * @param isEnabled If to enable or disable the preferences.
     */
    private void setAccessibilityServicePreferencesState(boolean isEnabled) {
        if (mAccessibilityServicesCategory == null) {
            return;
        }

        int count = mAccessibilityServicesCategory.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = mAccessibilityServicesCategory.getPreference(i);
            pref.setEnabled(isEnabled);
            if (!isEnabled){
               ((CheckBoxPreference) pref).setChecked(false);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (TOGGLE_ACCESSIBILITY_SERVICE_CHECKBOX.equals(key)) {
            boolean isChecked = ((CheckBoxPreference) preference).isChecked();
            handleEnableAccessibilityStateChange((CheckBoxPreference) preference);
        } else if (preference instanceof CheckBoxPreference) {
            handleEnableAccessibilityServiceStateChange((CheckBoxPreference) preference);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Handles the change of the accessibility enabled setting state.
     *
     * @param preference The preference for enabling/disabling accessibility.
     */
    private void handleEnableAccessibilityStateChange(CheckBoxPreference preference) {
        if (preference.isChecked()) {
            Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            setAccessibilityServicePreferencesState(true);
        } else {
            final CheckBoxPreference checkBoxPreference = preference;
            AlertDialog dialog = (new AlertDialog.Builder(this))
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.accessibility_service_disable_warning))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getContentResolver(),
                                Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                            setAccessibilityServicePreferencesState(false);
                        }
                })
                .setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            checkBoxPreference.setChecked(true);
                        }
                })
                .create();
            dialog.show();
        }
    }

    /**
     * Handles the change of the preference for enabling/disabling an AccessibilityService.
     *
     * @param preference The preference.
     */
    private void handleEnableAccessibilityServiceStateChange(CheckBoxPreference preference) {
        if (preference.isChecked()) {
            final CheckBoxPreference checkBoxPreference = preference;
            AlertDialog dialog = (new AlertDialog.Builder(this))
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.accessibility_service_security_warning,
                    mAccessibilityServices.get(preference.getKey())
                    .applicationInfo.loadLabel(getPackageManager())))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                checkBoxPreference.setChecked(true);
                                persistEnabledAccessibilityServices();
                            }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                checkBoxPreference.setChecked(false);
                            }
                })
                .create();
            dialog.show();
        } else {
            persistEnabledAccessibilityServices();
        }
    }

    /**
     * Persists the Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES setting.
     * The AccessibilityManagerService watches this property and manages the
     * AccessibilityServices.
     */
    private void persistEnabledAccessibilityServices() {
        StringBuilder builder = new StringBuilder(256);

        int firstEnabled = -1;
        for (String key : mAccessibilityServices.keySet()) {
            CheckBoxPreference preference = (CheckBoxPreference) findPreference(key);
            if (preference.isChecked()) {
                 builder.append(key);
                 builder.append(':');
            }
        }

        Settings.Secure.putString(getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, builder.toString());
    }

    /**
     * Adds {@link CheckBoxPreference} for enabling or disabling an accessibility services.
     */
    private void addAccessibilitServicePreferences() {
        AccessibilityManager accessibilityManager =
            (AccessibilityManager) getSystemService(Service.ACCESSIBILITY_SERVICE);

        List<ServiceInfo> installedServices = accessibilityManager.getAccessibilityServiceList();

        mAccessibilityServicesCategory =
            (PreferenceGroup) findPreference(ACCESSIBILITY_SERVICES_CATEGORY);

        if (installedServices.isEmpty()) {
            getPreferenceScreen().removePreference(mAccessibilityServicesCategory);
            mAccessibilityServicesCategory = null;
            return;
        }

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            ServiceInfo serviceInfo = installedServices.get(i);
            String key = serviceInfo.packageName + "/" + serviceInfo.name;

            mAccessibilityServices.put(key, serviceInfo);

            CheckBoxPreference preference = new CheckBoxPreference(this);
            preference.setKey(key);
            preference.setTitle(serviceInfo.loadLabel(getPackageManager()));
            mAccessibilityServicesCategory.addPreference(preference);
        }
    }
}
