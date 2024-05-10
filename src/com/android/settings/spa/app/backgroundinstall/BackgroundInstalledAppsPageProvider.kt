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

package com.android.settings.spa.app.backgroundinstall

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.IBackgroundInstallControlService
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ParceledListSlice
import android.os.Bundle
import android.os.ServiceManager
import android.provider.DeviceConfig
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settings.spa.app.startUninstallActivity
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.util.asyncMap
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListButtonItem
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

private const val KEY_GROUPING_MONTH = "key_grouping_by_month"
const val DEFAULT_GROUPING_MONTH_VALUE = 6
const val MONTH_IN_MILLIS = 2629800000L
const val KEY_BIC_UI_ENABLED = "key_bic_ui_enabled"
const val BACKGROUND_INSTALL_CONTROL_FLAG = PackageManager.MATCH_ALL.toLong()

object BackgroundInstalledAppsPageProvider : SettingsPageProvider {
    override val name = "BackgroundInstalledAppsPage"
    private val owner = createSettingsPage()
    private var backgroundInstallService = IBackgroundInstallControlService.Stub.asInterface(
        ServiceManager.getService(Context.BACKGROUND_INSTALL_CONTROL_SERVICE))
    private var featureIsDisabled = featureIsDisabled()

    @Composable
    override fun Page(arguments: Bundle?) {
        if(featureIsDisabled) return
        BackgroundInstalledAppList()
    }

    @Composable
    fun EntryItem() {
        if(featureIsDisabled) return
        val summary by generatePreferenceSummary()
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.background_install_title)
            override val summary = { summary }
            override val onClick = navigator(name)
        })
    }

    fun buildInjectEntry() = SettingsEntryBuilder
        .createInject(owner)
        .setSearchDataFn { null }
        .setUiLayoutFn { EntryItem() }

    private fun featureIsDisabled() = !DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
        KEY_BIC_UI_ENABLED, false)

    @Composable
    private fun generatePreferenceSummary(): State<String> {
        val context = LocalContext.current
        return produceState(initialValue = stringResource(R.string.summary_placeholder)) {
            withContext(Dispatchers.IO) {
                val backgroundInstalledApps =
                    backgroundInstallService.getBackgroundInstalledPackages(
                        BACKGROUND_INSTALL_CONTROL_FLAG, context.user.identifier
                    ).list.size
                value = context.formatString(
                    R.string.background_install_preference_summary,
                    "count" to backgroundInstalledApps
                )
            }
        }
    }

    @VisibleForTesting
    fun setDisableFeature(disableFeature : Boolean): BackgroundInstalledAppsPageProvider {
        featureIsDisabled = disableFeature
        return this
    }

    @VisibleForTesting
    fun setBackgroundInstallControlService(bic: IBackgroundInstallControlService):
        BackgroundInstalledAppsPageProvider {
        backgroundInstallService = bic
        return this
    }
}

@Composable
fun BackgroundInstalledAppList(
    appList: @Composable AppListInput<BackgroundInstalledAppListWithGroupingAppRecord>.() -> Unit
    = { AppList() },
) {
    AppListPage(
            title = stringResource(R.string.background_install_title),
            listModel = rememberContext(::BackgroundInstalledAppsWithGroupingListModel),
            noItemMessage = stringResource(R.string.background_install_feature_list_no_entry),
            appList = appList,
            header = {
                Box(Modifier.padding(SettingsDimension.itemPadding)) {
                    SettingsBody(stringResource(R.string.background_install_summary))
                }
            }
    )
}

data class BackgroundInstalledAppListWithGroupingAppRecord(
    override val app: ApplicationInfo,
    val dateOfInstall: Long,
) : AppRecord

class BackgroundInstalledAppsWithGroupingListModel(private val context: Context)
    : AppListModel<BackgroundInstalledAppListWithGroupingAppRecord> {

    companion object {
        private const val tag = "AppListModel<BackgroundInstalledAppListWithGroupingAppRecord>"
    }

    private var backgroundInstallService = IBackgroundInstallControlService.Stub.asInterface(
        ServiceManager.getService(Context.BACKGROUND_INSTALL_CONTROL_SERVICE))

    @VisibleForTesting
    fun setBackgroundInstallControlService(bic: IBackgroundInstallControlService) {
        backgroundInstallService = bic
    }
    @Composable
    override fun AppListItemModel<BackgroundInstalledAppListWithGroupingAppRecord>.AppItem() {
        val context = LocalContext.current
        val app = record.app
        AppListButtonItem(
            onClick = AppInfoSettingsProvider.navigator(app = app),
            onButtonClick = { context.startUninstallActivity(app.packageName, app.userHandle) },
            buttonIcon = Icons.Outlined.Delete,
            buttonIconDescription = stringResource(
                R.string.background_install_uninstall_button_description))
    }

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.combine(appListFlow) { userId, appList ->
            appList.asyncMap { app ->
                BackgroundInstalledAppListWithGroupingAppRecord(
                        app = app,
                        dateOfInstall = context.packageManager.getPackageInfoAsUser(app.packageName,
                                PackageManager.PackageInfoFlags.of(0), userId).firstInstallTime
                )
            }
        }

    override fun filter(
            userIdFlow: Flow<Int>,
            option: Int,
            recordListFlow: Flow<List<BackgroundInstalledAppListWithGroupingAppRecord>>
    ): Flow<List<BackgroundInstalledAppListWithGroupingAppRecord>> {
        if(backgroundInstallService == null) {
            Log.e(tag, "Failed to retrieve Background Install Control Service")
            return flowOf()
        }
        return userIdFlow.combine(recordListFlow) { userId, recordList ->
            @Suppress("UNCHECKED_CAST")
            val appList = (backgroundInstallService.getBackgroundInstalledPackages(
                PackageManager.MATCH_ALL.toLong(), userId) as ParceledListSlice<PackageInfo>).list
            val appNameList = appList.map { it.packageName }
            recordList.filter { record -> record.app.packageName in appNameList }
        }
    }

    override fun getComparator(
            option: Int,
    ): Comparator<AppEntry<BackgroundInstalledAppListWithGroupingAppRecord>> =
            compareByDescending { it.record.dateOfInstall }

    override fun getGroupTitle(option: Int, record: BackgroundInstalledAppListWithGroupingAppRecord)
    : String {
        val groupByMonth = getGroupSeparationByMonth()
        return when (record.dateOfInstall > System.currentTimeMillis()
            - (groupByMonth * MONTH_IN_MILLIS)) {
            true -> context.formatString(R.string.background_install_before, "count" to groupByMonth)
            else -> context.formatString(R.string.background_install_after, "count" to groupByMonth)
        }
    }
}

private fun getGroupSeparationByMonth(): Int {
    val month = DeviceConfig.getProperty(DeviceConfig.NAMESPACE_SETTINGS_UI, KEY_GROUPING_MONTH)
    return try {
        if (month.isNullOrBlank()) {
            DEFAULT_GROUPING_MONTH_VALUE
        } else {
            month.toInt()
        }
    } catch (e: Exception) {
        Log.d(
            BackgroundInstalledAppsPageProvider.name, "Error parsing list grouping value: " +
            "${e.message} falling back to default value: $DEFAULT_GROUPING_MONTH_VALUE")
        DEFAULT_GROUPING_MONTH_VALUE
    }
}
