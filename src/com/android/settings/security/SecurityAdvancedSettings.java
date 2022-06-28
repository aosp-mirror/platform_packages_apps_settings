/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_FOR_WORK;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGED_DEVICE_INFO;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGE_DEVICE_ADMIN_APPS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_SECURITY_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_SET_UNLOCK_LAUNCH_PICKER_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_UNIFY_LOCKS_SUMMARY;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.biometrics.combination.CombinedBiometricProfileStatusPreferenceController;
import com.android.settings.biometrics.face.FaceProfileStatusPreferenceController;
import com.android.settings.biometrics.fingerprint.FingerprintProfileStatusPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * An overflow menu for {@code SecuritySettings} containing advanced security settings.
 *
 * <p>This includes all work-profile related settings.
 */
@SearchIndexable
public class SecurityAdvancedSettings extends DashboardFragment {

    private static final String TAG = "SecurityAdvancedSettings";
    private static final String WORK_PROFILE_SECURITY_CATEGORY = "security_category_profile";

    /** Used in case of old Security settings when SafetyCenter is disabled */
    private static final String CATEGORY_SECURITY_LEGACY_ADVANCED_SETTINGS =
            "com.android.settings.category.ia.legacy_advanced_security";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterpriseStringTitle("unlock_set_or_change_profile",
                WORK_PROFILE_SET_UNLOCK_LAUNCH_PICKER_TITLE,
                R.string.unlock_set_unlock_launch_picker_title_profile);
        replaceEnterpriseStringSummary("unification",
                WORK_PROFILE_UNIFY_LOCKS_SUMMARY,
                R.string.lock_settings_profile_unification_summary);
        replaceEnterpriseStringTitle("fingerprint_settings_profile",
                FINGERPRINT_FOR_WORK,
                R.string.security_settings_work_fingerprint_preference_title);
        replaceEnterpriseStringTitle("manage_device_admin",
                MANAGE_DEVICE_ADMIN_APPS, R.string.manage_device_admin);
        replaceEnterpriseStringTitle("security_category_profile",
                WORK_PROFILE_SECURITY_TITLE, R.string.lock_settings_profile_title);
        replaceEnterpriseStringTitle("enterprise_privacy", MANAGED_DEVICE_INFO,
                R.string.enterprise_privacy_settings);

    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SECURITY_ADVANCED;
    }

    @Override
    public String getCategoryKey() {
        final Context context = getContext();
        if (context == null) {
            return CATEGORY_SECURITY_LEGACY_ADVANCED_SETTINGS;
        } else if (SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS;
        } else {
            final SecuritySettingsFeatureProvider securitySettingsFeatureProvider =
                    FeatureFactory.getFactory(context)
                            .getSecuritySettingsFeatureProvider();

            if (securitySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment()) {
                return securitySettingsFeatureProvider.getAlternativeAdvancedSettingsCategoryKey();
            } else {
                return CATEGORY_SECURITY_LEGACY_ADVANCED_SETTINGS;
            }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_advanced_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), this /* host*/);
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (use(TrustAgentListPreferenceController.class)
                .handleActivityResult(requestCode, resultCode)) {
            return;
        }
        if (use(LockUnificationPreferenceController.class)
                .handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, DashboardFragment host) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new TrustAgentListPreferenceController(context, host, lifecycle));

        final List<AbstractPreferenceController> profileSecurityControllers = new ArrayList<>();
        profileSecurityControllers.add(new ChangeProfileScreenLockPreferenceController(
                context, host));
        profileSecurityControllers.add(new LockUnificationPreferenceController(context, host));
        profileSecurityControllers.add(new VisiblePatternProfilePreferenceController(
                context, lifecycle));
        profileSecurityControllers.add(new FaceProfileStatusPreferenceController(
                context, lifecycle));
        profileSecurityControllers.add(new FingerprintProfileStatusPreferenceController(
                context, lifecycle));
        profileSecurityControllers
                .add(new CombinedBiometricProfileStatusPreferenceController(context, lifecycle));
        controllers.add(new PreferenceCategoryController(context, WORK_PROFILE_SECURITY_CATEGORY)
                .setChildren(profileSecurityControllers));
        controllers.addAll(profileSecurityControllers);

        return controllers;
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.security_advanced_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* host*/);
                }
            };
}
