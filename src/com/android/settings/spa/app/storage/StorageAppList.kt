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

package com.android.settings.spa.app.storage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.android.settingslib.spaprivileged.template.app.calculateSizeBytes
import com.android.settingslib.spaprivileged.template.app.getStorageSize
import kotlinx.coroutines.flow.Flow

sealed class StorageAppListPageProvider(private val type: StorageType) : SettingsPageProvider {
    @Composable
    override fun Page(arguments: Bundle?) {
        StorageAppListPage(type)
    }

    object Apps : StorageAppListPageProvider(StorageType.Apps) {
        override val name = "StorageAppList"
    }

    object Games : StorageAppListPageProvider(StorageType.Games) {
        override val name = "GameStorageAppList"
    }
}

sealed class StorageType(
    @StringRes val titleResource: Int,
    val filter: (AppRecordWithSize) -> Boolean
) {
    object Apps : StorageType(
        titleResource = R.string.apps_storage,
        filter = {
            (it.app.flags and ApplicationInfo.FLAG_IS_GAME) == 0 &&
            it.app.category != ApplicationInfo.CATEGORY_GAME
        }
    )
    object Games : StorageType(
        titleResource = R.string.game_storage_settings,
        filter = {
            (it.app.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                it.app.category == ApplicationInfo.CATEGORY_GAME
        }
    )
}

@Composable
fun StorageAppListPage(
    type: StorageType,
    appList: @Composable AppListInput<AppRecordWithSize>.() -> Unit = { AppList() }
) {
    val context = LocalContext.current
    AppListPage(
        title = stringResource(type.titleResource),
        listModel = when (type) {
            StorageType.Apps -> remember(context) { StorageAppListModel(context, type) }
            StorageType.Games -> remember(context) { StorageAppListModel(context, type) }
        },
        showInstantApps = true,
        matchAnyUserForAdmin = true,
        appList = appList,
        moreOptions = {  }, // TODO(b/292165031) Sorting in Options not yet supported
    )
}

data class AppRecordWithSize(
    override val app: ApplicationInfo,
    val size: Long
) : AppRecord

class StorageAppListModel(
    private val context: Context,
    private val type: StorageType,
    private val getStorageSummary: @Composable ApplicationInfo.() -> State<String> = {
        getStorageSize()
    }
) : AppListModel<AppRecordWithSize> {
    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        appListFlow.mapItem {
            AppRecordWithSize(it, it.calculateSizeBytes(context) ?: 0L)
        }

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<AppRecordWithSize>>
    ): Flow<List<AppRecordWithSize>> = recordListFlow.filterItem { type.filter(it) }

    @Composable
    override fun getSummary(option: Int, record: AppRecordWithSize): () -> String {
        val storageSummary by record.app.getStorageSummary()
        return { storageSummary }
    }

    @Composable
    override fun AppListItemModel<AppRecordWithSize>.AppItem() {
        AppListItem(onClick = AppInfoSettingsProvider.navigator(app = record.app))
    }

    override fun getComparator(option: Int) = compareByDescending<AppEntry<AppRecordWithSize>> {
        it.record.size
    }.then(super.getComparator(option))
}
