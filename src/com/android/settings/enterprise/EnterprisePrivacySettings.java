/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_ACCESS_CAMERA;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_ACCESS_LOCATION;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_ACCESS_MICROPHONE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_APPS_INSTALLED;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_SET_CURRENT_INPUT_METHOD;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_SET_DEFAULT_APPS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_SET_HTTP_PROXY;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_LOCK_DEVICE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_SEE_APPS_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_SEE_BUG_REPORT_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_SEE_NETWORK_LOGS_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_SEE_SECURITY_LOGS_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_SEE_USAGE_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_SEE_WORK_DATA_WARNING;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CAN_WIPE_DEVICE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CONFIGURED_FAILED_PASSWORD_WIPE_DEVICE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_CONFIGURED_FAILED_PASSWORD_WIPE_WORK_PROFILE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ALWAYS_ON_VPN_WORK_PROFILE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CA_CERTS_PERSONAL_PROFILE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CA_CERTS_WORK_PROFILE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CHANGES_BY_ORGANIZATION_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.ENTERPRISE_PRIVACY_FOOTER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.INFORMATION_SEEN_BY_ORGANIZATION_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGED_DEVICE_INFO;
import static android.app.admin.DevicePolicyResources.Strings.Settings.YOUR_ACCESS_TO_THIS_DEVICE_TITLE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class EnterprisePrivacySettings extends DashboardFragment {

    static final String TAG = "EnterprisePrivacySettings";

    @VisibleForTesting
    PrivacySettingsPreference mPrivacySettingsPreference;

    @Override
    public void onAttach(Context context) {
        mPrivacySettingsPreference =
                PrivacySettingsPreferenceFactory.createPrivacySettingsPreference(context);

        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (mPrivacySettingsPreference instanceof PrivacySettingsFinancedPreference) {
            return;
        }

        replaceEnterprisePreferenceScreenTitle(
                MANAGED_DEVICE_INFO, R.string.enterprise_privacy_settings);

        replaceEnterpriseStringTitle("exposure_category",
                INFORMATION_SEEN_BY_ORGANIZATION_TITLE,
                R.string.enterprise_privacy_exposure_category);
        replaceEnterpriseStringTitle("enterprise_privacy_enterprise_data",
                ADMIN_CAN_SEE_WORK_DATA_WARNING, R.string.enterprise_privacy_enterprise_data);
        replaceEnterpriseStringTitle("enterprise_privacy_installed_packages",
                ADMIN_CAN_SEE_APPS_WARNING, R.string.enterprise_privacy_installed_packages);
        replaceEnterpriseStringTitle("enterprise_privacy_usage_stats",
                ADMIN_CAN_SEE_USAGE_WARNING, R.string.enterprise_privacy_usage_stats);
        replaceEnterpriseStringTitle("network_logs",
                ADMIN_CAN_SEE_NETWORK_LOGS_WARNING, R.string.enterprise_privacy_network_logs);
        replaceEnterpriseStringTitle("bug_reports",
                ADMIN_CAN_SEE_BUG_REPORT_WARNING, R.string.enterprise_privacy_bug_reports);
        replaceEnterpriseStringTitle("security_logs",
                ADMIN_CAN_SEE_SECURITY_LOGS_WARNING, R.string.enterprise_privacy_security_logs);
        replaceEnterpriseStringTitle("exposure_changes_category",
                CHANGES_BY_ORGANIZATION_TITLE,
                R.string.enterprise_privacy_exposure_changes_category);
        replaceEnterpriseStringTitle("number_enterprise_installed_packages",
                ADMIN_ACTION_APPS_INSTALLED,
                R.string.enterprise_privacy_enterprise_installed_packages);
        replaceEnterpriseStringTitle("enterprise_privacy_number_location_access_packages",
                ADMIN_ACTION_ACCESS_LOCATION, R.string.enterprise_privacy_location_access);
        replaceEnterpriseStringTitle("enterprise_privacy_number_microphone_access_packages",
                ADMIN_ACTION_ACCESS_MICROPHONE, R.string.enterprise_privacy_microphone_access);
        replaceEnterpriseStringTitle("enterprise_privacy_number_camera_access_packages",
                ADMIN_ACTION_ACCESS_CAMERA, R.string.enterprise_privacy_camera_access);
        replaceEnterpriseStringTitle("number_enterprise_set_default_apps",
                ADMIN_ACTION_SET_DEFAULT_APPS,
                R.string.enterprise_privacy_enterprise_set_default_apps);
        replaceEnterpriseStringTitle("always_on_vpn_managed_profile",
                ALWAYS_ON_VPN_WORK_PROFILE, R.string.enterprise_privacy_always_on_vpn_work);
        replaceEnterpriseStringTitle("input_method",
                ADMIN_ACTION_SET_CURRENT_INPUT_METHOD, R.string.enterprise_privacy_input_method);
        replaceEnterpriseStringTitle("global_http_proxy",
                ADMIN_ACTION_SET_HTTP_PROXY, R.string.enterprise_privacy_global_http_proxy);
        replaceEnterpriseStringTitle("ca_certs_current_user",
                CA_CERTS_PERSONAL_PROFILE, R.string.enterprise_privacy_ca_certs_personal);
        replaceEnterpriseStringTitle("ca_certs_managed_profile",
                CA_CERTS_WORK_PROFILE, R.string.enterprise_privacy_ca_certs_work);
        replaceEnterpriseStringTitle("device_access_category",
                YOUR_ACCESS_TO_THIS_DEVICE_TITLE,
                R.string.enterprise_privacy_device_access_category);
        replaceEnterpriseStringTitle("enterprise_privacy_lock_device",
                ADMIN_CAN_LOCK_DEVICE, R.string.enterprise_privacy_lock_device);
        replaceEnterpriseStringTitle("enterprise_privacy_wipe_device",
                ADMIN_CAN_WIPE_DEVICE, R.string.enterprise_privacy_wipe_device);
        replaceEnterpriseStringTitle("failed_password_wipe_current_user",
                ADMIN_CONFIGURED_FAILED_PASSWORD_WIPE_DEVICE,
                R.string.enterprise_privacy_failed_password_wipe_device);
        replaceEnterpriseStringTitle("failed_password_wipe_managed_profile",
                ADMIN_CONFIGURED_FAILED_PASSWORD_WIPE_WORK_PROFILE,
                R.string.enterprise_privacy_failed_password_wipe_work);
        replaceEnterpriseStringTitle("enterprise_privacy_footer",
                ENTERPRISE_PRIVACY_FOOTER, R.string.enterprise_privacy_header);
    }

    @Override
    public void onDetach() {
        mPrivacySettingsPreference = null;
        super.onDetach();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ENTERPRISE_PRIVACY_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return mPrivacySettingsPreference.getPreferenceScreenResId();
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return mPrivacySettingsPreference.createPreferenceControllers(true /* async */);
    }

    public static boolean isPageEnabled(Context context) {
        return FeatureFactory.getFeatureFactory()
                .getEnterprisePrivacyFeatureProvider()
                .hasDeviceOwner();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                private PrivacySettingsPreference mPrivacySettingsPreference;

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isPageEnabled(context);
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    mPrivacySettingsPreference =
                            PrivacySettingsPreferenceFactory.createPrivacySettingsPreference(
                                    context);
                    return mPrivacySettingsPreference.getXmlResourcesToIndex();
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    mPrivacySettingsPreference =
                            PrivacySettingsPreferenceFactory.createPrivacySettingsPreference(
                                    context);
                    return mPrivacySettingsPreference.createPreferenceControllers(
                            false /* async */);
                }
            };
}
