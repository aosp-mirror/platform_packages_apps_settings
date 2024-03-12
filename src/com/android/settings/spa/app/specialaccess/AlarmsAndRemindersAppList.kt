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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.compat.CompatChanges
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.PowerExemptionManager
import androidx.compose.runtime.Composable
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.R
import com.android.settingslib.spa.livedata.observeAsCallback
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object AlarmsAndRemindersAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "AlarmsAndReminders"
    override fun createModel(context: Context) = AlarmsAndRemindersAppListModel(context)
}

data class AlarmsAndRemindersAppRecord(
    override val app: ApplicationInfo,
    val isTrumped: Boolean,
    val isChangeable: Boolean,
    var controller: AlarmsAndRemindersController,
) : AppRecord

class AlarmsAndRemindersAppListModel(
    private val context: Context,
    private val packageManagers: IPackageManagers = PackageManagers,
) : TogglePermissionAppListModel<AlarmsAndRemindersAppRecord> {
    override val pageTitleResId = R.string.alarms_and_reminders_title
    override val switchTitleResId = R.string.alarms_and_reminders_switch_title
    override val footerResId = R.string.alarms_and_reminders_footer_title
    override val enhancedConfirmationKey: String = AppOpsManager.OPSTR_SCHEDULE_EXACT_ALARM

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.map { userId ->
            PackageManagers.getAppOpPermissionPackages(userId, PERMISSION)
        }.combine(appListFlow) { packageNames, appList ->
            appList.map { app ->
                createRecord(app = app, hasRequestPermission = app.packageName in packageNames)
            }
        }

    override fun transformItem(app: ApplicationInfo) = with(packageManagers) {
        createRecord(app = app, hasRequestPermission = app.hasRequestPermission(PERMISSION))
    }

    override fun filter(
        userIdFlow: Flow<Int>,
        recordListFlow: Flow<List<AlarmsAndRemindersAppRecord>>,
    ) = recordListFlow.map { recordList ->
        recordList.filter { it.isChangeable }
    }

    @Composable
    override fun isAllowed(record: AlarmsAndRemindersAppRecord): () -> Boolean? = when {
        record.isTrumped -> ({ true })
        else -> record.controller.isAllowed.observeAsCallback()
    }

    override fun isChangeable(record: AlarmsAndRemindersAppRecord) = record.isChangeable

    override fun setAllowed(record: AlarmsAndRemindersAppRecord, newAllowed: Boolean) {
        record.controller.setAllowed(newAllowed)
        logPermissionChange(newAllowed)
    }

    private fun logPermissionChange(newAllowed: Boolean) {
        featureFactory.metricsFeatureProvider.action(
            SettingsEnums.PAGE_UNKNOWN,
            SettingsEnums.ACTION_ALARMS_AND_REMINDERS_TOGGLE,
            SettingsEnums.ALARMS_AND_REMINDERS,
            "",
            if (newAllowed) 1 else 0
        )
    }

    private fun createRecord(
        app: ApplicationInfo,
        hasRequestPermission: Boolean,
    ): AlarmsAndRemindersAppRecord {
        val hasRequestSeaPermission = hasRequestPermission && app.isSeaEnabled()
        val isTrumped = hasRequestSeaPermission && app.isTrumped()
        return AlarmsAndRemindersAppRecord(
            app = app,
            isTrumped = isTrumped,
            isChangeable = hasRequestPermission && !isTrumped,
            controller = AlarmsAndRemindersController(context, app),
        )
    }

    /**
     * If trumped, this app will be treated as allowed, and the toggle is not changeable by user.
     */
    private fun ApplicationInfo.isTrumped(): Boolean = with(packageManagers) {
        val hasRequestUseExactAlarm = hasRequestPermission(Manifest.permission.USE_EXACT_ALARM) &&
            CompatChanges.isChangeEnabled(
                AlarmManager.ENABLE_USE_EXACT_ALARM, packageName, userHandle,
            )
        val isPowerAllowListed = context.getSystemService(PowerExemptionManager::class.java)
            ?.isAllowListed(packageName, true) ?: false
        return hasRequestUseExactAlarm || isPowerAllowListed
    }

    companion object {
        private const val PERMISSION: String = Manifest.permission.SCHEDULE_EXACT_ALARM

        /** Checks whether [Manifest.permission.SCHEDULE_EXACT_ALARM] is enabled. */
        private fun ApplicationInfo.isSeaEnabled(): Boolean =
            CompatChanges.isChangeEnabled(
                AlarmManager.REQUIRE_EXACT_ALARM_PERMISSION, packageName, userHandle,
            )
    }
}
