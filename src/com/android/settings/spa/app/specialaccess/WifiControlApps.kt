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
import android.app.AppOpsManager.MODE_IGNORED
import android.content.Context
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object WifiControlAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "WifiControl"
    override fun createModel(context: Context) = WifiControlAppListModel(context)
}

class WifiControlAppListModel(
    private val context: Context,
    private val packageManagers: IPackageManagers = PackageManagers
) : AppOpPermissionListModel(context, packageManagers) {
    override val pageTitleResId = R.string.change_wifi_state_title
    override val switchTitleResId = R.string.change_wifi_state_app_detail_switch
    override val footerResId = R.string.change_wifi_state_app_detail_summary

    override val appOp = AppOpsManager.OP_CHANGE_WIFI_STATE
    override val permission = Manifest.permission.CHANGE_WIFI_STATE

    /** NETWORK_SETTINGS permission trumps CHANGE_WIFI_CONFIG. */
    override val broaderPermission = Manifest.permission.NETWORK_SETTINGS
    override val permissionHasAppopFlag = false
    override val modeForNotAllowed = MODE_IGNORED
}
