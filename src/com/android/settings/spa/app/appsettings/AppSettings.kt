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

package com.android.settings.spa.app.appsettings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.PictureInPictureListProvider
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.toRoute
import com.android.settingslib.spaprivileged.template.app.AppInfoProvider

private const val PACKAGE_NAME = "packageName"
private const val USER_ID = "userId"

object AppSettingsProvider : SettingsPageProvider {
    override val name = "AppSettings"

    override val parameter = listOf(
        navArgument(PACKAGE_NAME) { type = NavType.StringType },
        navArgument(USER_ID) { type = NavType.IntType },
    )

    @Composable
    override fun Page(arguments: Bundle?) {
        val packageName = arguments!!.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        remember { PackageManagers.getPackageInfoAsUser(packageName, userId) }?.let {
            AppSettings(it)
        }
    }

    @Composable
    fun navigator(app: ApplicationInfo) = navigator(route = "$name/${app.toRoute()}")
}

@Composable
private fun AppSettings(packageInfo: PackageInfo) {
    RegularScaffold(title = stringResource(R.string.application_info_label)) {
        val appInfoProvider = remember { AppInfoProvider(packageInfo) }

        appInfoProvider.AppInfo()

        Category(title = stringResource(R.string.advanced_apps)) {
            val app = packageInfo.applicationInfo
            DisplayOverOtherAppsAppListProvider.InfoPageEntryItem(app)
            ModifySystemSettingsAppListProvider.InfoPageEntryItem(app)
            PictureInPictureListProvider.InfoPageEntryItem(app)
            InstallUnknownAppsListProvider.InfoPageEntryItem(app)
            // TODO: interact_across_profiles
            AlarmsAndRemindersAppListProvider.InfoPageEntryItem(app)
        }

        // TODO: app_installer
        appInfoProvider.FooterAppVersion()
    }
}
