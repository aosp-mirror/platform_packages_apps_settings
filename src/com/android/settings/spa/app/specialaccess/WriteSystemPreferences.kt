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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object WriteSystemPreferencesAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "WriteSystemPreferences"
    override fun createModel(context: Context) = WriteSystemPreferencesAppListModel(context)
}

class WriteSystemPreferencesAppListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.write_system_preferences_page_title
    override val switchTitleResId = R.string.write_system_preferences_switch_title
    override val footerResId = R.string.write_system_preferences_footer_description
    override val appOps = AppOps(
        op = AppOpsManager.OP_WRITE_SYSTEM_PREFERENCES,
        setModeByUid = true,
    )
    override val permission = Manifest.permission.WRITE_SYSTEM_PREFERENCES

    override fun filter(userIdFlow: Flow<Int>, recordListFlow: Flow<List<AppOpPermissionRecord>>):
            Flow<List<AppOpPermissionRecord>> {
        return super.filter(userIdFlow, recordListFlow).map { recordList ->
            recordList.filter { app ->
                // Only apps that have READ_SYSTEM_PREFERENCES can utilize WRITE_SYSTEM_PREFERENCES.
                // This write permission is (currently) non-functionality without the corresponding
                // read permission, and the read permission can only be granted via pre-grant or
                // role. As such, we don't show apps that are "requesting" this permission without
                // holding the read permission, as it would create confusion and not provide them
                // any functionality.
                with (PackageManagers) {
                    app.app.hasGrantPermission(Manifest.permission.READ_SYSTEM_PREFERENCES)
                }
            }
        }
    }
}