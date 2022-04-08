/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;
import java.util.Set;

public class AccessibilityDetailsSettingsFragment extends InstrumentedFragment {

    private final static String TAG = "A11yDetailsSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_DETAILS_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case the Intent doesn't have component name, go to a11y services list.
        final String extraComponentName = getActivity().getIntent().getStringExtra(
                Intent.EXTRA_COMPONENT_NAME);
        if (extraComponentName == null) {
            Log.w(TAG, "Open accessibility services list due to no component name.");
            openAccessibilitySettingsAndFinish();
            return;
        }

        // In case the A11yServiceInfo doesn't exist, go to ally services list.
        final ComponentName componentName = ComponentName.unflattenFromString(extraComponentName);
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo(componentName);
        if (info == null) {
            Log.w(TAG, "Open accessibility services list due to invalid component name.");
            openAccessibilitySettingsAndFinish();
            return;
        }

        // In case this accessibility service isn't permitted, go to a11y services list.
        if (!isServiceAllowed(componentName.getPackageName())) {
            Log.w(TAG,
                    "Open accessibility services list due to target accessibility service is "
                            + "prohibited by Device Admin.");
            openAccessibilitySettingsAndFinish();
            return;
        }

        openAccessibilityDetailsSettingsAndFinish(buildArguments(info));
    }

    @VisibleForTesting
    void openAccessibilitySettingsAndFinish() {
        new SubSettingLauncher(getActivity())
                .setDestination(AccessibilitySettings.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
        finish();
    }

    @VisibleForTesting
    void openAccessibilityDetailsSettingsAndFinish(Bundle arguments) {
        new SubSettingLauncher(getActivity())
                .setDestination(ToggleAccessibilityServicePreferenceFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setArguments(arguments)
                .launch();
        finish();
    }

    @VisibleForTesting
    boolean isServiceAllowed(String packageName) {
        final DevicePolicyManager dpm = getContext().getSystemService(DevicePolicyManager.class);
        final List<String> permittedServices = dpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        return (permittedServices == null || permittedServices.contains(packageName));
    }

    private AccessibilityServiceInfo getAccessibilityServiceInfo(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }

        final List<AccessibilityServiceInfo> serviceInfos = AccessibilityManager.getInstance(
                getActivity()).getInstalledAccessibilityServiceList();
        final int serviceInfoCount = serviceInfos.size();
        for (int i = 0; i < serviceInfoCount; i++) {
            AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
            ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
            if (componentName.getPackageName().equals(resolveInfo.serviceInfo.packageName)
                    && componentName.getClassName().equals(resolveInfo.serviceInfo.name)) {
                return serviceInfo;
            }
        }
        return null;
    }

    private Bundle buildArguments(AccessibilityServiceInfo info) {
        final ResolveInfo resolveInfo = info.getResolveInfo();
        final String title = resolveInfo.loadLabel(getActivity().getPackageManager()).toString();
        final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        final String packageName = serviceInfo.packageName;
        final ComponentName componentName = new ComponentName(packageName, serviceInfo.name);

        final Set<ComponentName> enabledServices =
                AccessibilityUtils.getEnabledServicesFromSettings(getActivity());
        final boolean serviceEnabled = enabledServices.contains(componentName);
        String description = info.loadDescription(getActivity().getPackageManager());

        if (serviceEnabled && info.crashed) {
            // Update the summaries for services that have crashed.
            description = getString(R.string.accessibility_description_state_stopped);
        }

        final Bundle extras = new Bundle();
        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                componentName.flattenToString());
        extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED, serviceEnabled);
        extras.putString(AccessibilitySettings.EXTRA_TITLE, title);
        extras.putParcelable(AccessibilitySettings.EXTRA_RESOLVE_INFO, resolveInfo);
        extras.putString(AccessibilitySettings.EXTRA_SUMMARY, description);

        final String settingsClassName = info.getSettingsActivityName();
        if (!TextUtils.isEmpty(settingsClassName)) {
            extras.putString(AccessibilitySettings.EXTRA_SETTINGS_TITLE,
                    getString(R.string.accessibility_menu_item_settings));
            extras.putString(AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME,
                    new ComponentName(packageName, settingsClassName).flattenToString());
        }
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);
        extras.putInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES, info.getAnimatedImageRes());

        final String htmlDescription = info.loadHtmlDescription(getActivity().getPackageManager());
        extras.putString(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, htmlDescription);
        return extras;
    }

    private void finish() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.finish();
    }
}
