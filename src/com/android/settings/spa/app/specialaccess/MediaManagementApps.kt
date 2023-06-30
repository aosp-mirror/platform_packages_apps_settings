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
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object MediaManagementAppsAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "MediaManagementApps"
    override fun createModel(context: Context) = MediaManagementAppsListModel(context)
}

class MediaManagementAppsListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.media_management_apps_title
    override val switchTitleResId = R.string.media_management_apps_toggle_label
    override val footerResId = R.string.media_management_apps_description
    override val appOp = AppOpsManager.OP_MANAGE_MEDIA
    override val permission = Manifest.permission.MANAGE_MEDIA
    override val setModeByUid = true

    override fun setAllowed(record: AppOpPermissionRecord, newAllowed: Boolean) {
        super.setAllowed(record, newAllowed)
        logPermissionChange(newAllowed)
    }

    private fun logPermissionChange(newAllowed: Boolean) {
        featureFactory.metricsFeatureProvider.action(
            SettingsEnums.PAGE_UNKNOWN,
            SettingsEnums.ACTION_MEDIA_MANAGEMENT_APPS_TOGGLE,
            SettingsEnums.MEDIA_MANAGEMENT_APPS,
            "",
            if (newAllowed) 1 else 0
        )
    }
}