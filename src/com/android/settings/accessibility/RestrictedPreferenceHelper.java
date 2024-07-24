/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settingslib.widget.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class helps setup RestrictedPreference for accessibility.
 */
public class RestrictedPreferenceHelper {
    // Index of the first preference in a preference category.
    private static final int FIRST_PREFERENCE_IN_CATEGORY_INDEX = -1;

    private final Context mContext;
    private final DevicePolicyManager mDpm;
    private final PackageManager mPm;
    private final AppOpsManager mAppOps;

    public RestrictedPreferenceHelper(Context context) {
        mContext = context;
        mDpm = context.getSystemService(DevicePolicyManager.class);
        mPm = context.getPackageManager();
        mAppOps = context.getSystemService(AppOpsManager.class);
    }

    /**
     * Creates the list of {@link RestrictedPreference} with the installedServices arguments.
     *
     * @param installedServices The list of {@link AccessibilityServiceInfo}s of the
     *                          installed accessibility services
     * @return The list of {@link RestrictedPreference}
     */
    public List<RestrictedPreference> createAccessibilityServicePreferenceList(
            List<AccessibilityServiceInfo> installedServices) {

        final Set<ComponentName> enabledServices =
                AccessibilityUtils.getEnabledServicesFromSettings(mContext);
        final List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final int installedServicesSize = installedServices.size();

        final List<RestrictedPreference> preferenceList = new ArrayList<>(
                installedServicesSize);

        for (int i = 0; i < installedServicesSize; ++i) {
            final AccessibilityServiceInfo info = installedServices.get(i);
            final ResolveInfo resolveInfo = info.getResolveInfo();
            final String packageName = resolveInfo.serviceInfo.packageName;
            final ComponentName componentName = new ComponentName(packageName,
                    resolveInfo.serviceInfo.name);

            final String key = componentName.flattenToString();
            final CharSequence title = resolveInfo.loadLabel(mPm);
            final boolean serviceEnabled = enabledServices.contains(componentName);
            final CharSequence summary = AccessibilitySettings.getServiceSummary(
                    mContext, info, serviceEnabled);
            final String fragment = getAccessibilityServiceFragmentTypeName(info);

            Drawable icon = resolveInfo.loadIcon(mPm);
            if (resolveInfo.getIconResource() == 0) {
                icon = ContextCompat.getDrawable(mContext,
                        R.drawable.ic_accessibility_generic);
            }

            final RestrictedPreference preference = createRestrictedPreference(key, title,
                    summary, icon, fragment, packageName,
                    resolveInfo.serviceInfo.applicationInfo.uid);

            setRestrictedPreferenceEnabled(preference, permittedServices, serviceEnabled);

            final String prefKey = preference.getKey();
            final int imageRes = info.getAnimatedImageRes();
            final CharSequence intro = info.loadIntro(mPm);
            final CharSequence description = AccessibilitySettings.getServiceDescription(
                    mContext, info, serviceEnabled);
            final String htmlDescription = info.loadHtmlDescription(mPm);
            final String settingsClassName = info.getSettingsActivityName();
            final String tileServiceClassName = info.getTileServiceName();
            final int metricsCategory = FeatureFactory.getFeatureFactory()
                    .getAccessibilityMetricsFeatureProvider()
                    .getDownloadedFeatureMetricsCategory(componentName);

            putBasicExtras(preference, prefKey, title, intro, description, imageRes,
                    htmlDescription, componentName, metricsCategory);
            putServiceExtras(preference, resolveInfo, serviceEnabled);
            putSettingsExtras(preference, packageName, settingsClassName);
            putTileServiceExtras(preference, packageName, tileServiceClassName);

            preferenceList.add(preference);
        }
        return preferenceList;
    }

    /**
     * Creates the list of {@link RestrictedPreference} with the installedShortcuts arguments.
     *
     * @param installedShortcuts The list of {@link AccessibilityShortcutInfo}s of the
     *                           installed accessibility shortcuts
     * @return The list of {@link RestrictedPreference}
     */
    public List<RestrictedPreference> createAccessibilityActivityPreferenceList(
            List<AccessibilityShortcutInfo> installedShortcuts) {
        final Set<ComponentName> enabledServices =
                AccessibilityUtils.getEnabledServicesFromSettings(mContext);
        final List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());

        final int installedShortcutsSize = installedShortcuts.size();
        final List<RestrictedPreference> preferenceList = new ArrayList<>(
                installedShortcutsSize);

        for (int i = 0; i < installedShortcutsSize; ++i) {
            final AccessibilityShortcutInfo info = installedShortcuts.get(i);
            final ActivityInfo activityInfo = info.getActivityInfo();
            final ComponentName componentName = info.getComponentName();

            final String key = componentName.flattenToString();
            final CharSequence title = activityInfo.loadLabel(mPm);
            final String summary = info.loadSummary(mPm);
            final String fragment =
                    LaunchAccessibilityActivityPreferenceFragment.class.getName();

            Drawable icon = activityInfo.loadIcon(mPm);
            if (activityInfo.getIconResource() == 0) {
                icon = ContextCompat.getDrawable(mContext, R.drawable.ic_accessibility_generic);
            }

            final RestrictedPreference preference = createRestrictedPreference(key, title,
                    summary, icon, fragment, componentName.getPackageName(),
                    activityInfo.applicationInfo.uid);
            final boolean serviceEnabled = enabledServices.contains(componentName);

            setRestrictedPreferenceEnabled(preference, permittedServices, serviceEnabled);

            final String prefKey = preference.getKey();
            final CharSequence intro = info.loadIntro(mPm);
            final String description = info.loadDescription(mPm);
            final int imageRes = info.getAnimatedImageRes();
            final String htmlDescription = info.loadHtmlDescription(mPm);
            final String settingsClassName = info.getSettingsActivityName();
            final String tileServiceClassName = info.getTileServiceName();
            final int metricsCategory = FeatureFactory.getFeatureFactory()
                    .getAccessibilityMetricsFeatureProvider()
                    .getDownloadedFeatureMetricsCategory(componentName);

            putBasicExtras(preference, prefKey, title, intro, description, imageRes,
                    htmlDescription, componentName, metricsCategory);
            putSettingsExtras(preference, componentName.getPackageName(), settingsClassName);
            putTileServiceExtras(preference, componentName.getPackageName(),
                    tileServiceClassName);

            preferenceList.add(preference);
        }
        return preferenceList;
    }

    private String getAccessibilityServiceFragmentTypeName(AccessibilityServiceInfo info) {
        final int type = AccessibilityUtil.getAccessibilityServiceFragmentType(info);
        switch (type) {
            case AccessibilityUtil.AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE:
                return VolumeShortcutToggleAccessibilityServicePreferenceFragment.class.getName();
            case AccessibilityUtil.AccessibilityServiceFragmentType.INVISIBLE_TOGGLE:
                return InvisibleToggleAccessibilityServicePreferenceFragment.class.getName();
            case AccessibilityUtil.AccessibilityServiceFragmentType.TOGGLE:
                return ToggleAccessibilityServicePreferenceFragment.class.getName();
            default:
                throw new IllegalArgumentException(
                        "Unsupported accessibility fragment type " + type);
        }
    }

    private RestrictedPreference createRestrictedPreference(String key, CharSequence title,
            CharSequence summary, Drawable icon, String fragment, String packageName, int uid) {
        final RestrictedPreference preference = new RestrictedPreference(mContext, packageName,
                uid);

        preference.setKey(key);
        preference.setTitle(title);
        preference.setSummary(summary);
        preference.setIcon(Utils.getAdaptiveIcon(mContext, icon, Color.WHITE));
        preference.setFragment(fragment);
        preference.setIconSize(ICON_SIZE_MEDIUM);
        preference.setPersistent(false); // Disable SharedPreferences.
        preference.setOrder(FIRST_PREFERENCE_IN_CATEGORY_INDEX);

        return preference;
    }

    private void setRestrictedPreferenceEnabled(RestrictedPreference preference,
            final List<String> permittedServices, boolean serviceEnabled) {
        // permittedServices null means all accessibility services are allowed.
        boolean serviceAllowed = permittedServices == null || permittedServices.contains(
                preference.getPackageName());

        if (android.permission.flags.Flags.enhancedConfirmationModeApisEnabled()
                && android.security.Flags.extendEcmToAllSettings()) {
            preference.checkEcmRestrictionAndSetDisabled(
                    AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE,
                    preference.getPackageName());
            if (preference.isDisabledByEcm()) {
                serviceAllowed = false;
            }

            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true);
            } else {
                // Disable accessibility service that are not permitted.
                final RestrictedLockUtils.EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                mContext, preference.getPackageName(), UserHandle.myUserId());

                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else if (!preference.isDisabledByEcm()) {
                    preference.setEnabled(false);
                }
            }
        } else {
            boolean appOpsAllowed;
            if (serviceAllowed) {
                try {
                    final int mode = mAppOps.noteOpNoThrow(
                            AppOpsManager.OP_ACCESS_RESTRICTED_SETTINGS,
                            preference.getUid(), preference.getPackageName());
                    final boolean ecmEnabled = mContext.getResources().getBoolean(
                            com.android.internal.R.bool.config_enhancedConfirmationModeEnabled);
                    appOpsAllowed = !ecmEnabled || mode == AppOpsManager.MODE_ALLOWED;
                    serviceAllowed = appOpsAllowed;
                } catch (Exception e) {
                    // Allow service in case if app ops is not available in testing.
                    appOpsAllowed = true;
                }
            } else {
                appOpsAllowed = false;
            }
            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true);
            } else {
                // Disable accessibility service that are not permitted.
                final RestrictedLockUtils.EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                mContext, preference.getPackageName(), UserHandle.myUserId());

                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else if (!appOpsAllowed) {
                    preference.setDisabledByAppOps(true);
                } else {
                    preference.setEnabled(false);
                }
            }
        }
    }

    /** Puts the basic extras into {@link RestrictedPreference}'s getExtras(). */
    private void putBasicExtras(RestrictedPreference preference, String prefKey,
            CharSequence title, CharSequence intro, CharSequence summary, int imageRes,
            String htmlDescription, ComponentName componentName, int metricsCategory) {
        final Bundle extras = preference.getExtras();
        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY, prefKey);
        extras.putCharSequence(AccessibilitySettings.EXTRA_TITLE, title);
        extras.putCharSequence(AccessibilitySettings.EXTRA_INTRO, intro);
        extras.putCharSequence(AccessibilitySettings.EXTRA_SUMMARY, summary);
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);
        extras.putInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES, imageRes);
        extras.putString(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, htmlDescription);
        extras.putInt(AccessibilitySettings.EXTRA_METRICS_CATEGORY, metricsCategory);
    }

    /**
     * Puts the service extras into {@link RestrictedPreference}'s getExtras().
     *
     * <p><b>Note:</b> Called by {@link AccessibilityServiceInfo}.</p>
     *
     * @param preference The preference we are configuring.
     * @param resolveInfo The service resolve info.
     * @param serviceEnabled Whether the accessibility service is enabled.
     */
    private void putServiceExtras(RestrictedPreference preference, ResolveInfo resolveInfo,
            Boolean serviceEnabled) {
        final Bundle extras = preference.getExtras();

        extras.putParcelable(AccessibilitySettings.EXTRA_RESOLVE_INFO, resolveInfo);
        extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED, serviceEnabled);
    }

    /**
     * Puts the settings extras into {@link RestrictedPreference}'s getExtras().
     *
     * <p><b>Note:</b> Called when settings UI is needed.</p>
     *
     * @param preference The preference we are configuring.
     * @param packageName Package of accessibility feature.
     * @param settingsClassName The component name of an activity that allows the user to modify
     *                          the settings for this accessibility feature.
     */
    private void putSettingsExtras(RestrictedPreference preference, String packageName,
            String settingsClassName) {
        final Bundle extras = preference.getExtras();

        if (!TextUtils.isEmpty(settingsClassName)) {
            extras.putString(AccessibilitySettings.EXTRA_SETTINGS_TITLE,
                    mContext.getText(R.string.accessibility_menu_item_settings).toString());
            extras.putString(AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME,
                    new ComponentName(packageName, settingsClassName).flattenToString());
        }
    }

    /**
     * Puts the information about a particular application
     * {@link android.service.quicksettings.TileService} into {@link RestrictedPreference}'s
     * getExtras().
     *
     * <p><b>Note:</b> Called when a tooltip of
     * {@link android.service.quicksettings.TileService} is needed.</p>
     *
     * @param preference The preference we are configuring.
     * @param packageName Package of accessibility feature.
     * @param tileServiceClassName The component name of tileService is associated with this
     *                             accessibility feature.
     */
    private void putTileServiceExtras(RestrictedPreference preference, String packageName,
            String tileServiceClassName) {
        final Bundle extras = preference.getExtras();
        if (!TextUtils.isEmpty(tileServiceClassName)) {
            extras.putString(AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME,
                    new ComponentName(packageName, tileServiceClassName).flattenToString());
        }
    }
}
