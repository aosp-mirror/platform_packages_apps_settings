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

package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.settings.PrintingSettings.ToggleSwitch.OnBeforeCheckedChangeListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity with the printing settings.
 */
public class PrintingSettings extends SettingsPreferenceFragment implements DialogCreatable {

    private static final char ENABLED_PRINT_SERVICES_SEPARATOR = ':';

    // Preference categories
    private static final String SERVICES_CATEGORY = "services_category";

    // Extras passed to sub-fragments.
    private static final String EXTRA_PREFERENCE_KEY = "preference_key";
    private static final String EXTRA_CHECKED = "checked";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_ENABLE_WARNING_TITLE = "enable_warning_title";
    private static final String EXTRA_ENABLE_WARNING_MESSAGE = "enable_warning_message";
    private static final String EXTRA_SETTINGS_TITLE = "settings_title";
    private static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";
    private static final String EXTRA_ADD_PRINTERS_TITLE = "add_printers_title";
    private static final String EXTRA_ADD_PRINTERS_COMPONENT_NAME = "add_printers_component_name";
    private static final String EXTRA_SERVICE_COMPONENT_NAME = "service_component_name";

    // Auxiliary members.
    private final static SimpleStringSplitter sStringColonSplitter =
            new SimpleStringSplitter(ENABLED_PRINT_SERVICES_SEPARATOR);

    private static final List<ResolveInfo> sInstalledServicesList = new ArrayList<ResolveInfo>();

    private static final Set<ComponentName> sEnabledServiceNameSet = new HashSet<ComponentName>();

    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            loadInstalledServices(getActivity());
            loadEnabledServices(getActivity());
            updateServicesPreferences();
        }
    };

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            loadInstalledServices(getActivity());
            loadEnabledServices(getActivity());
            updateServicesPreferences();
        }
    };

    // Preference controls.
    private PreferenceCategory mServicesCategory;

    private Preference mNoServicesMessagePreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.print_settings);
        mServicesCategory = (PreferenceCategory) findPreference(SERVICES_CATEGORY);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        loadInstalledServices(getActivity());
        loadEnabledServices(getActivity());
        updateServicesPreferences();
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    private void updateServicesPreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go.
        mServicesCategory.removeAll();

       final int installedServiceCount = sInstalledServicesList.size();
        for (int i = 0; i < installedServiceCount; i++) {
            ResolveInfo installedService = sInstalledServicesList.get(i);

            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                    getActivity());

            String title = installedService.loadLabel(getPackageManager()).toString();
            preference.setTitle(title);

            ComponentName componentName = new ComponentName(
                    installedService.serviceInfo.packageName,
                    installedService.serviceInfo.name);
            preference.setKey(componentName.flattenToString());

            final boolean serviceEnabled = sEnabledServiceNameSet.contains(componentName);
            if (serviceEnabled) {
                preference.setSummary(getString(R.string.print_feature_state_on));
            } else {
                preference.setSummary(getString(R.string.print_feature_state_off));
            }

            preference.setOrder(i);
            preference.setFragment(TogglePrintServicePreferenceFragment.class.getName());
            preference.setPersistent(false);

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);

            PrintServiceInfo printServiceInfo = PrintServiceInfo.create(
                    installedService, getActivity());

            CharSequence applicationLabel = installedService.loadLabel(getPackageManager());

            extras.putString(EXTRA_ENABLE_WARNING_TITLE, getString(
                    R.string.print_service_security_warning_title, applicationLabel));
            extras.putString(EXTRA_ENABLE_WARNING_MESSAGE, getString(
                    R.string.print_service_security_warning_summary, applicationLabel));

            String settingsClassName = printServiceInfo.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.print_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(installedService.serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            String addPrinterClassName = printServiceInfo.getAddPrintersActivityName();
            if (!TextUtils.isEmpty(addPrinterClassName)) {
                extras.putString(EXTRA_ADD_PRINTERS_TITLE,
                        getString(R.string.print_menu_item_add_printers));
                extras.putString(EXTRA_ADD_PRINTERS_COMPONENT_NAME,
                        new ComponentName(installedService.serviceInfo.packageName,
                                addPrinterClassName).flattenToString());
            }

            extras.putString(EXTRA_SERVICE_COMPONENT_NAME, componentName.flattenToString());

            mServicesCategory.addPreference(preference);
        }

        if (mServicesCategory.getPreferenceCount() == 0) {
            if (mNoServicesMessagePreference == null) {
                mNoServicesMessagePreference = new Preference(getActivity()) {
                    @Override
                    protected void onBindView(View view) {
                        super.onBindView(view);
                        TextView summaryView = (TextView) view.findViewById(R.id.summary);
                        String title = getString(R.string.print_no_services_installed);
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

    private static void loadInstalledServices(Context context) {
        sInstalledServicesList.clear();
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentServices(
                    new Intent(android.printservice.PrintService.SERVICE_INTERFACE),
                    PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        final int resolveInfoCount = resolveInfos.size();
        for (int i = 0, count = resolveInfoCount; i < count; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            sInstalledServicesList.add(resolveInfo);
        }
    }

    private static void loadEnabledServices(Context context) {
        sEnabledServiceNameSet.clear();

        String enabledServicesSetting = Settings.Secure.getString(context
                .getContentResolver(), Settings.Secure.ENABLED_PRINT_SERVICES);
        if (enabledServicesSetting == null) {
            enabledServicesSetting = "";
        }

        SimpleStringSplitter colonSplitter = sStringColonSplitter;
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(
                    componentNameString);
            sEnabledServiceNameSet.add(enabledService);
        }
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        @Override
        public void onPackageAdded(String packageName, int uid) {
           mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
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

    public static class TogglePrintServicePreferenceFragment
            extends ToggleFeaturePreferenceFragment implements DialogInterface.OnClickListener {

        private static final int DIALOG_ID_ENABLE_WARNING = 1;

        private final SettingsContentObserver mSettingsContentObserver =
                new SettingsContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                String settingValue = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ENABLED_PRINT_SERVICES);
                final boolean enabled = settingValue.contains(mComponentName);
                mToggleSwitch.setCheckedInternal(enabled);
            }
        };

        private CharSequence mEnableWarningTitle;
        private CharSequence mEnableWarningMessage;

        private String mComponentName;

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
            Set<ComponentName> enabledServices = sEnabledServiceNameSet;
            ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
            if (enabled) {
                enabledServices.add(toggledService);
            } else {
                enabledServices.remove(toggledService);
            }
            StringBuilder enabledServicesBuilder = new StringBuilder();
            for (ComponentName enabledService : enabledServices) {
                enabledServicesBuilder.append(enabledService.flattenToString());
                enabledServicesBuilder.append(ENABLED_PRINT_SERVICES_SEPARATOR);
            }
            final int enabledServicesBuilderLength = enabledServicesBuilder.length();
            if (enabledServicesBuilderLength > 0) {
                enabledServicesBuilder.deleteCharAt(enabledServicesBuilderLength - 1);
            }
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_PRINT_SERVICES,
                    enabledServicesBuilder.toString());
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            CharSequence title = null;
            CharSequence message = null;
            switch (dialogId) {
                case DIALOG_ID_ENABLE_WARNING:
                    title = mEnableWarningTitle;
                    message = mEnableWarningMessage;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final boolean checked;
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    checked = true;
                    mToggleSwitch.setCheckedInternal(checked);
                    getArguments().putBoolean(EXTRA_CHECKED, checked);
                    onPreferenceToggled(mPreferenceKey, checked);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    checked = false;
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
                        if (!TextUtils.isEmpty(mEnableWarningMessage)) {
                            toggleSwitch.setCheckedInternal(false);
                            getArguments().putBoolean(EXTRA_CHECKED, false);
                            showDialog(DIALOG_ID_ENABLE_WARNING);
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
            // Add printers title and intent.
            String addPrintersTitle = arguments.getString(EXTRA_ADD_PRINTERS_TITLE);
            String addPrintersComponentName =
                    arguments.getString(EXTRA_ADD_PRINTERS_COMPONENT_NAME);
            if (!TextUtils.isEmpty(addPrintersTitle)
                    && !TextUtils.isEmpty(addPrintersComponentName)) {
                Intent addPritnersIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                        ComponentName.unflattenFromString(addPrintersComponentName.toString()));
                if (!getPackageManager().queryIntentActivities(addPritnersIntent, 0).isEmpty()) {
                    mAddPrintersTitle = addPrintersTitle;
                    mAddPrintersIntent = addPritnersIntent;
                    setHasOptionsMenu(true);
                }
            }
            // Enable warning title.
            mEnableWarningTitle = arguments.getCharSequence(
                    PrintingSettings.EXTRA_ENABLE_WARNING_TITLE);
            // Enable warning message.
            mEnableWarningMessage = arguments.getCharSequence(
                    PrintingSettings.EXTRA_ENABLE_WARNING_MESSAGE);
            // Component name.
            mComponentName = arguments.getString(EXTRA_SERVICE_COMPONENT_NAME);
        }
    }

    public static abstract class ToggleFeaturePreferenceFragment
            extends SettingsPreferenceFragment {

        protected ToggleSwitch mToggleSwitch;

        protected String mPreferenceKey;

        protected CharSequence mSettingsTitle;
        protected Intent mSettingsIntent;

        protected CharSequence mAddPrintersTitle;
        protected Intent mAddPrintersIntent;

        // TODO: Showing sub-sub fragment does not handle the activity title
        // so we do it but this is wrong. Do a real fix when there is time.
        private CharSequence mOldActivityTitle;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            setPreferenceScreen(preferenceScreen);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            onInstallActionBarToggleSwitch();
            onProcessArguments(getArguments());
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

        protected abstract void onPreferenceToggled(String preferenceKey, boolean enabled);

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            if (!TextUtils.isEmpty(mSettingsTitle) && mSettingsIntent != null) {
                MenuItem menuItem = menu.add(mSettingsTitle);
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                menuItem.setIntent(mSettingsIntent);
            }
            if (!TextUtils.isEmpty(mAddPrintersTitle) && mAddPrintersIntent != null) {
                MenuItem menuItem = menu.add(mAddPrintersTitle);
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                menuItem.setIntent(mAddPrintersIntent);
            }
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
        }
    }

    private static abstract class SettingsContentObserver extends ContentObserver {

        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.ENABLED_PRINT_SERVICES), false, this);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override
        public abstract void onChange(boolean selfChange, Uri uri);
    }
}
