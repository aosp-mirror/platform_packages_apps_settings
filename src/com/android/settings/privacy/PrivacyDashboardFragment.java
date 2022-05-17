/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.privacy;

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECTED_WORK_AND_PERSONAL_APPS_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCKED_NOTIFICATION_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_NOTIFICATIONS_SECTION_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PRIVACY_POLICY_INFO;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PRIVACY_POLICY_INFO_SUMMARY;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class PrivacyDashboardFragment extends DashboardFragment {
    private static final String TAG = "PrivacyDashboardFrag";
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "privacy_lock_screen_notifications";
    private static final String KEY_WORK_PROFILE_CATEGORY =
            "privacy_work_profile_notifications_category";
    private static final String KEY_NOTIFICATION_WORK_PROFILE_NOTIFICATIONS =
            "privacy_lock_screen_work_profile_notifications";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TOP_LEVEL_PRIVACY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterpriseStringTitle("privacy_lock_screen_work_profile_notifications",
                WORK_PROFILE_LOCKED_NOTIFICATION_TITLE,
                R.string.locked_work_profile_notification_title);
        replaceEnterpriseStringTitle("interact_across_profiles_privacy",
                CONNECTED_WORK_AND_PERSONAL_APPS_TITLE, R.string.interact_across_profiles_title);
        replaceEnterpriseStringTitle("privacy_work_profile_notifications_category",
                WORK_PROFILE_NOTIFICATIONS_SECTION_HEADER, R.string.profile_section_header);
        replaceEnterpriseStringTitle("work_policy_info",
                WORK_PROFILE_PRIVACY_POLICY_INFO, R.string.work_policy_privacy_settings);
        replaceEnterpriseStringSummary("work_policy_info",
                WORK_PROFILE_PRIVACY_POLICY_INFO_SUMMARY,
                R.string.work_policy_privacy_settings_summary);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_privacy_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return getPreferenceScreenResId(getContext());
    }

    private static int getPreferenceScreenResId(Context context) {
        if (SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return R.xml.privacy_advanced_settings;
        } else {
            return R.xml.privacy_dashboard_settings;
        }
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final LockScreenNotificationPreferenceController notificationController =
                new LockScreenNotificationPreferenceController(context,
                        KEY_LOCK_SCREEN_NOTIFICATIONS,
                        KEY_WORK_PROFILE_CATEGORY,
                        KEY_NOTIFICATION_WORK_PROFILE_NOTIFICATIONS);
        if (lifecycle != null) {
            lifecycle.addObserver(notificationController);
        }
        controllers.add(notificationController);

        return controllers;

    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = getPreferenceScreenResId(context);
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
