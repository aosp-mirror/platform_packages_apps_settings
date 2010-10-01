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
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity with the accessibility settings.
 */
public class AccessibilitySettings extends SettingsPreferenceFragment implements DialogCreatable {
    private static final String DEFAULT_SCREENREADER_MARKET_LINK =
        "market://search?q=pname:com.google.android.marvin.talkback";

    private final String TOGGLE_ACCESSIBILITY_SERVICE_CHECKBOX =
        "toggle_accessibility_service_checkbox";

    private static final String ACCESSIBILITY_SERVICES_CATEGORY =
        "accessibility_services_category";

    private static final String TOGGLE_ACCESSIBILITY_SCRIPT_INJECTION_CHECKBOX =
        "toggle_accessibility_script_injection_checkbox";

    private static final String POWER_BUTTON_CATEGORY =
        "power_button_category";

    private final String POWER_BUTTON_ENDS_CALL_CHECKBOX =
        "power_button_ends_call";

    private static final int DIALOG_ID_DISABLE_ACCESSIBILITY = 1;
    private static final int DIALOG_ID_ENABLE_SCRIPT_INJECTION = 2;
    private static final int DIALOG_ID_ENABLE_ACCESSIBILITY_SERVICE = 3;
    private static final int DIALOG_ID_NO_ACCESSIBILITY_SERVICES = 4;

    private CheckBoxPreference mToggleAccessibilityCheckBox;
    private CheckBoxPreference mToggleScriptInjectionCheckBox;
    private CheckBoxPreference mToggleAccessibilityServiceCheckBox;

    private PreferenceCategory mPowerButtonCategory;
    private CheckBoxPreference mPowerButtonEndsCallCheckBox;

    private PreferenceGroup mAccessibilityServicesCategory;

    private Map<String, ServiceInfo> mAccessibilityServices =
        new LinkedHashMap<String, ServiceInfo>();

    private TextUtils.SimpleStringSplitter mStringColonSplitter =
        new TextUtils.SimpleStringSplitter(':');

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.accessibility_settings);

        mAccessibilityServicesCategory =
            (PreferenceGroup) findPreference(ACCESSIBILITY_SERVICES_CATEGORY);

        mToggleAccessibilityCheckBox = (CheckBoxPreference) findPreference(
                TOGGLE_ACCESSIBILITY_SERVICE_CHECKBOX);

        mToggleScriptInjectionCheckBox = (CheckBoxPreference) findPreference(
                TOGGLE_ACCESSIBILITY_SCRIPT_INJECTION_CHECKBOX);

        mPowerButtonCategory = (PreferenceCategory) findPreference(POWER_BUTTON_CATEGORY);
        mPowerButtonEndsCallCheckBox = (CheckBoxPreference) findPreference(
                POWER_BUTTON_ENDS_CALL_CHECKBOX);

        // set the accessibility script injection category
        boolean scriptInjectionEnabled = (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION, 0) == 1);
        mToggleScriptInjectionCheckBox.setChecked(scriptInjectionEnabled);
        mToggleScriptInjectionCheckBox.setEnabled(true);

        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                && Utils.isVoiceCapable(getActivity())) {
            int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            // The checkbox is labeled "Power button ends call"; thus the in-call
            // Power button behavior is INCALL_POWER_BUTTON_BEHAVIOR_HANGUP if
            // checked, and INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF if unchecked.
            boolean powerButtonCheckboxEnabled =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerButtonEndsCallCheckBox.setChecked(powerButtonCheckboxEnabled);
            mPowerButtonEndsCallCheckBox.setEnabled(true);
        } else {
            // No POWER key on the current device or no voice capability;
            // this entire category is irrelevant.
            getPreferenceScreen().removePreference(mPowerButtonCategory);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        persistEnabledAccessibilityServices();
    }

    @Override
    public void onResume() {
        super.onResume();

        addAccessibilitServicePreferences();

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
                mToggleAccessibilityCheckBox.setChecked(true);
            } else {
                setAccessibilityServicePreferencesState(false);
            }
            mToggleAccessibilityCheckBox.setEnabled(true);
        } else {
            if (serviceState == 1) {
                // no service and accessibility is enabled => disable
                Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            }
            mToggleAccessibilityCheckBox.setEnabled(false);
            // Notify user that they do not have any accessibility apps
            // installed and direct them to Market to get TalkBack
            displayNoAppsAlert();
        }
    }

    /**
     * Sets the state of the preferences for enabling/disabling
     * AccessibilityServices.
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
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (TOGGLE_ACCESSIBILITY_SERVICE_CHECKBOX.equals(key)) {
            handleEnableAccessibilityStateChange((CheckBoxPreference) preference);
        } else if (POWER_BUTTON_ENDS_CALL_CHECKBOX.equals(key)) {
            boolean isChecked = ((CheckBoxPreference) preference).isChecked();
            // The checkbox is labeled "Power button ends call"; thus the in-call
            // Power button behavior is INCALL_POWER_BUTTON_BEHAVIOR_HANGUP if
            // checked, and INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF if unchecked.
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    (isChecked ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                            : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
        } else if (TOGGLE_ACCESSIBILITY_SCRIPT_INJECTION_CHECKBOX.equals(key)) {
            handleToggleAccessibilityScriptInjection((CheckBoxPreference) preference);
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
            // set right enabled state since the user may press back
            preference.setChecked(true);
            showDialog(DIALOG_ID_DISABLE_ACCESSIBILITY);
        }
    }

    /**
     * Handles the change of the accessibility script injection setting state.
     *
     * @param preference The preference for enabling/disabling accessibility script injection.
     */
    private void handleToggleAccessibilityScriptInjection(CheckBoxPreference preference) {
        if (preference.isChecked()) {
            // set right enabled state since the user may press back
            preference.setChecked(false);
            showDialog(DIALOG_ID_ENABLE_SCRIPT_INJECTION);
        } else {
            Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION, 0);
        }
    }

    /**
     * Handles the change of the preference for enabling/disabling an AccessibilityService.
     *
     * @param preference The preference.
     */
    private void handleEnableAccessibilityServiceStateChange(CheckBoxPreference preference) {
        if (preference.isChecked()) {
            mToggleAccessibilityServiceCheckBox = preference;
            // set right enabled state since the user may press back
            preference.setChecked(false);
            showDialog(DIALOG_ID_ENABLE_ACCESSIBILITY_SERVICE);
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

        if (installedServices.isEmpty()) {
            getPreferenceScreen().removePreference(mAccessibilityServicesCategory);
            return;
        }

        getPreferenceScreen().addPreference(mAccessibilityServicesCategory);

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            ServiceInfo serviceInfo = installedServices.get(i);
            String key = serviceInfo.packageName + "/" + serviceInfo.name;

            if (mAccessibilityServices.put(key, serviceInfo) == null) {
                CheckBoxPreference preference = new CheckBoxPreference(getActivity());
                preference.setKey(key);
                preference.setTitle(serviceInfo.loadLabel(getActivity().getPackageManager()));
                mAccessibilityServicesCategory.addPreference(preference);
            }
        }
    }

    /**
     * Displays a message telling the user that they do not have any accessibility
     * related apps installed and that they can get TalkBack (Google's free screen
     * reader) from Market.
     */
    private void displayNoAppsAlert() {
        try {
            PackageManager pm = getActivity().getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo("com.android.vending", 0);
            showDialog(DIALOG_ID_NO_ACCESSIBILITY_SERVICES);
        } catch (NameNotFoundException e) {
            // This is a no-op if the user does not have Android Market
            return;
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_ID_DISABLE_ACCESSIBILITY:
                return (new AlertDialog.Builder(getActivity()))
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getResources().
                            getString(R.string.accessibility_service_disable_warning))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Settings.Secure.putInt(getContentResolver(),
                                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                                mToggleAccessibilityCheckBox.setChecked(false);
                                setAccessibilityServicePreferencesState(false);
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            case DIALOG_ID_ENABLE_SCRIPT_INJECTION:
                return new AlertDialog.Builder(getActivity())
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getActivity().getString(
                        R.string.accessibility_script_injection_security_warning))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION, 1);
                            mToggleScriptInjectionCheckBox.setChecked(true);
                        }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
            case DIALOG_ID_ENABLE_ACCESSIBILITY_SERVICE:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getResources().getString(
                            R.string.accessibility_service_security_warning,
                            mAccessibilityServices.get(mToggleAccessibilityServiceCheckBox.getKey())
                            .applicationInfo.loadLabel(getActivity().getPackageManager())))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mToggleAccessibilityServiceCheckBox.setChecked(true);
                                    persistEnabledAccessibilityServices();
                                }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            case DIALOG_ID_NO_ACCESSIBILITY_SERVICES:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.accessibility_service_no_apps_title)
                    .setMessage(R.string.accessibility_service_no_apps_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // dismiss the dialog before launching the activity otherwise
                                // the dialog removal occurs after onSaveInstanceState which
                                // triggers an exception
                                dialog.dismiss();
                                String screenreaderMarketLink = SystemProperties.get(
                                    "ro.screenreader.market", DEFAULT_SCREENREADER_MARKET_LINK);
                                Uri marketUri = Uri.parse(screenreaderMarketLink);
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                                startActivity(marketIntent);
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            default:
                return null;
        }
    }
}
