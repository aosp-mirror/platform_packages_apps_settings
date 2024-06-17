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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.ui.SpinnerOption
import com.android.settingslib.spaprivileged.framework.compose.getPlaceholder
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.installed
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.android.settingslib.spaprivileged.template.app.getStorageSize
import kotlinx.coroutines.flow.Flow

object AllAppListPageProvider : SettingsPageProvider {
    override val name = "AllAppList"
    private val owner = createSettingsPage()

    @Composable
    override fun Page(arguments: Bundle?) {
        AllAppListPage()
    }

    fun buildInjectEntry() = SettingsEntryBuilder
        .createInject(owner)
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
        listModel = rememberContext(::AllAppListModel),
        showInstantApps = true,
        matchAnyUserForAdmin = true,
        moreOptions = { ResetAppPreferences(resetAppDialogPresenter::open) },
        appList = appList,
    )
}

data class AppRecordWithSize(
    override val app: ApplicationInfo,
) : AppRecord

class AllAppListModel(
    private val context: Context,
    private val getStorageSummary: @Composable ApplicationInfo.() -> State<String> = {
        getStorageSize()
    },
) : AppListModel<AppRecordWithSize> {

    override fun getSpinnerOptions(recordList: List<AppRecordWithSize>): List<SpinnerOption> {
        val hasDisabled = recordList.any(isDisabled)
        val hasInstant = recordList.any(isInstant)
        if (!hasDisabled && !hasInstant) return emptyList()
        val options = mutableListOf(SpinnerItem.All, SpinnerItem.Enabled)
        if (hasDisabled) options += SpinnerItem.Disabled
        if (hasInstant) options += SpinnerItem.Instant
        return options.map {
            SpinnerOption(
                id = it.ordinal,
                text = context.getString(it.stringResId),
            )
        }
    }

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        appListFlow.mapItem(::AppRecordWithSize)

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<AppRecordWithSize>>,
    ): Flow<List<AppRecordWithSize>> = recordListFlow.filterItem(
        when (SpinnerItem.entries.getOrNull(option)) {
            SpinnerItem.Enabled -> ({ it.app.enabled && !it.app.isInstantApp })
            SpinnerItem.Disabled -> isDisabled
            SpinnerItem.Instant -> isInstant
            else -> ({ true })
        }
    )

    private val isDisabled: (AppRecordWithSize) -> Boolean =
        { !it.app.enabled && !it.app.isInstantApp }

    private val isInstant: (AppRecordWithSize) -> Boolean = { it.app.isInstantApp }

    @Composable
    override fun getSummary(option: Int, record: AppRecordWithSize): () -> String {
        val storageSummary = record.app.getStorageSummary()
        return {
            val summaryList = mutableListOf<String>()
            val storageSummaryValue = storageSummary.value
            if (storageSummaryValue.isNotBlank()) {
                summaryList += storageSummaryValue
            }
            when {
                !record.app.installed && !record.app.isArchived -> {
                    summaryList += context.getString(R.string.not_installed)
                }

                isDisabled(record) -> {
                    summaryList += context.getString(com.android.settingslib.R.string.disabled)
                }
            }
            summaryList.joinToString(separator = System.lineSeparator())
                .ifEmpty { context.getPlaceholder() } // Use placeholder to reduce flaky
        }
    }

    @Composable
    override fun AppListItemModel<AppRecordWithSize>.AppItem() {
        AppListItem(onClick = AppInfoSettingsProvider.navigator(app = record.app))
    }
}

private enum class SpinnerItem(val stringResId: Int) {
    All(R.string.filter_all_apps),
    Enabled(R.string.filter_enabled_apps),
    Disabled(R.string.filter_apps_disabled),
    Instant(R.string.filter_instant_apps);
}
