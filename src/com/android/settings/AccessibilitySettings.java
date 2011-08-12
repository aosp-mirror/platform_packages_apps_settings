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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.settings.AccessibilitySettings.ToggleSwitch.OnBeforeCheckedChangeListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity with the accessibility settings.
 */
public class AccessibilitySettings extends SettingsPreferenceFragment implements DialogCreatable,
        Preference.OnPreferenceChangeListener {

    private static final String DEFAULT_SCREENREADER_MARKET_LINK =
        "market://search?q=pname:com.google.android.marvin.talkback";

    private static final float LARGE_FONT_SCALE = 1.3f;

    private static final String SYSTEM_PROPERTY_MARKET_URL = "ro.screenreader.market";

    // Timeout before we update the services if packages are added/removed since
    // the AccessibilityManagerService has to do that processing first to generate
    // the AccessibilityServiceInfo we need for proper presentation.
    private static final long DELAY_UPDATE_SERVICES_PREFERENCES_MILLIS = 1000;

    private static final char ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':';

    private static final String KEY_ACCESSIBILITY_TUTORIAL_LAUNCHED_ONCE =
        "key_accessibility_tutorial_launched_once";

    private static final String KEY_INSTALL_ACCESSIBILITY_SERVICE_OFFERED_ONCE =
        "key_install_accessibility_service_offered_once";

    // Preference categories
    private static final String SERVICES_CATEGORY = "services_category";
    private static final String SYSTEM_CATEGORY = "system_category";

    // Preferences
    private static final String TOGGLE_LARGE_TEXT_PREFERENCE = "toggle_large_text_preference";
    private static final String TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE =
        "toggle_power_button_ends_call_preference";
    private static final String TOGGLE_TOUCH_EXPLORATION_PREFERENCE =
        "toggle_touch_exploration_preference";
    private static final String SELECT_LONG_PRESS_TIMEOUT_PREFERENCE =
        "select_long_press_timeout_preference";
    private static final String TOGGLE_SCRIPT_INJECTION_PREFERENCE =
        "toggle_script_injection_preference";

    // Extras passed to sub-fragments.
    private static final String EXTRA_PREFERENCE_KEY = "preference_key";
    private static final String EXTRA_CHECKED = "checked";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_SUMMARY = "summary";
    private static final String EXTRA_WARNING_MESSAGE = "warning_message";
    private static final String EXTRA_SETTINGS_TITLE = "settings_title";
    private static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";

    // Dialog IDs.
    private static final int DIALOG_ID_DISABLE_ACCESSIBILITY = 1;
    private static final int DIALOG_ID_NO_ACCESSIBILITY_SERVICES = 2;

    // Auxiliary members.
    private final SimpleStringSplitter mStringColonSplitter =
        new SimpleStringSplitter(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);

    private final Map<String, String> mLongPressTimeoutValuetoTitleMap =
        new HashMap<String, String>();

    private final Configuration mCurConfig = new Configuration();

    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            updateServicesPreferences(mToggleAccessibilitySwitch.isChecked());
        }
    };

    // Preference controls.
    private ToggleSwitch mToggleAccessibilitySwitch;

    private PreferenceCategory mServicesCategory;
    private PreferenceCategory mSystemsCategory;

    private CheckBoxPreference mToggleLargeTextPreference;
    private CheckBoxPreference mTogglePowerButtonEndsCallPreference;
    private Preference mToggleTouchExplorationPreference;
    private ListPreference mSelectLongPressTimeoutPreference;
    private AccessibilityEnableScriptInjectionPreference mToggleScriptInjectionPreference;

    private int mLongPressTimeoutDefault;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        final boolean accessibilityEnabled = mToggleAccessibilitySwitch.isChecked();
        updateAllPreferences(accessibilityEnabled);
        if (accessibilityEnabled) {
            offerInstallAccessibilitySerivceOnce();
        }
        mSettingsPackageMonitor.register(getActivity(), false);
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        super.onPause();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        addToggleAccessibilitySwitch();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        removeToggleAccessibilitySwitch();
        super.onDestroyView();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSelectLongPressTimeoutPreference) {
            String stringValue = (String) newValue;
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LONG_PRESS_TIMEOUT, Integer.parseInt(stringValue));
            mSelectLongPressTimeoutPreference.setSummary(
                    mLongPressTimeoutValuetoTitleMap.get(stringValue));
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (mToggleLargeTextPreference == preference) {
            handleToggleLargeTextPreferenceClick();
            return true;
        } else if (mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleToggleLargeTextPreferenceClick() {
        try {
            mCurConfig.fontScale = mToggleLargeTextPreference.isChecked() ? LARGE_FONT_SCALE : 1;
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException re) {
            /* ignore */
        }
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                (mTogglePowerButtonEndsCallPreference.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void addToggleAccessibilitySwitch() {
        mToggleAccessibilitySwitch = createAndAddActionBarToggleSwitch(getActivity());
        final boolean checked = (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1);
        mToggleAccessibilitySwitch.setChecked(checked);
        mToggleAccessibilitySwitch.setOnBeforeCheckedChangeListener(
                new OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (!checked) {
                    toggleSwitch.setCheckedInternal(true);
                    showDialog(DIALOG_ID_DISABLE_ACCESSIBILITY);
                    return true;
                }
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                updateAllPreferences(true);
                offerInstallAccessibilitySerivceOnce();
                return false;
            }
        });
    }

    public void removeToggleAccessibilitySwitch() {
        mToggleAccessibilitySwitch.setOnBeforeCheckedChangeListener(null);
        getActivity().getActionBar().setCustomView(null);
    }

    private void initializeAllPreferences() {
        // The basic logic here is if accessibility is not enabled all accessibility
        // settings will have no effect but still their selected state should be kept
        // unchanged, so the user can see what settings will be enabled when turning
        // on accessibility.

        final boolean accessibilityEnabled = (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1);

        mServicesCategory = (PreferenceCategory) findPreference(SERVICES_CATEGORY);
        mSystemsCategory = (PreferenceCategory) findPreference(SYSTEM_CATEGORY);

        // Large text.
        mToggleLargeTextPreference =
            (CheckBoxPreference) findPreference(TOGGLE_LARGE_TEXT_PREFERENCE);
        if (accessibilityEnabled) {
            try {
                mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
            } catch (RemoteException re) {
                /* ignore */
            }
            mToggleLargeTextPreference.setChecked(mCurConfig.fontScale == LARGE_FONT_SCALE);
        }

        // Power button ends calls.
        mTogglePowerButtonEndsCallPreference =
            (CheckBoxPreference) findPreference(TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE);
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                && Utils.isVoiceCapable(getActivity())) {
            if (accessibilityEnabled) {
                final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                        Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
                final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
                mTogglePowerButtonEndsCallPreference.setChecked(powerButtonEndsCall);
            }
        } else {
            mSystemsCategory.removePreference(mTogglePowerButtonEndsCallPreference);
        }

        // Touch exploration enabled.
        mToggleTouchExplorationPreference = findPreference(TOGGLE_TOUCH_EXPLORATION_PREFERENCE);
        final boolean touchExplorationEnabled = (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.TOUCH_EXPLORATION_ENABLED, 0) == 1);
        if (touchExplorationEnabled) {
            mToggleTouchExplorationPreference.setSummary(
                    getString(R.string.accessibility_service_state_on));
            mToggleTouchExplorationPreference.getExtras().putBoolean(EXTRA_CHECKED, true);
        } else {
            mToggleTouchExplorationPreference.setSummary(
                    getString(R.string.accessibility_service_state_off));
            mToggleTouchExplorationPreference.getExtras().putBoolean(EXTRA_CHECKED, false);
        }

        // Long press timeout.
        mSelectLongPressTimeoutPreference =
            (ListPreference) findPreference(SELECT_LONG_PRESS_TIMEOUT_PREFERENCE);
        mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);
        if (mLongPressTimeoutValuetoTitleMap.size() == 0) {
            String[] timeoutValues = getResources().getStringArray(
                    R.array.long_press_timeout_selector_values);
            mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            String[] timeoutTitles = getResources().getStringArray(
                    R.array.long_press_timeout_selector_titles);
            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
               mLongPressTimeoutValuetoTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }
        if (accessibilityEnabled) {
            final int longPressTimeout = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LONG_PRESS_TIMEOUT, mLongPressTimeoutDefault);
            String value = String.valueOf(longPressTimeout);
            mSelectLongPressTimeoutPreference.setValue(value);
            mSelectLongPressTimeoutPreference.setSummary(
                    mLongPressTimeoutValuetoTitleMap.get(value));
        } else {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LONG_PRESS_TIMEOUT,
                    mLongPressTimeoutDefault);
        }

        // Script injection.
        mToggleScriptInjectionPreference = (AccessibilityEnableScriptInjectionPreference)
            findPreference(TOGGLE_SCRIPT_INJECTION_PREFERENCE);
        if (accessibilityEnabled) {
            final boolean  scriptInjectionAllowed = (Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION, 0) == 1);
            mToggleScriptInjectionPreference.setInjectionAllowed(scriptInjectionAllowed);
        }
    }

    private void updateAllPreferences(boolean accessibilityEnabled) {
        updateServicesPreferences(accessibilityEnabled);
        updateSystemPreferences(accessibilityEnabled);
    }

    private void updateServicesPreferences(boolean accessibilityEnabled) {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.

        // Generate.
        mServicesCategory.removeAll();

        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());

        List<AccessibilityServiceInfo> installedServices =
            accessibilityManager.getInstalledAccessibilityServiceList();

        Set<ComponentName> enabledComponentNames = new HashSet<ComponentName>();
        String settingValue = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            SimpleStringSplitter splitter = mStringColonSplitter;
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                enabledComponentNames.add(ComponentName.unflattenFromString(splitter.next()));
            }
        }

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            AccessibilityServiceInfo info = installedServices.get(i);
            String key = info.getId();

            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();

            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            preference.setKey(componentName.flattenToString());

            preference.setTitle(title);
            final boolean enabled = enabledComponentNames.contains(componentName);
            if (enabled) {
                preference.setSummary(getString(R.string.accessibility_service_state_on));
            } else {
                preference.setSummary(getString(R.string.accessibility_service_state_off));
            }

            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServiceFragment.class.getName());
            preference.setPersistent(true);

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, enabled);
            extras.putString(EXTRA_TITLE, title);

            String description = info.getDescription();
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString(EXTRA_SUMMARY, description);

            extras.putString(EXTRA_WARNING_MESSAGE, getString(
                    R.string.accessibility_service_security_warning,
                    info.getResolveInfo().loadLabel(getPackageManager())));

            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.accessibility_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            mServicesCategory.addPreference(preference);
        }

        // Update enabled state.
        mServicesCategory.setEnabled(accessibilityEnabled);
    }

    private void updateSystemPreferences(boolean accessibilityEnabled) {
        // The basic logic here is if accessibility is not enabled all accessibility
        // settings will have no effect but still their selected state should be kept
        // unchanged, so the user can see what settings will be enabled when turning
        // on accessibility.

        // Large text.
        mToggleLargeTextPreference.setEnabled(accessibilityEnabled);
        if (accessibilityEnabled) {
            mCurConfig.fontScale =
                mToggleLargeTextPreference.isChecked() ? LARGE_FONT_SCALE : 1;
        } else {
            mCurConfig.fontScale = 1;
        }
        try {
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException re) {
            /* ignore */
        }

        // Power button ends calls.
        if (mTogglePowerButtonEndsCallPreference != null) {
            mTogglePowerButtonEndsCallPreference.setEnabled(accessibilityEnabled);
            final int powerButtonEndsCall;
            if (accessibilityEnabled) {
                powerButtonEndsCall = mTogglePowerButtonEndsCallPreference.isChecked()
                    ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                    : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF;
            } else {
                powerButtonEndsCall = Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF;
            }
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    powerButtonEndsCall);
        }

        // Touch exploration enabled.
        mToggleTouchExplorationPreference.setEnabled(accessibilityEnabled);
        final boolean touchExplorationEnabled = (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.TOUCH_EXPLORATION_ENABLED, 0) == 1);
        if (touchExplorationEnabled) {
            mToggleTouchExplorationPreference.setSummary(
                    getString(R.string.accessibility_service_state_on));
            mToggleTouchExplorationPreference.getExtras().putBoolean(EXTRA_CHECKED, true);
        } else {
            mToggleTouchExplorationPreference.setSummary(
                    getString(R.string.accessibility_service_state_off));
            mToggleTouchExplorationPreference.getExtras().putBoolean(EXTRA_CHECKED, false);
        }

        // Long press timeout.
        mSelectLongPressTimeoutPreference.setEnabled(accessibilityEnabled);
        final int longPressTimeout;
        if (accessibilityEnabled) {
            String value = mSelectLongPressTimeoutPreference.getValue();
            longPressTimeout = (value != null) ? Integer.parseInt(value) : mLongPressTimeoutDefault;
        } else {
            longPressTimeout = mLongPressTimeoutDefault;
        }
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LONG_PRESS_TIMEOUT,
                longPressTimeout);
        String value = mSelectLongPressTimeoutPreference.getValue();
        mSelectLongPressTimeoutPreference.setSummary(mLongPressTimeoutValuetoTitleMap.get(value));

        // Script injection.
        mToggleScriptInjectionPreference.setEnabled(accessibilityEnabled);
        final boolean scriptInjectionAllowed;
        if (accessibilityEnabled) {
            scriptInjectionAllowed = mToggleScriptInjectionPreference.isInjectionAllowed();
        } else {
            scriptInjectionAllowed = false;
        }
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION,
                scriptInjectionAllowed ? 1 : 0);
    }

    private void offerInstallAccessibilitySerivceOnce() {
        if (mServicesCategory.getPreferenceCount() > 0) {
            return;
        }
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        final boolean offerInstallService = !preferences.getBoolean(
                KEY_INSTALL_ACCESSIBILITY_SERVICE_OFFERED_ONCE, false);
        if (offerInstallService) {
            preferences.edit().putBoolean(KEY_INSTALL_ACCESSIBILITY_SERVICE_OFFERED_ONCE,
                    true).commit();
            // Notify user that they do not have any accessibility
            // services installed and direct them to Market to get TalkBack.
            showDialog(DIALOG_ID_NO_ACCESSIBILITY_SERVICES);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_ID_DISABLE_ACCESSIBILITY:
                return (new AlertDialog.Builder(getActivity()))
                    .setTitle(R.string.accessibility_disable_warning_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getResources().
                            getString(R.string.accessibility_disable_warning_summary))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Settings.Secure.putInt(getContentResolver(),
                                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                                mToggleAccessibilitySwitch.setCheckedInternal(
                                        false);
                                updateAllPreferences(false);
                            }
                    })
                    .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mToggleAccessibilitySwitch.setCheckedInternal(
                                        true);
                            }
                        })
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
                                removeDialog(DIALOG_ID_NO_ACCESSIBILITY_SERVICES);
                                String screenreaderMarketLink = SystemProperties.get(
                                        SYSTEM_PROPERTY_MARKET_URL,
                                        DEFAULT_SCREENREADER_MARKET_LINK);
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

    private class SettingsPackageMonitor extends PackageMonitor {

        @Override
        public void onPackageAdded(String packageName, int uid) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_PREFERENCES_MILLIS);
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_PREFERENCES_MILLIS);
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_PREFERENCES_MILLIS);
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_PREFERENCES_MILLIS);
        }
    }

    private static ToggleSwitch createAndAddActionBarToggleSwitch(Activity activity) {
        ToggleSwitch toggleSwitch = new ToggleSwitch(activity);
        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        toggleSwitch.setPadding(0, 0, padding, 0);
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(toggleSwitch,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        return toggleSwitch;
    }

    public static class ToggleSwitch extends Switch {

        private OnBeforeCheckedChangeListener mOnBeforeListener;

        public static interface OnBeforeCheckedChangeListener {
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked);
        }

        public ToggleSwitch(Context context) {
            super(context);
        }

        public void setOnBeforeCheckedChangeListener(OnBeforeCheckedChangeListener listener) {
            mOnBeforeListener = listener;
        }

        @Override
        public void setChecked(boolean checked) {
            if (mOnBeforeListener != null
                    && mOnBeforeListener.onBeforeCheckedChanged(this, checked)) {
                return;
            }
            super.setChecked(checked);
        }

        public void setCheckedInternal(boolean checked) {
            super.setChecked(checked);
        }
    }

    public static class ToggleAccessibilityServiceFragment extends TogglePreferenceFragment {
        @Override
        public void onPreferenceToggled(String preferenceKey, boolean enabled) {
            String enabledServices = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledServices == null) {
                enabledServices = "";
            }
            final int length = enabledServices.length();
            if (enabled) {
                if (enabledServices.contains(preferenceKey)) {
                    return;
                }
                if (length == 0) {
                    enabledServices += preferenceKey;
                    Settings.Secure.putString(getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabledServices);
                } else if (length > 0) {
                    enabledServices += ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR + preferenceKey;
                    Settings.Secure.putString(getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabledServices);
                }
            } else {
                final int index = enabledServices.indexOf(preferenceKey);
                if (index == 0) {
                    enabledServices = enabledServices.replace(preferenceKey, "");
                    Settings.Secure.putString(getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabledServices);
                } else if (index > 0) {
                    enabledServices = enabledServices.replace(
                            ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR + preferenceKey, "");
                    Settings.Secure.putString(getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabledServices);
                }
            }
        }
    }

    public static class ToggleTouchExplorationFragment extends TogglePreferenceFragment {
        @Override
        public void onPreferenceToggled(String preferenceKey, boolean enabled) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.TOUCH_EXPLORATION_ENABLED, enabled ? 1 : 0);
            if (enabled) {
                SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
                final boolean launchAccessibilityTutorial = !preferences.getBoolean(
                        KEY_ACCESSIBILITY_TUTORIAL_LAUNCHED_ONCE, false);
                if (launchAccessibilityTutorial) {
                    preferences.edit().putBoolean(KEY_ACCESSIBILITY_TUTORIAL_LAUNCHED_ONCE,
                            true).commit();
                    Intent intent = new Intent(AccessibilityTutorialActivity.ACTION);
                    getActivity().startActivity(intent);
                }
            }
        }
    }

    private abstract static class TogglePreferenceFragment extends SettingsPreferenceFragment
            implements DialogInterface.OnClickListener {

        private static final int DIALOG_ID_WARNING = 1;

        private String mPreferenceKey;

        private ToggleSwitch mToggleSwitch;

        private CharSequence mWarningMessage;
        private Preference mSummaryPreference;

        private CharSequence mSettingsTitle;
        private Intent mSettingsIntent;

        // TODO: Showing sub-sub fragment does not handle the activity title
        //       so we do it but this is wrong. Do a real fix when there is time.
        private CharSequence mOldActivityTitle;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            setPreferenceScreen(preferenceScreen);
            mSummaryPreference = new Preference(getActivity()) {
                @Override
                protected void onBindView(View view) {
                    super.onBindView(view);
                    TextView summaryView = (TextView) view.findViewById(R.id.summary);
                    summaryView.setText(getSummary());
                    sendAccessibilityEvent(summaryView);
                }

                private void sendAccessibilityEvent(View view) {
                    // Since the view is still not attached we create, populate,
                    // and send the event directly since we do not know when it
                    // will be attached and posting commands is not as clean.
                    AccessibilityManager accessibilityManager =
                        AccessibilityManager.getInstance(getActivity());
                    if (accessibilityManager.isEnabled()) {
                        AccessibilityEvent event = AccessibilityEvent.obtain();
                        event.setEventType(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                        view.onInitializeAccessibilityEvent(event);
                        view.dispatchPopulateAccessibilityEvent(event);
                        accessibilityManager.sendAccessibilityEvent(event);
                    }
                }
            };
            mSummaryPreference.setPersistent(false);
            mSummaryPreference.setLayoutResource(R.layout.text_description_preference);
            preferenceScreen.addPreference(mSummaryPreference);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            installActionBarToggleSwitch();
            processArguments();
            getListView().setDivider(null);
            getListView().setEnabled(false);
        }

        @Override
        public void onDestroyView() {
            getActivity().getActionBar().setCustomView(null);
            if (mOldActivityTitle != null) {
                getActivity().getActionBar().setTitle(mOldActivityTitle);
            }
            mToggleSwitch.setOnBeforeCheckedChangeListener(null);
            super.onDestroyView();
        }

        public abstract void onPreferenceToggled(String preferenceKey, boolean value);

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            MenuItem menuItem = menu.add(mSettingsTitle);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menuItem.setIntent(mSettingsIntent);
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            switch (dialogId) {
                case DIALOG_ID_WARNING:
                    return new AlertDialog.Builder(getActivity())
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(mWarningMessage)
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // OK, we got the user consent so set checked.
                    mToggleSwitch.setCheckedInternal(true);
                    onPreferenceToggled(mPreferenceKey, true);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    onPreferenceToggled(mPreferenceKey, false);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        private void installActionBarToggleSwitch() {
            mToggleSwitch = createAndAddActionBarToggleSwitch(getActivity());
            mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
                public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                    if (checked) {
                        if (!TextUtils.isEmpty(mWarningMessage)) {
                            toggleSwitch.setCheckedInternal(false);
                            showDialog(DIALOG_ID_WARNING);
                            return true;
                        }
                        onPreferenceToggled(mPreferenceKey, true);
                    } else {
                        onPreferenceToggled(mPreferenceKey, false);
                    }
                    return false;
                }
            });
        }

        private void processArguments() {
            Bundle arguments = getArguments();

            // Key.
            mPreferenceKey = arguments.getString(EXTRA_PREFERENCE_KEY);

            // Enabled.
            final boolean enabled = arguments.getBoolean(EXTRA_CHECKED);
            mToggleSwitch.setCheckedInternal(enabled);

            // Title.
            PreferenceActivity activity = (PreferenceActivity) getActivity();
            if (!activity.onIsMultiPane() || activity.onIsHidingHeaders()) {
                mOldActivityTitle = getActivity().getTitle();
                String title = arguments.getString(EXTRA_TITLE);
                getActivity().getActionBar().setTitle(arguments.getCharSequence(EXTRA_TITLE));
            }

            // Summary.
            String summary = arguments.getString(EXTRA_SUMMARY);
            mSummaryPreference.setSummary(summary);

            // Settings title and intent.
            String settingsTitle = arguments.getString(EXTRA_SETTINGS_TITLE);
            String settingsComponentName = arguments.getString(EXTRA_SETTINGS_COMPONENT_NAME);
            if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
                Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                        ComponentName.unflattenFromString(settingsComponentName.toString()));
                if (!getPackageManager().queryIntentActivities(settingsIntent, 0).isEmpty()) {
                    mSettingsTitle = settingsTitle;
                    mSettingsIntent = settingsIntent;
                    setHasOptionsMenu(true);
                }
            }

            // Waring message.
            mWarningMessage = arguments.getCharSequence(
                    AccessibilitySettings.EXTRA_WARNING_MESSAGE);
        }
    }
}
