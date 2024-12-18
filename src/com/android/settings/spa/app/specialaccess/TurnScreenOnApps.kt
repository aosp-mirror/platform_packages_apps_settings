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
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object TurnScreenOnAppsAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "TurnScreenOnApps"
    override fun createModel(context: Context) = TurnScreenOnAppsListModel(context)
}

class TurnScreenOnAppsListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = com.android.settingslib.R.string.turn_screen_on_title
    override val switchTitleResId = com.android.settingslib.R.string.allow_turn_screen_on
    override val footerResId = com.android.settingslib.R.string.allow_turn_screen_on_description
    override val appOps = AppOps(
        op = AppOpsManager.OP_TURN_SCREEN_ON,
        setModeByUid = true,
    )
    override val permission = Manifest.permission.TURN_SCREEN_ON

    override fun setAllowed(record: AppOpPermissionRecord, newAllowed: Boolean) {
        super.setAllowed(record, newAllowed)
        logPermissionChange(newAllowed)
    }

    private fun logPermissionChange(newAllowed: Boolean) {
        featureFactory.metricsFeatureProvider.action(
            context,
            SettingsEnums.SETTINGS_MANAGE_TURN_SCREEN_ON,
            if (newAllowed) 1 else 0
        )
    }
}