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

package com.android.settings.spa

import android.content.Context
import com.android.settings.spa.app.AllAppListPageProvider
import com.android.settings.spa.app.AppsMainPageProvider
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settings.spa.app.backgroundinstall.BackgroundInstalledAppsPageProvider
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.AllFilesAccessAppListProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.MediaManagementAppsAppListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.PictureInPictureListProvider
import com.android.settings.spa.app.specialaccess.SpecialAppAccessPageProvider
import com.android.settings.spa.app.specialaccess.WifiControlAppListProvider
import com.android.settings.spa.development.UsageStatsPageProvider
import com.android.settings.spa.home.HomePageProvider
import com.android.settings.spa.notification.AppListNotificationsPageProvider
import com.android.settings.spa.notification.NotificationMainPageProvider
import com.android.settings.spa.system.AppLanguagesPageProvider
import com.android.settings.spa.system.LanguageAndInputPageProvider
import com.android.settings.spa.system.SystemMainPageProvider
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.SpaEnvironment
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListTemplate

open class SettingsSpaEnvironment(context: Context) : SpaEnvironment(context) {
    override val pageProviderRepository = lazy {
        val togglePermissionAppListTemplate =
            TogglePermissionAppListTemplate(
                allProviders =
                    listOf(
                        AllFilesAccessAppListProvider,
                        DisplayOverOtherAppsAppListProvider,
                        MediaManagementAppsAppListProvider,
                        ModifySystemSettingsAppListProvider,
                        PictureInPictureListProvider,
                        InstallUnknownAppsListProvider,
                        AlarmsAndRemindersAppListProvider,
                        WifiControlAppListProvider,
                    ),
            )
        SettingsPageProviderRepository(
            allPageProviders = listOf(
                HomePageProvider,
                AppsMainPageProvider,
                AllAppListPageProvider,
                AppInfoSettingsProvider,
                SpecialAppAccessPageProvider,
                NotificationMainPageProvider,
                AppListNotificationsPageProvider,
                SystemMainPageProvider,
                LanguageAndInputPageProvider,
                AppLanguagesPageProvider,
                UsageStatsPageProvider,
                BackgroundInstalledAppsPageProvider,
            ) + togglePermissionAppListTemplate.createPageProviders(),
            rootPages = listOf(
                SettingsPage.create(HomePageProvider.name),
            ),
        )
    }
}
