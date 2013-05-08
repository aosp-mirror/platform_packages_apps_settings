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
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.internal.view.RotationPolicy;
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
    // the AccessibilityManagerService has to do that processing first to
    // generate
    // the AccessibilityServiceInfo we need for proper presentation.
    private static final long DELAY_UPDATE_SERVICES_MILLIS = 1000;

    private static final char ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':';

    private static final String KEY_INSTALL_ACCESSIBILITY_SERVICE_OFFERED_ONCE =
            "key_install_accessibility_service_offered_once";

    // Preference categories
    private static final String SERVICES_CATEGORY = "services_category";
    private static final String SYSTEM_CATEGORY = "system_category";

    // Preferences
    private static final String TOGGLE_LARGE_TEXT_PREFERENCE =
            "toggle_large_text_preference";
    private static final String TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE =
            "toggle_power_button_ends_call_preference";
    private static final String TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE =
            "toggle_lock_screen_rotation_preference";
    private static final String TOGGLE_SPEAK_PASSWORD_PREFERENCE =
            "toggle_speak_password_preference";
    private static final String SELECT_LONG_PRESS_TIMEOUT_PREFERENCE =
            "select_long_press_timeout_preference";
    private static final String ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN =
            "enable_global_gesture_preference_screen";
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN =
            "screen_magnification_preference_screen";

    // Extras passed to sub-fragments.
    private static final String EXTRA_PREFERENCE_KEY = "preference_key";
    private static final String EXTRA_CHECKED = "checked";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_SUMMARY = "summary";
    private static final String EXTRA_SETTINGS_TITLE = "settings_title";
    private static final String EXTRA_COMPONENT_NAME = "component_name";
    private static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";

    // Dialog IDs.
    private static final int DIALOG_ID_NO_ACCESSIBILITY_SERVICES = 1;

    // Auxiliary members.
    private final static SimpleStringSplitter sStringColonSplitter =
            new SimpleStringSplitter(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);

    private static final Set<ComponentName> sInstalledServices = new HashSet<ComponentName>();

    private final Map<String, String> mLongPressTimeoutValuetoTitleMap =
            new HashMap<String, String>();

    private final Configuration mCurConfig = new Configuration();

    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            loadInstalledServices();
            updateServicesPreferences();
        }
    };

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            loadInstalledServices();
            updateServicesPreferences();
        }
    };

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    updateLockScreenRotationCheckbox();
                }
            };

    // Preference controls.
    private PreferenceCategory mServicesCategory;
    private PreferenceCategory mSystemsCategory;

    private CheckBoxPreference mToggleLargeTextPreference;
    private CheckBoxPreference mTogglePowerButtonEndsCallPreference;
    private CheckBoxPreference mToggleLockScreenRotationPreference;
    private CheckBoxPreference mToggleSpeakPasswordPreference;
    private ListPreference mSelectLongPressTimeoutPreference;
    private Preference mNoServicesMessagePreference;
    private PreferenceScreen mDisplayMagnificationPreferenceScreen;
    private PreferenceScreen mGlobalGesturePreferenceScreen;

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
        loadInstalledServices();
        updateAllPreferences();

        offerInstallAccessibilitySerivceOnce();

        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
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
        if (mToggleLargeTextPreference == preference) {
            handleToggleLargeTextPreferenceClick();
            return true;
        } else if (mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (mToggleLockScreenRotationPreference == preference) {
            handleLockScreenRotationPreferenceClick();
            return true;
        } else if (mToggleSpeakPasswordPreference == preference) {
            handleToggleSpeakPasswordPreferenceClick();
            return true;
        } else if (mGlobalGesturePreferenceScreen == preference) {
            handleTogglEnableAccessibilityGesturePreferenceClick();
            return true;
        } else if (mDisplayMagnificationPreferenceScreen == preference) {
            handleDisplayMagnificationPreferenceScreenClick();
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

    private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(),
                !mToggleLockScreenRotationPreference.isChecked());
    }

    private void handleToggleSpeakPasswordPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD,
                mToggleSpeakPasswordPreference.isChecked() ? 1 : 0);
    }

    private void handleTogglEnableAccessibilityGesturePreferenceClick() {
        Bundle extras = mGlobalGesturePreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_global_gesture_preference_title));
        extras.putString(EXTRA_SUMMARY, getString(
                R.string.accessibility_global_gesture_preference_description));
        extras.putBoolean(EXTRA_CHECKED, Settings.Global.getInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mGlobalGesturePreferenceScreen,
                mGlobalGesturePreferenceScreen);
    }

    private void handleDisplayMagnificationPreferenceScreenClick() {
        Bundle extras = mDisplayMagnificationPreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_screen_magnification_title));
        extras.putCharSequence(EXTRA_SUMMARY, getActivity().getResources().getText(
                R.string.accessibility_screen_magnification_summary));
        extras.putBoolean(EXTRA_CHECKED, Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mDisplayMagnificationPreferenceScreen,
                mDisplayMagnificationPreferenceScreen);
    }

    private void initializeAllPreferences() {
        mServicesCategory = (PreferenceCategory) findPreference(SERVICES_CATEGORY);
        mSystemsCategory = (PreferenceCategory) findPreference(SYSTEM_CATEGORY);

        // Large text.
        mToggleLargeTextPreference =
                (CheckBoxPreference) findPreference(TOGGLE_LARGE_TEXT_PREFERENCE);

        // Power button ends calls.
        mTogglePowerButtonEndsCallPreference =
                (CheckBoxPreference) findPreference(TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE);
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || !Utils.isVoiceCapable(getActivity())) {
            mSystemsCategory.removePreference(mTogglePowerButtonEndsCallPreference);
        }

        // Lock screen rotation.
        mToggleLockScreenRotationPreference =
                (CheckBoxPreference) findPreference(TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE);

        // Speak passwords.
        mToggleSpeakPasswordPreference =
                (CheckBoxPreference) findPreference(TOGGLE_SPEAK_PASSWORD_PREFERENCE);

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

        // Display magnification.
        mDisplayMagnificationPreferenceScreen = (PreferenceScreen) findPreference(
                DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN);

        // Global gesture.
        mGlobalGesturePreferenceScreen =
                (PreferenceScreen) findPreference(ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN);
    }

    private void updateAllPreferences() {
        updateServicesPreferences();
        updateSystemPreferences();
    }

    private void updateServicesPreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.

        // Generate.
        mServicesCategory.removeAll();

        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());

        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = getEnabledServicesFromSettings(getActivity());

        final boolean accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            AccessibilityServiceInfo info = installedServices.get(i);

            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();

            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            preference.setKey(componentName.flattenToString());

            preference.setTitle(title);
            final boolean serviceEnabled = accessibilityEnabled
                    && enabledServices.contains(componentName);
            if (serviceEnabled) {
                preference.setSummary(getString(R.string.accessibility_feature_state_on));
            } else {
                preference.setSummary(getString(R.string.accessibility_feature_state_off));
            }

            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);

            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString(EXTRA_SUMMARY, description);

            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.accessibility_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            extras.putParcelable(EXTRA_COMPONENT_NAME, componentName);

            mServicesCategory.addPreference(preference);
        }

        if (mServicesCategory.getPreferenceCount() == 0) {
            if (mNoServicesMessagePreference == null) {
                mNoServicesMessagePreference = new Preference(getActivity()) {
                    @Override
                    protected void onBindView(View view) {
                        super.onBindView(view);
                        TextView summaryView = (TextView) view.findViewById(R.id.summary);
                        String title = getString(R.string.accessibility_no_services_installed);
                        summaryView.setText(title);
                    }
                };
                mNoServicesMessagePreference.setPersistent(false);
                mNoServicesMessagePreference.setLayoutResource(
                        R.layout.text_description_preference);
                mNoServicesMessagePreference.setSelectable(false);
            }
            mServicesCategory.addPreference(mNoServicesMessagePreference);
        }
    }

    private void updateSystemPreferences() {
        // Large text.
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException re) {
            /* ignore */
        }
        mToggleLargeTextPreference.setChecked(mCurConfig.fontScale == LARGE_FONT_SCALE);

        // Power button ends calls.
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                && Utils.isVoiceCapable(getActivity())) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mTogglePowerButtonEndsCallPreference.setChecked(powerButtonEndsCall);
        }

        // Auto-rotate screen
        updateLockScreenRotationCheckbox();

        // Speak passwords.
        final boolean speakPasswordEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0) != 0;
        mToggleSpeakPasswordPreference.setChecked(speakPasswordEnabled);

        // Long press timeout.
        final int longPressTimeout = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, mLongPressTimeoutDefault);
        String value = String.valueOf(longPressTimeout);
        mSelectLongPressTimeoutPreference.setValue(value);
        mSelectLongPressTimeoutPreference.setSummary(mLongPressTimeoutValuetoTitleMap.get(value));

        // Screen magnification.
        final boolean magnificationEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1;
        if (magnificationEnabled) {
            mDisplayMagnificationPreferenceScreen.setSummary(
                    R.string.accessibility_feature_state_on);            
        } else {
            mDisplayMagnificationPreferenceScreen.setSummary(
                    R.string.accessibility_feature_state_off);
        }

        // Global gesture
        final boolean globalGestureEnabled = Settings.Global.getInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1;
        if (globalGestureEnabled) {
            mGlobalGesturePreferenceScreen.setSummary(
                    R.string.accessibility_global_gesture_preference_summary_on);
        } else {
            mGlobalGesturePreferenceScreen.setSummary(
                    R.string.accessibility_global_gesture_preference_summary_off);
        }
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            mToggleLockScreenRotationPreference.setChecked(
                    !RotationPolicy.isRotationLocked(context));
        }
    }

    private void offerInstallAccessibilitySerivceOnce() {
        // There is always one preference - if no services it is just a message.
        if (mServicesCategory.getPreference(0) != mNoServicesMessagePreference) {
            return;
        }
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        final boolean offerInstallService = !preferences.getBoolean(
                KEY_INSTALL_ACCESSIBILITY_SERVICE_OFFERED_ONCE, false);
        if (offerInstallService) {
            String screenreaderMarketLink = SystemProperties.get(
                    SYSTEM_PROPERTY_MARKET_URL,
                    DEFAULT_SCREENREADER_MARKET_LINK);
            Uri marketUri = Uri.parse(screenreaderMarketLink);
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);

            if (getPackageManager().resolveActivity(marketIntent, 0) == null) {
                // Don't show the dialog if no market app is found/installed.
                return;
            }

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
            case DIALOG_ID_NO_ACCESSIBILITY_SERVICES:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.accessibility_service_no_apps_title)
                        .setMessage(R.string.accessibility_service_no_apps_message)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // dismiss the dialog before launching
                                        // the activity otherwise
                                        // the dialog removal occurs after
                                        // onSaveInstanceState which
                                        // triggers an exception
                                        removeDialog(DIALOG_ID_NO_ACCESSIBILITY_SERVICES);
                                        String screenreaderMarketLink = SystemProperties.get(
                                                SYSTEM_PROPERTY_MARKET_URL,
                                                DEFAULT_SCREENREADER_MARKET_LINK);
                                        Uri marketUri = Uri.parse(screenreaderMarketLink);
                                        Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                                                marketUri);
                                        startActivity(marketIntent);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
            default:
                return null;
        }
    }

    private void loadInstalledServices() {
        Set<ComponentName> installedServices = sInstalledServices;
        installedServices.clear();

        List<AccessibilityServiceInfo> installedServiceInfos =
                AccessibilityManager.getInstance(getActivity())
                        .getInstalledAccessibilityServiceList();
        if (installedServiceInfos == null) {
            return;
        }

        final int installedServiceInfoCount = installedServiceInfos.size();
        for (int i = 0; i < installedServiceInfoCount; i++) {
            ResolveInfo resolveInfo = installedServiceInfos.get(i).getResolveInfo();
            ComponentName installedService = new ComponentName(
                    resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            installedServices.add(installedService);
        }
    }

    private static Set<ComponentName> getEnabledServicesFromSettings(Context context) {
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) {
            enabledServicesSetting = "";
        }
        Set<ComponentName> enabledServices = new HashSet<ComponentName>();
        SimpleStringSplitter colonSplitter = sStringColonSplitter;
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(
                    componentNameString);
            if (enabledService != null) {
                enabledServices.add(enabledService);
            }
        }
        return enabledServices;
    }

    private class SettingsPackageMonitor extends PackageMonitor {

        @Override
        public void onPackageAdded(String packageName, int uid) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_MILLIS);
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_MILLIS);
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_MILLIS);
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            Message message = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(message, DELAY_UPDATE_SERVICES_MILLIS);
        }
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

    public static class ToggleAccessibilityServicePreferenceFragment
            extends ToggleFeaturePreferenceFragment implements DialogInterface.OnClickListener {

        private static final int DIALOG_ID_ENABLE_WARNING = 1;
        private static final int DIALOG_ID_DISABLE_WARNING = 2;

        private final SettingsContentObserver mSettingsContentObserver =
                new SettingsContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                String settingValue = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                final boolean enabled = settingValue.contains(mComponentName.flattenToString());
                mToggleSwitch.setCheckedInternal(enabled);
            }
        };

        private ComponentName mComponentName;

        private int mShownDialogId;

        @Override
        public void onResume() {
            mSettingsContentObserver.register(getContentResolver());
            super.onResume();
        }

        @Override
        public void onPause() {
            mSettingsContentObserver.unregister(getContentResolver());
            super.onPause();
        }

        @Override
        public void onPreferenceToggled(String preferenceKey, boolean enabled) {
            // Parse the enabled services.
            Set<ComponentName> enabledServices = getEnabledServicesFromSettings(getActivity());

            // Determine enabled services and accessibility state.
            ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
            boolean accessibilityEnabled = false;
            if (enabled) {
                enabledServices.add(toggledService);
                // Enabling at least one service enables accessibility.
                accessibilityEnabled = true;
            } else {
                enabledServices.remove(toggledService);
                // Check how many enabled and installed services are present.
                Set<ComponentName> installedServices = sInstalledServices;
                for (ComponentName enabledService : enabledServices) {
                    if (installedServices.contains(enabledService)) {
                        // Disabling the last service disables accessibility.
                        accessibilityEnabled = true;
                        break;
                    }
                }
            }

            // Update the enabled services setting.
            StringBuilder enabledServicesBuilder = new StringBuilder();
            // Keep the enabled services even if they are not installed since we
            // have no way to know whether the application restore process has
            // completed. In general the system should be responsible for the
            // clean up not settings.
            for (ComponentName enabledService : enabledServices) {
                enabledServicesBuilder.append(enabledService.flattenToString());
                enabledServicesBuilder.append(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);
            }
            final int enabledServicesBuilderLength = enabledServicesBuilder.length();
            if (enabledServicesBuilderLength > 0) {
                enabledServicesBuilder.deleteCharAt(enabledServicesBuilderLength - 1);
            }
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    enabledServicesBuilder.toString());

            // Update accessibility enabled.
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, accessibilityEnabled ? 1 : 0);
        }

        // IMPORTANT: Refresh the info since there are dynamically changing capabilities. For
        // example, before JellyBean MR2 the user was granting the explore by touch one.
        private AccessibilityServiceInfo getAccessibilityServiceInfo() {
            List<AccessibilityServiceInfo> serviceInfos = AccessibilityManager.getInstance(
                    getActivity()).getInstalledAccessibilityServiceList();
            final int serviceInfoCount = serviceInfos.size();
            for (int i = 0; i < serviceInfoCount; i++) {
                AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
                ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
                if (mComponentName.getPackageName().equals(resolveInfo.serviceInfo.packageName)
                        && mComponentName.getClassName().equals(resolveInfo.serviceInfo.name)) {
                    return serviceInfo;
                }
            }
            return null;
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            switch (dialogId) {
                case DIALOG_ID_ENABLE_WARNING: {
                    mShownDialogId = DIALOG_ID_ENABLE_WARNING;
                    AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                    if (info == null) {
                        return null;
                    }
                    return new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.enable_service_title,
                                info.getResolveInfo().loadLabel(getPackageManager())))
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setView(createEnableDialogContentView(info))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();
                }
                case DIALOG_ID_DISABLE_WARNING: {
                    mShownDialogId = DIALOG_ID_DISABLE_WARNING;
                    AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                    if (info == null) {
                        return null;
                    }
                    return new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.disable_service_title,
                                info.getResolveInfo().loadLabel(getPackageManager())))
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(getString(R.string.disable_service_message,
                                info.getResolveInfo().loadLabel(getPackageManager())))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
        }

        private View createEnableDialogContentView(AccessibilityServiceInfo info) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content,
                    null);

            TextView capabilitiesHeaderView = (TextView) content.findViewById(
                    R.id.capabilities_header);
            capabilitiesHeaderView.setText(getString(R.string.capabilities_list_title,
                    info.getResolveInfo().loadLabel(getPackageManager())));

            LinearLayout capabilitiesView = (LinearLayout) content.findViewById(R.id.capabilities);

            // This capability is implicit for all services.
            View capabilityView = inflater.inflate(
                    com.android.internal.R.layout.app_permission_item_old, null);

            ImageView imageView = (ImageView) capabilityView.findViewById(
                    com.android.internal.R.id.perm_icon);
            imageView.setImageDrawable(getResources().getDrawable(
                    com.android.internal.R.drawable.ic_text_dot));

            TextView labelView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_group);
            labelView.setText(getString(R.string.capability_title_receiveAccessibilityEvents));

            TextView descriptionView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_list);
            descriptionView.setText(getString(R.string.capability_desc_receiveAccessibilityEvents));

            List<AccessibilityServiceInfo.CapabilityInfo> capabilities =
                    info.getCapabilityInfos();

            capabilitiesView.addView(capabilityView);

            // Service specific capabilities.
            final int capabilityCount = capabilities.size();
            for (int i = 0; i < capabilityCount; i++) {
                AccessibilityServiceInfo.CapabilityInfo capability = capabilities.get(i);

                capabilityView = inflater.inflate(
                        com.android.internal.R.layout.app_permission_item_old, null);

                imageView = (ImageView) capabilityView.findViewById(
                        com.android.internal.R.id.perm_icon);
                imageView.setImageDrawable(getResources().getDrawable(
                        com.android.internal.R.drawable.ic_text_dot));

                labelView = (TextView) capabilityView.findViewById(
                        com.android.internal.R.id.permission_group);
                labelView.setText(getString(capability.titleResId));

                descriptionView = (TextView) capabilityView.findViewById(
                        com.android.internal.R.id.permission_list);
                descriptionView.setText(getString(capability.descResId));

                capabilitiesView.addView(capabilityView);
            }

            return content;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final boolean checked;
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    checked = (mShownDialogId == DIALOG_ID_ENABLE_WARNING);
                    mToggleSwitch.setCheckedInternal(checked);
                    getArguments().putBoolean(EXTRA_CHECKED, checked);
                    onPreferenceToggled(mPreferenceKey, checked);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    checked = (mShownDialogId == DIALOG_ID_DISABLE_WARNING);
                    mToggleSwitch.setCheckedInternal(checked);
                    getArguments().putBoolean(EXTRA_CHECKED, checked);
                    onPreferenceToggled(mPreferenceKey, checked);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        protected void onInstallActionBarToggleSwitch() {
            super.onInstallActionBarToggleSwitch();
            mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
                public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                    if (checked) {
                        toggleSwitch.setCheckedInternal(false);
                        getArguments().putBoolean(EXTRA_CHECKED, false);
                        showDialog(DIALOG_ID_ENABLE_WARNING);
                    } else {
                        toggleSwitch.setCheckedInternal(true);
                        getArguments().putBoolean(EXTRA_CHECKED, true);
                        showDialog(DIALOG_ID_DISABLE_WARNING);
                    }
                    return true;
                }
            });
        }

        @Override
        protected void onProcessArguments(Bundle arguments) {
            super.onProcessArguments(arguments);
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

            mComponentName = arguments.getParcelable(EXTRA_COMPONENT_NAME);
        }
    }

    public static class ToggleScreenMagnificationPreferenceFragment
            extends ToggleFeaturePreferenceFragment {
        @Override
        protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, enabled? 1 : 0);
        }

        @Override
        protected void onInstallActionBarToggleSwitch() {
            super.onInstallActionBarToggleSwitch();
            mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
                public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                    toggleSwitch.setCheckedInternal(checked);
                    getArguments().putBoolean(EXTRA_CHECKED, checked);
                    onPreferenceToggled(mPreferenceKey, checked);
                    return false;
                }
            });
        }
    }

    public static class ToggleGlobalGesturePreferenceFragment
            extends ToggleFeaturePreferenceFragment {
        @Override
        protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, enabled ? 1 : 0);
        }

        @Override
        protected void onInstallActionBarToggleSwitch() {
            super.onInstallActionBarToggleSwitch();
            mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
                public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                    toggleSwitch.setCheckedInternal(checked);
                    getArguments().putBoolean(EXTRA_CHECKED, checked);
                    onPreferenceToggled(mPreferenceKey, checked);
                    return false;
                }
            });
        }
    }

    public static abstract class ToggleFeaturePreferenceFragment
            extends SettingsPreferenceFragment {

        protected ToggleSwitch mToggleSwitch;

        protected String mPreferenceKey;
        protected Preference mSummaryPreference;

        protected CharSequence mSettingsTitle;
        protected Intent mSettingsIntent;

        // TODO: Showing sub-sub fragment does not handle the activity title
        // so we do it but this is wrong. Do a real fix when there is time.
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
            onInstallActionBarToggleSwitch();
            onProcessArguments(getArguments());
            // Set a transparent drawable to prevent use of the default one.
            getListView().setSelector(new ColorDrawable(Color.TRANSPARENT));
            getListView().setDivider(null);
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

        protected abstract void onPreferenceToggled(String preferenceKey, boolean enabled);

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            MenuItem menuItem = menu.add(mSettingsTitle);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menuItem.setIntent(mSettingsIntent);
        }

        protected void onInstallActionBarToggleSwitch() {
            mToggleSwitch = createAndAddActionBarToggleSwitch(getActivity());
        }

        private ToggleSwitch createAndAddActionBarToggleSwitch(Activity activity) {
            ToggleSwitch toggleSwitch = new ToggleSwitch(activity);
            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            toggleSwitch.setPaddingRelative(0, 0, padding, 0);
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(toggleSwitch,
                    new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL | Gravity.END));
            return toggleSwitch;
        }

        protected void onProcessArguments(Bundle arguments) {
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
                getActivity().getActionBar().setTitle(title);
            }
            // Summary.
            CharSequence summary = arguments.getCharSequence(EXTRA_SUMMARY);
            mSummaryPreference.setSummary(summary);
        }
    }

    private static abstract class SettingsContentObserver extends ContentObserver {

        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.ACCESSIBILITY_ENABLED), false, this);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, this);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override
        public abstract void onChange(boolean selfChange, Uri uri);
    }
}
