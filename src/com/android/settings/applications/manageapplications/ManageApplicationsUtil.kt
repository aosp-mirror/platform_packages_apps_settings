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

package com.android.settings.applications.manageapplications

import android.content.Context
import android.util.FeatureFlagUtils
import com.android.settings.Settings.AlarmsAndRemindersActivity
import com.android.settings.Settings.AppBatteryUsageActivity
import com.android.settings.Settings.UserAspectRatioAppListActivity
import com.android.settings.Settings.ChangeNfcTagAppsActivity
import com.android.settings.Settings.ChangeWifiStateActivity
import com.android.settings.Settings.ClonedAppsListActivity
import com.android.settings.Settings.GamesStorageActivity
import com.android.settings.Settings.HighPowerApplicationsActivity
import com.android.settings.Settings.LongBackgroundTasksActivity
import com.android.settings.Settings.ManageExternalSourcesActivity
import com.android.settings.Settings.ManageExternalStorageActivity
import com.android.settings.Settings.MediaManagementAppsActivity
import com.android.settings.Settings.NotificationAppListActivity
import com.android.settings.Settings.NotificationReviewPermissionsActivity
import com.android.settings.Settings.OverlaySettingsActivity
import com.android.settings.Settings.StorageUseActivity
import com.android.settings.Settings.TurnScreenOnSettingsActivity
import com.android.settings.Settings.UsageAccessSettingsActivity
import com.android.settings.Settings.WriteSettingsActivity
import com.android.settings.applications.appinfo.AppLocaleDetails
import com.android.settings.applications.manageapplications.ManageApplications.LIST_MANAGE_EXTERNAL_STORAGE
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_ALARMS_AND_REMINDERS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_APPS_LOCALE
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_USER_ASPECT_RATIO_APPS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_BATTERY_OPTIMIZATION
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_CLONED_APPS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_GAMES
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_HIGH_POWER
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_LONG_BACKGROUND_TASKS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MAIN
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MANAGE_SOURCES
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MEDIA_MANAGEMENT_APPS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_NFC_TAG_APPS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_NOTIFICATION
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_OVERLAY
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_STORAGE
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_TURN_SCREEN_ON
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_USAGE_ACCESS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_WIFI_ACCESS
import com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_WRITE_SETTINGS
import com.android.settings.spa.app.AllAppListPageProvider
import com.android.settings.spa.app.battery.BatteryOptimizationModeAppListPageProvider
import com.android.settings.spa.app.appcompat.UserAspectRatioAppsPageProvider
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.AllFilesAccessAppListProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.LongBackgroundTasksAppListProvider
import com.android.settings.spa.app.specialaccess.MediaManagementAppsAppListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.NfcTagAppsSettingsProvider
import com.android.settings.spa.app.specialaccess.TurnScreenOnAppsAppListProvider
import com.android.settings.spa.app.specialaccess.WifiControlAppListProvider
import com.android.settings.spa.notification.AppListNotificationsPageProvider
import com.android.settings.spa.system.AppLanguagesPageProvider

/**
 * Utils for [ManageApplications].
 */
object ManageApplicationsUtil {
    private val LIST_TYPE_CLASS_MAP = mapOf(
        StorageUseActivity::class to LIST_TYPE_STORAGE,
        UsageAccessSettingsActivity::class to LIST_TYPE_USAGE_ACCESS,
        HighPowerApplicationsActivity::class to LIST_TYPE_HIGH_POWER,
        OverlaySettingsActivity::class to LIST_TYPE_OVERLAY,
        WriteSettingsActivity::class to LIST_TYPE_WRITE_SETTINGS,
        ManageExternalSourcesActivity::class to LIST_TYPE_MANAGE_SOURCES,
        GamesStorageActivity::class to LIST_TYPE_GAMES,
        ChangeWifiStateActivity::class to LIST_TYPE_WIFI_ACCESS,
        ManageExternalStorageActivity::class to LIST_MANAGE_EXTERNAL_STORAGE,
        MediaManagementAppsActivity::class to LIST_TYPE_MEDIA_MANAGEMENT_APPS,
        AlarmsAndRemindersActivity::class to LIST_TYPE_ALARMS_AND_REMINDERS,
        NotificationAppListActivity::class to LIST_TYPE_NOTIFICATION,
        NotificationReviewPermissionsActivity::class to LIST_TYPE_NOTIFICATION,
        AppLocaleDetails::class to LIST_TYPE_APPS_LOCALE,
        AppBatteryUsageActivity::class to LIST_TYPE_BATTERY_OPTIMIZATION,
        LongBackgroundTasksActivity::class to LIST_TYPE_LONG_BACKGROUND_TASKS,
        ClonedAppsListActivity::class to LIST_TYPE_CLONED_APPS,
        ChangeNfcTagAppsActivity::class to LIST_TYPE_NFC_TAG_APPS,
        TurnScreenOnSettingsActivity::class to LIST_TYPE_TURN_SCREEN_ON,
        UserAspectRatioAppListActivity::class to LIST_TYPE_USER_ASPECT_RATIO_APPS,
    )

    @JvmField
    val LIST_TYPE_MAP = LIST_TYPE_CLASS_MAP.mapKeys { it.key.java.name }

    @JvmStatic
    fun getSpaDestination(context: Context, listType: Int): String? {
        if (!FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_ENABLE_SPA)) {
            return null
        }
        return when (listType) {
            LIST_TYPE_OVERLAY -> DisplayOverOtherAppsAppListProvider.getAppListRoute()
            LIST_TYPE_WRITE_SETTINGS -> ModifySystemSettingsAppListProvider.getAppListRoute()
            LIST_TYPE_MANAGE_SOURCES -> InstallUnknownAppsListProvider.getAppListRoute()
            LIST_MANAGE_EXTERNAL_STORAGE -> AllFilesAccessAppListProvider.getAppListRoute()
            LIST_TYPE_MEDIA_MANAGEMENT_APPS -> MediaManagementAppsAppListProvider.getAppListRoute()
            LIST_TYPE_ALARMS_AND_REMINDERS -> AlarmsAndRemindersAppListProvider.getAppListRoute()
            LIST_TYPE_WIFI_ACCESS -> WifiControlAppListProvider.getAppListRoute()
            LIST_TYPE_NOTIFICATION -> AppListNotificationsPageProvider.name
            LIST_TYPE_APPS_LOCALE -> AppLanguagesPageProvider.name
            LIST_TYPE_MAIN -> AllAppListPageProvider.name
            LIST_TYPE_NFC_TAG_APPS -> NfcTagAppsSettingsProvider.getAppListRoute()
            LIST_TYPE_USER_ASPECT_RATIO_APPS -> UserAspectRatioAppsPageProvider.name
            LIST_TYPE_LONG_BACKGROUND_TASKS -> LongBackgroundTasksAppListProvider.getAppListRoute()
            LIST_TYPE_TURN_SCREEN_ON -> TurnScreenOnAppsAppListProvider.getAppListRoute()
            // TODO(b/292165031) enable once sorting is supported
            //LIST_TYPE_STORAGE -> StorageAppListPageProvider.Apps.name
            //LIST_TYPE_GAMES -> StorageAppListPageProvider.Games.name
            LIST_TYPE_BATTERY_OPTIMIZATION -> BatteryOptimizationModeAppListPageProvider.name
            else -> null
        }
    }
}
