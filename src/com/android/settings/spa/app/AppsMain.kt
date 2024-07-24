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

package com.android.settings.spa.app

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.backgroundinstall.BackgroundInstalledAppsPageProvider
import com.android.settings.spa.app.specialaccess.SpecialAppAccessPageProvider
import com.android.settingslib.spa.framework.common.SettingsEntry
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.SettingsIcon

object AppsMainPageProvider : SettingsPageProvider {
    override val name = "AppsMain"
    private val owner = createSettingsPage()

    override fun isEnabled(arguments: Bundle?) = false

    @Composable
    override fun Page(arguments: Bundle?) {
        RegularScaffold(title = getTitle(arguments)) {
            AllAppListPageProvider.buildInjectEntry().build().UiLayout()
            SpecialAppAccessPageProvider.EntryItem()
            BackgroundInstalledAppsPageProvider.EntryItem()
        }
    }

    fun buildInjectEntry() =
        SettingsEntryBuilder.createInject(owner = owner)
            .setUiLayoutFn {
                val summary = stringResource(R.string.app_and_notification_dashboard_summary)
                Preference(object : PreferenceModel {
                    override val title = stringResource(R.string.apps_dashboard_title)
                    override val summary = { summary }
                    override val onClick = navigator(name)
                    override val icon = @Composable {
                        SettingsIcon(imageVector = Icons.Outlined.Apps)
                    }
                })
            }

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.apps_dashboard_title)
    }

    override fun buildEntry(arguments: Bundle?): List<SettingsEntry> {
        return listOf(
            AllAppListPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            SpecialAppAccessPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            BackgroundInstalledAppsPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
        )
    }
}
