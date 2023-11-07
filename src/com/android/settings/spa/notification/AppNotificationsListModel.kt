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

package com.android.settings.spa.notification

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.notification.app.AppNotificationSettings
import com.android.settings.spa.notification.SpinnerItem.Companion.toSpinnerItem
import com.android.settingslib.spa.framework.util.asyncFilter
import com.android.settingslib.spa.framework.util.asyncForEach
import com.android.settingslib.spa.livedata.observeAsCallback
import com.android.settingslib.spa.widget.ui.SpinnerOption
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListTwoTargetSwitchItem
import com.android.settingslib.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AppNotificationsRecord(
    override val app: ApplicationInfo,
    val sentState: NotificationSentState?,
    val controller: AppNotificationController,
) : AppRecord

class AppNotificationsListModel(
    private val context: Context,
) : AppListModel<AppNotificationsRecord> {
    private val repository = AppNotificationRepository(context)
    private val now = System.currentTimeMillis()

    override fun transform(
        userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>,
    ) = repository.getAggregatedUsageEvents(userIdFlow)
        .combine(appListFlow) { usageEvents, appList ->
            appList.map { app ->
                AppNotificationsRecord(
                    app = app,
                    sentState = usageEvents[app.packageName],
                    controller = AppNotificationController(repository, app),
                )
            }
        }

    override fun filter(
        userIdFlow: Flow<Int>, option: Int, recordListFlow: Flow<List<AppNotificationsRecord>>,
    ) = recordListFlow.map { recordList ->
        recordList.asyncFilter { record ->
            when (option.toSpinnerItem()) {
                SpinnerItem.MostRecent -> record.sentState != null
                SpinnerItem.MostFrequent -> record.sentState != null
                SpinnerItem.TurnedOff -> !record.controller.getEnabled()
                else -> true
            }
        }
    }

    override suspend fun onFirstLoaded(recordList: List<AppNotificationsRecord>): Boolean {
        recordList.asyncForEach { it.controller.getEnabled() }
        return true
    }

    override fun getComparator(option: Int) = when (option.toSpinnerItem()) {
        SpinnerItem.MostRecent -> compareByDescending { it.record.sentState?.lastSent }
        SpinnerItem.MostFrequent -> compareByDescending { it.record.sentState?.sentCount }
        else -> compareBy<AppEntry<AppNotificationsRecord>> { 0 }
    }.then(super.getComparator(option))

    @Composable
    override fun getSummary(option: Int, record: AppNotificationsRecord): (() -> String)? =
        record.sentState?.let {
            when (option.toSpinnerItem()) {
                SpinnerItem.MostRecent -> ({ formatLastSent(it.lastSent) })
                SpinnerItem.MostFrequent -> ({ repository.calculateFrequencySummary(it.sentCount) })
                else -> null
            }
        }

    override fun getSpinnerOptions(recordList: List<AppNotificationsRecord>): List<SpinnerOption> =
        SpinnerItem.entries.map {
            SpinnerOption(
                id = it.ordinal,
                text = context.getString(it.stringResId),
            )
        }

    private fun formatLastSent(lastSent: Long) =
        StringUtil.formatRelativeTime(
            context,
            (now - lastSent).toDouble(),
            true,
            RelativeDateTimeFormatter.Style.LONG,
        ).toString()

    @Composable
    override fun AppListItemModel<AppNotificationsRecord>.AppItem() {
        val changeable by produceState(initialValue = false) {
            withContext(Dispatchers.Default) {
                value = repository.isChangeable(record.app)
            }
        }
        AppListTwoTargetSwitchItem(
            onClick = { navigateToAppNotificationSettings(app = record.app) },
            checked = record.controller.isEnabled.observeAsCallback(),
            changeable = { changeable },
            onCheckedChange = record.controller::setEnabled,
        )
    }

    private fun navigateToAppNotificationSettings(app: ApplicationInfo) {
        AppInfoBase.startAppInfoFragment(
            AppNotificationSettings::class.java,
            context.getString(R.string.notifications_title),
            app,
            context,
            SettingsEnums.MANAGE_APPLICATIONS_NOTIFICATIONS,
        )
    }
}

private enum class SpinnerItem(val stringResId: Int) {
    MostRecent(R.string.sort_order_recent_notification),
    MostFrequent(R.string.sort_order_frequent_notification),
    AllApps(R.string.filter_all_apps),
    TurnedOff(R.string.filter_notif_blocked_apps);

    companion object {
        fun Int.toSpinnerItem(): SpinnerItem = entries[this]
    }
}
