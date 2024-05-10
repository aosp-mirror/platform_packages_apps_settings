/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.safetycenter;

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONNECTED_WORK_AND_PERSONAL_APPS_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_FOR_WORK;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGED_DEVICE_INFO;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGE_DEVICE_ADMIN_APPS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCKED_NOTIFICATION_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_NOTIFICATIONS_SECTION_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PRIVACY_POLICY_INFO;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PRIVACY_POLICY_INFO_SUMMARY;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_SECURITY_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_SET_UNLOCK_LAUNCH_PICKER_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_UNIFY_LOCKS_SUMMARY;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.biometrics.combination.CombinedBiometricProfileStatusPreferenceController;
import com.android.settings.biometrics.face.FaceProfileStatusPreferenceController;
import com.android.settings.biometrics.fingerprint.FingerprintProfileStatusPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.privacy.PrivacyDashboardFragment;
import com.android.settings.security.ChangeProfileScreenLockPreferenceController;
import com.android.settings.security.LockUnificationPreferenceController;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/** A class with helper method used in logic involving safety center. */
public final class SafetyCenterUtils {

    private static final String WORK_PROFILE_SECURITY_CATEGORY = "work_profile_category";
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "privacy_lock_screen_notifications";
    private static final String KEY_WORK_PROFILE_CATEGORY =
            "privacy_work_profile_notifications_category";
    private static final String KEY_NOTIFICATION_WORK_PROFILE_NOTIFICATIONS =
            "privacy_lock_screen_work_profile_notifications";

    /**
     * Returns preference controllers related to advanced security entries. This is used in {@link
     * MoreSecurityPrivacyFragment} and {@link
     * com.android.settings.security.SecurityAdvancedSettings}.
     */
    public static List<AbstractPreferenceController> getControllersForAdvancedSecurity(
            Context context,
            com.android.settingslib.core.lifecycle.Lifecycle lifecycle,
            DashboardFragment host) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new TrustAgentListPreferenceController(context, host, lifecycle));

        final List<AbstractPreferenceController> profileSecurityControllers = new ArrayList<>();
        profileSecurityControllers.add(
                new ChangeProfileScreenLockPreferenceController(context, host));
        profileSecurityControllers.add(new LockUnificationPreferenceController(context, host));
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
     * Returns preference controllers for advanced privacy entries. This is used in {@link
     * MoreSecurityPrivacyFragment} and {@link PrivacyDashboardFragment}.
     */
    public static List<AbstractPreferenceController> getControllersForAdvancedPrivacy(
            Context context, com.android.settingslib.core.lifecycle.Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final LockScreenNotificationPreferenceController notificationController =
                new LockScreenNotificationPreferenceController(
                        context,
                        KEY_LOCK_SCREEN_NOTIFICATIONS,
                        KEY_WORK_PROFILE_CATEGORY,
                        KEY_NOTIFICATION_WORK_PROFILE_NOTIFICATIONS);
        if (lifecycle != null) {
            lifecycle.addObserver(notificationController);
        }
        controllers.add(notificationController);
        return controllers;
    }

    /** Replaces relevant strings with their enterprise variants for the privacy entries. */
    public static void replaceEnterpriseStringsForPrivacyEntries(
            DashboardFragment dashboardFragment) {
        dashboardFragment.replaceEnterpriseStringTitle(
                "privacy_lock_screen_work_profile_notifications",
                WORK_PROFILE_LOCKED_NOTIFICATION_TITLE,
                R.string.locked_work_profile_notification_title);
        dashboardFragment.replaceEnterpriseStringTitle(
                "interact_across_profiles_privacy",
                CONNECTED_WORK_AND_PERSONAL_APPS_TITLE,
                R.string.interact_across_profiles_title);
        dashboardFragment.replaceEnterpriseStringTitle(
                "privacy_work_profile_notifications_category",
                WORK_PROFILE_NOTIFICATIONS_SECTION_HEADER,
                R.string.profile_section_header);
        dashboardFragment.replaceEnterpriseStringTitle(
                "work_policy_info",
                WORK_PROFILE_PRIVACY_POLICY_INFO,
                R.string.work_policy_privacy_settings);
        dashboardFragment.replaceEnterpriseStringSummary(
                "work_policy_info",
                WORK_PROFILE_PRIVACY_POLICY_INFO_SUMMARY,
                R.string.work_policy_privacy_settings_summary);
    }

    /** Replaces relevant strings with their enterprise variants for the security entries. */
    public static void replaceEnterpriseStringsForSecurityEntries(
            DashboardFragment dashboardFragment) {
        dashboardFragment.replaceEnterpriseStringTitle(
                "unlock_set_or_change_profile",
                WORK_PROFILE_SET_UNLOCK_LAUNCH_PICKER_TITLE,
                R.string.unlock_set_unlock_launch_picker_title_profile);
        dashboardFragment.replaceEnterpriseStringSummary(
                "unification",
                WORK_PROFILE_UNIFY_LOCKS_SUMMARY,
                R.string.lock_settings_profile_unification_summary);
        dashboardFragment.replaceEnterpriseStringTitle(
                "fingerprint_settings_profile",
                FINGERPRINT_FOR_WORK,
                R.string.security_settings_work_fingerprint_preference_title);
        dashboardFragment.replaceEnterpriseStringTitle(
                "manage_device_admin", MANAGE_DEVICE_ADMIN_APPS, R.string.manage_device_admin);
        dashboardFragment.replaceEnterpriseStringTitle(
                "security_category_profile",
                WORK_PROFILE_SECURITY_TITLE,
                R.string.lock_settings_profile_title);
        dashboardFragment.replaceEnterpriseStringTitle(
                "enterprise_privacy", MANAGED_DEVICE_INFO, R.string.enterprise_privacy_settings);
    }

    private SafetyCenterUtils() {}
}
