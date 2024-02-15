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

package com.android.settings.spa.development.compat

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import com.android.settings.core.SubSettingLauncher
import com.android.settings.development.compat.PlatformCompatDashboard
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import kotlinx.coroutines.flow.Flow

data class PlatformCompatAppRecord(
    override val app: ApplicationInfo,
) : AppRecord

class PlatformCompatAppListModel(
    private val context: Context,
) : AppListModel<PlatformCompatAppRecord> {

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        appListFlow.mapItem(::PlatformCompatAppRecord)

    override fun filter(
        userIdFlow: Flow<Int>, option: Int, recordListFlow: Flow<List<PlatformCompatAppRecord>>,
    ) = recordListFlow.filterItem { record ->
        Build.IS_DEBUGGABLE || record.app.hasFlag(ApplicationInfo.FLAG_DEBUGGABLE)
    }

    @Composable
    override fun getSummary(option: Int, record: PlatformCompatAppRecord): () -> String = {
        record.app.packageName
    }

    @Composable
    override fun AppListItemModel<PlatformCompatAppRecord>.AppItem() {
        AppListItem { navigateToAppCompat(app = record.app) }
    }

    private fun navigateToAppCompat(app: ApplicationInfo) {
        SubSettingLauncher(context)
            .setDestination(PlatformCompatDashboard::class.qualifiedName)
            .setSourceMetricsCategory(SettingsEnums.DEVELOPMENT)
            .setArguments(bundleOf(PlatformCompatDashboard.COMPAT_APP to app.packageName))
            .setUserHandle(app.userHandle)
            .launch()
    }
}
