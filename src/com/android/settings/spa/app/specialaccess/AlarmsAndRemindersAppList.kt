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
import android.app.compat.CompatChanges
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasRequestPermission
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
    val isChangeable: Boolean,
    var controller: AlarmsAndRemindersController,
) : AppRecord

class AlarmsAndRemindersAppListModel(
    private val context: Context,
) : TogglePermissionAppListModel<AlarmsAndRemindersAppRecord> {
    override val pageTitleResId = R.string.alarms_and_reminders_title
    override val switchTitleResId = R.string.alarms_and_reminders_switch_title
    override val footerResId = R.string.alarms_and_reminders_footer_title

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.map { userId ->
            PackageManagers.getAppOpPermissionPackages(userId, PERMISSION)
        }.combine(appListFlow) { packageNames, appList ->
            appList.map { app ->
                createRecord(app = app, hasRequestPermission = app.packageName in packageNames)
            }
        }

    override fun transformItem(app: ApplicationInfo) =
        createRecord(app = app, hasRequestPermission = app.hasRequestPermission(PERMISSION))

    override fun filter(
        userIdFlow: Flow<Int>,
        recordListFlow: Flow<List<AlarmsAndRemindersAppRecord>>,
    ) = recordListFlow.map { recordList ->
        recordList.filter { it.isChangeable }
    }

    @Composable
    override fun isAllowed(record: AlarmsAndRemindersAppRecord) =
        record.controller.isAllowed.observeAsState()

    override fun isChangeable(record: AlarmsAndRemindersAppRecord) = record.isChangeable

    override fun setAllowed(record: AlarmsAndRemindersAppRecord, newAllowed: Boolean) {
        record.controller.setAllowed(newAllowed)
    }

    private fun createRecord(app: ApplicationInfo, hasRequestPermission: Boolean) =
        AlarmsAndRemindersAppRecord(
            app = app,
            isChangeable = hasRequestPermission && app.isChangeEnabled(),
            controller = AlarmsAndRemindersController(context, app),
        )

    companion object {
        private const val PERMISSION: String = Manifest.permission.SCHEDULE_EXACT_ALARM

        private fun ApplicationInfo.isChangeEnabled(): Boolean =
            CompatChanges.isChangeEnabled(
                AlarmManager.REQUIRE_EXACT_ALARM_PERMISSION, packageName, userHandle,
            )
    }
}
