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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.accessibility.ToggleSwitch.OnBeforeCheckedChangeListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ToggleAccessibilityServicePreferenceFragment
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
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(
                getActivity());

        if (enabledServices == (Set<?>) Collections.emptySet()) {
            enabledServices = new HashSet<ComponentName>();
        }

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
            Set<ComponentName> installedServices = AccessibilitySettings.sInstalledServices;
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
            enabledServicesBuilder.append(
                    AccessibilitySettings.ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);
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

    // IMPORTANT: Refresh the info since there are dynamically changing
    // capabilities. For
    // example, before JellyBean MR2 the user was granting the explore by touch
    // one.
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
                getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                checked = (mShownDialogId == DIALOG_ID_DISABLE_WARNING);
                mToggleSwitch.setCheckedInternal(checked);
                getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, checked);
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
                    getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, false);
                    showDialog(DIALOG_ID_ENABLE_WARNING);
                } else {
                    toggleSwitch.setCheckedInternal(true);
                    getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, true);
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
        String settingsTitle = arguments.getString(AccessibilitySettings.EXTRA_SETTINGS_TITLE);
        String settingsComponentName = arguments.getString(
                AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(settingsComponentName.toString()));
            if (!getPackageManager().queryIntentActivities(settingsIntent, 0).isEmpty()) {
                mSettingsTitle = settingsTitle;
                mSettingsIntent = settingsIntent;
                setHasOptionsMenu(true);
            }
        }

        mComponentName = arguments.getParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME);
    }
}
