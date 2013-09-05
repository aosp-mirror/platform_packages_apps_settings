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

package com.android.settings.print;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

/**
 * Fragment with the top level print settings.
 */
public class PrintSettingsFragment extends SettingsPreferenceFragment implements DialogCreatable {

    static final char ENABLED_PRINT_SERVICES_SEPARATOR = ':';

    // Extras passed to sub-fragments.
    static final String EXTRA_PREFERENCE_KEY = "preference_key";
    static final String EXTRA_CHECKED = "checked";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_ENABLE_WARNING_TITLE = "enable_warning_title";
    static final String EXTRA_ENABLE_WARNING_MESSAGE = "enable_warning_message";
    static final String EXTRA_SETTINGS_TITLE = "settings_title";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";
    static final String EXTRA_ADD_PRINTERS_TITLE = "add_printers_title";
    static final String EXTRA_ADD_PRINTERS_COMPONENT_NAME = "add_printers_component_name";
    static final String EXTRA_SERVICE_COMPONENT_NAME = "service_component_name";

    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            updateServicesPreferences();
        }
    };

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateServicesPreferences();
        }
    };

    private Preference mNoServicesMessagePreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.print_settings);
        getActivity().getActionBar().setTitle(R.string.print_settings_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        updateServicesPreferences();
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.print_settings, menu);
        MenuItem menuItem = menu.findItem(R.id.print_menu_item_add_service);
        menuItem.setIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.download_print_service_query))));
    }

    private void updateServicesPreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go.
        getPreferenceScreen().removeAll();

        List<ComponentName> enabledServices = SettingsUtils
                .readEnabledPrintServices(getActivity());

        List<ResolveInfo> installedServices = getActivity().getPackageManager()
                .queryIntentServices(
                        new Intent(android.printservice.PrintService.SERVICE_INTERFACE),
                        PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);

        final int installedServiceCount = installedServices.size();
        for (int i = 0; i < installedServiceCount; i++) {
            ResolveInfo installedService = installedServices.get(i);

            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                    getActivity());

            String title = installedService.loadLabel(getPackageManager()).toString();
            preference.setTitle(title);

            ComponentName componentName = new ComponentName(
                    installedService.serviceInfo.packageName,
                    installedService.serviceInfo.name);
            preference.setKey(componentName.flattenToString());

            preference.setOrder(i);
            preference.setFragment(PrintServiceSettingsFragment.class.getName());
            preference.setPersistent(false);

            final boolean serviceEnabled = enabledServices.contains(componentName);
            if (serviceEnabled) {
                preference.setSummary(getString(R.string.print_feature_state_on));
            } else {
                preference.setSummary(getString(R.string.print_feature_state_off));
            }

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

            getPreferenceScreen().addPreference(preference);
        }

        if (getPreferenceScreen().getPreferenceCount() == 0) {
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
            getPreferenceScreen().addPreference(mNoServicesMessagePreference);
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
