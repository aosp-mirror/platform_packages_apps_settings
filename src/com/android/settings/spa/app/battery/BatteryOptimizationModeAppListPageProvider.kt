/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.spa.app.battery

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.SubSettingLauncher
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail
import com.android.settings.fuelgauge.BatteryOptimizeUtils
import com.android.settings.spa.app.AppRecordWithSize
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settings.spa.app.rememberResetAppDialogPresenter
import com.android.settingslib.fuelgauge.PowerAllowlistBackend
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
import com.android.settingslib.spaprivileged.model.app.installed
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import kotlinx.coroutines.flow.Flow

object BatteryOptimizationModeAppListPageProvider : SettingsPageProvider {
    override val name = "BatteryOptimizationModeAppList"
    private val owner = createSettingsPage()

    @Composable
    override fun Page(arguments: Bundle?) {
        BatteryOptimizationModeAppList()
    }

    fun buildInjectEntry() = SettingsEntryBuilder
        .createInject(owner)
        .setSearchDataFn { null }
        .setUiLayoutFn {
            Preference(object : PreferenceModel {
                override val title = stringResource(R.string.app_battery_usage_title)
                override val onClick = navigator(name)
            })
        }
}

@Composable
fun BatteryOptimizationModeAppList(
    appList: @Composable AppListInput<AppRecordWithSize>.() -> Unit = { AppList() },
) {
    AppListPage(
        title = stringResource(R.string.app_battery_usage_title),
        listModel = rememberContext(::BatteryOptimizationModeAppListModel),
        appList = appList,
    )
}

class BatteryOptimizationModeAppListModel(
    private val context: Context,
) : AppListModel<AppRecordWithSize> {

    override fun getSpinnerOptions(recordList: List<AppRecordWithSize>): List<SpinnerOption> =
        OptimizationModeSpinnerItem.entries.map {
            SpinnerOption(
                id = it.ordinal,
                text = context.getString(it.stringResId),
            )
        }

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        appListFlow.mapItem(::AppRecordWithSize)

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<AppRecordWithSize>>,
    ): Flow<List<AppRecordWithSize>> {
        PowerAllowlistBackend.getInstance(context).refreshList()
        return recordListFlow.filterItem {
            val appOptimizationMode = BatteryOptimizeUtils(context, it.app.uid, it.app.packageName)
                .getAppOptimizationMode(/* refreshList */ false);
            when (OptimizationModeSpinnerItem.entries.getOrNull(option)) {
                OptimizationModeSpinnerItem.Restricted ->
                    appOptimizationMode == BatteryOptimizeUtils.MODE_RESTRICTED
                OptimizationModeSpinnerItem.Optimized ->
                    appOptimizationMode == BatteryOptimizeUtils.MODE_OPTIMIZED
                OptimizationModeSpinnerItem.Unrestricted ->
                    appOptimizationMode == BatteryOptimizeUtils.MODE_UNRESTRICTED
                else -> (true)
            }
        }
    }

    @Composable
    override fun getSummary(option: Int, record: AppRecordWithSize): () -> String = {
        var summary = String()
        val app = record.app
        when {
            !app.installed && !app.isArchived -> {
                summary += context.getString(R.string.not_installed)
            }

            !app.enabled -> {
                summary += context.getString(com.android.settingslib.R.string.disabled)
            }
        }
        summary
    }

    @Composable
    override fun AppListItemModel<AppRecordWithSize>.AppItem() {
        AppListItem(onClick = {
            val args = bundleOf(
                AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME to record.app.packageName,
                AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT to Utils.formatPercentage(0),
                AdvancedPowerUsageDetail.EXTRA_UID to record.app.uid,
            )
            SubSettingLauncher(context)
                .setDestination(AdvancedPowerUsageDetail::class.java.name)
                .setTitleRes(R.string.battery_details_title)
                .setArguments(args)
                .setUserHandle(record.app.userHandle)
                .setSourceMetricsCategory(AppInfoSettingsProvider.METRICS_CATEGORY)
                .launch()
        })
    }
}

private enum class OptimizationModeSpinnerItem(val stringResId: Int) {
    All(R.string.filter_all_apps),
    Restricted(R.string.filter_battery_restricted_title),
    Optimized(R.string.filter_battery_optimized_title),
    Unrestricted(R.string.filter_battery_unrestricted_title);
}
