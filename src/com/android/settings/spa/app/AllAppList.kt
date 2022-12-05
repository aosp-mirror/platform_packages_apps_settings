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

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.android.settingslib.spaprivileged.template.app.getStorageSize
import kotlinx.coroutines.flow.Flow

object AllAppListPageProvider : SettingsPageProvider {
    override val name = "AllAppList"

    @Composable
    override fun Page(arguments: Bundle?) {
        AllAppListPage()
    }

    fun buildInjectEntry() = SettingsEntryBuilder
        .createInject(owner = SettingsPage.create(name))
        .setSearchDataFn { null }
        .setUiLayoutFn {
            Preference(object : PreferenceModel {
                override val title = stringResource(R.string.all_apps)
                override val onClick = navigator(name)
            })
        }
}

@Composable
fun AllAppListPage(
    appList: @Composable AppListInput<AppRecordWithSize>.() -> Unit = { AppList() },
) {
    val resetAppDialogPresenter = rememberResetAppDialogPresenter()
    AppListPage(
        title = stringResource(R.string.all_apps),
        listModel = remember { AllAppListModel() },
        showInstantApps = true,
        moreOptions = { ResetAppPreferences(resetAppDialogPresenter::open) },
        appList = appList,
    ) {
        AppListItem(onClick = AppInfoSettingsProvider.navigator(app = record.app))
    }
}

data class AppRecordWithSize(
    override val app: ApplicationInfo,
) : AppRecord

class AllAppListModel(
    private val getSummary: @Composable ApplicationInfo.() -> State<String> = { getStorageSize() },
) : AppListModel<AppRecordWithSize> {

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        appListFlow.mapItem(::AppRecordWithSize)

    @Composable
    override fun getSummary(option: Int, record: AppRecordWithSize) = record.app.getSummary()
}
