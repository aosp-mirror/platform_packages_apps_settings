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

import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_BUTTON_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AppOpsManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AccessibilityDetailsSettingsFragment extends InstrumentedFragment {

    private final static String TAG = "A11yDetailsSettings";
    private AppOpsManager mAppOps;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_DETAILS_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppOps = getActivity().getSystemService(AppOpsManager.class);

        // In case the Intent doesn't have component name, go to a11y services list.
        final String extraComponentName = getActivity().getIntent().getStringExtra(
                Intent.EXTRA_COMPONENT_NAME);
        if (extraComponentName == null) {
            Log.w(TAG, "Open accessibility services list due to no component name.");
            openAccessibilitySettingsAndFinish();
            return;
        }

        final ComponentName componentName = ComponentName.unflattenFromString(extraComponentName);
        if (openSystemAccessibilitySettingsAndFinish(componentName)) {
            return;
        }

        if (openAccessibilityDetailsSettingsAndFinish(componentName)) {
            return;
        }
        // Fall back to open accessibility services list.
        openAccessibilitySettingsAndFinish();
    }

    private boolean openSystemAccessibilitySettingsAndFinish(
            @Nullable ComponentName componentName) {
        final LaunchFragmentArguments launchArguments =
                getSystemAccessibilitySettingsLaunchArguments(componentName);
        if (launchArguments == null) {
            return false;
        }
        openSubSettings(launchArguments.mDestination, launchArguments.mArguments);
        finish();
        return true;
    }

    @Nullable
    private LaunchFragmentArguments getSystemAccessibilitySettingsLaunchArguments(
            @Nullable ComponentName componentName) {
        if (MAGNIFICATION_COMPONENT_NAME.equals(componentName)) {
            final String destination = ToggleScreenMagnificationPreferenceFragment.class.getName();
            return new LaunchFragmentArguments(destination, /* arguments= */ null);
        }

        if (ACCESSIBILITY_BUTTON_COMPONENT_NAME.equals(componentName)) {
            final String destination = AccessibilityButtonFragment.class.getName();
            return new LaunchFragmentArguments(destination, /* arguments= */ null);
        }

        if (ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME.equals(componentName)) {
            final String destination = AccessibilityHearingAidsFragment.class.getName();
            return new LaunchFragmentArguments(destination, /* arguments= */ null);
        }

        return null;
    }


    private void openAccessibilitySettingsAndFinish() {
        openSubSettings(AccessibilitySettings.class.getName(), /* arguments= */ null);
        finish();
    }

    private boolean openAccessibilityDetailsSettingsAndFinish(
            @Nullable ComponentName componentName) {
        // In case the A11yServiceInfo doesn't exist, go to ally services list.
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo(componentName);
        if (info == null) {
            Log.w(TAG, "openAccessibilityDetailsSettingsAndFinish : invalid component name.");
            return false;
        }

        // In case this accessibility service isn't permitted, go to a11y services list.
        if (!isServiceAllowed(info.getResolveInfo().serviceInfo.applicationInfo.uid,
                componentName.getPackageName())) {
            Log.w(TAG,
                    "openAccessibilityDetailsSettingsAndFinish: target accessibility service is"
                            + "prohibited by Device Admin or App Op.");
            return false;
        }
        openSubSettings(ToggleAccessibilityServicePreferenceFragment.class.getName(),
                buildArguments(info));
        finish();
        return true;
    }

    private void openSubSettings(@NonNull String destination, @Nullable Bundle arguments) {
        new SubSettingLauncher(getActivity())
                .setDestination(destination)
                .setSourceMetricsCategory(getMetricsCategory())
                .setArguments(arguments)
                .launch();
    }

    @VisibleForTesting
    boolean isServiceAllowed(int uid, String packageName) {
        final DevicePolicyManager dpm = getContext().getSystemService(DevicePolicyManager.class);
        final List<String> permittedServices = dpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        if (permittedServices != null && !permittedServices.contains(packageName)) {
            return false;
        }

        return !RestrictedLockUtilsInternal.isEnhancedConfirmationRestricted(getContext(),
                packageName, AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE);
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

        final String tileServiceClassName = info.getTileServiceName();
        if (!TextUtils.isEmpty(tileServiceClassName)) {
            extras.putString(AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME,
                    new ComponentName(packageName, tileServiceClassName).flattenToString());
        }

        final int metricsCategory = FeatureFactory.getFeatureFactory()
                .getAccessibilityMetricsFeatureProvider()
                .getDownloadedFeatureMetricsCategory(componentName);
        extras.putInt(AccessibilitySettings.EXTRA_METRICS_CATEGORY, metricsCategory);
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);
        extras.putInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES, info.getAnimatedImageRes());

        final String htmlDescription = info.loadHtmlDescription(getActivity().getPackageManager());
        extras.putString(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, htmlDescription);

        final CharSequence intro = info.loadIntro(getActivity().getPackageManager());
        extras.putCharSequence(AccessibilitySettings.EXTRA_INTRO, intro);

        // We will log nonA11yTool status from PolicyWarningUIController; others none.
        extras.putLong(AccessibilitySettings.EXTRA_TIME_FOR_LOGGING,
                getActivity().getIntent().getLongExtra(
                        AccessibilitySettings.EXTRA_TIME_FOR_LOGGING, 0));
        return extras;
    }

    private void finish() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.finish();
    }

    private static class LaunchFragmentArguments {
        final String mDestination;
        final Bundle mArguments;
        LaunchFragmentArguments(@NonNull String destination, @Nullable Bundle arguments) {
            mDestination = Objects.requireNonNull(destination);
            mArguments = arguments;
        }
    }
}
