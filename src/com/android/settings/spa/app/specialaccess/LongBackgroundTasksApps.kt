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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object LongBackgroundTasksAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "LongBackgroundTasksApps"
    override fun createModel(context: Context) = LongBackgroundTasksAppsListModel(context)
}

class LongBackgroundTasksAppsListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.long_background_tasks_title
    override val switchTitleResId = R.string.long_background_tasks_switch_title
    override val footerResId = R.string.long_background_tasks_footer_title
    override val appOp = AppOpsManager.OP_RUN_USER_INITIATED_JOBS
    override val permission = Manifest.permission.RUN_USER_INITIATED_JOBS
    override val setModeByUid = true

    override fun setAllowed(record: AppOpPermissionRecord, newAllowed: Boolean) {
        super.setAllowed(record, newAllowed)
        logPermissionChange(newAllowed)
    }

    private fun logPermissionChange(newAllowed: Boolean) {
        featureFactory.metricsFeatureProvider.action(
            context,
            SettingsEnums.ACTION_LONG_BACKGROUND_TASKS_TOGGLE,
            if (newAllowed) 1 else 0
        )
    }
}