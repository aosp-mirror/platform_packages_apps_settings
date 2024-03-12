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

package com.android.settings.spa.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.UserHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.applications.AppLocaleUtil
import com.android.settings.applications.appinfo.AppLocaleDetails
import com.android.settings.localepicker.AppLocalePickerActivity
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

data class AppLanguagesRecord(
    override val app: ApplicationInfo,
    val isAppLocaleSupported: Boolean,
) : AppRecord

class AppLanguagesListModel(private val context: Context) : AppListModel<AppLanguagesRecord> {
    private val packageManager = context.packageManager

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.map { userId ->
            userId to packageManager.queryIntentActivitiesAsUser(
                AppLocaleUtil.LAUNCHER_ENTRY_INTENT,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                userId,
            )
        }.combine(appListFlow) { (userId, resolveInfos), appList ->
            val userContext = context.asUser(UserHandle.of(userId))
            appList.map { app ->
                AppLanguagesRecord(
                    app = app,
                    isAppLocaleSupported = AppLocaleUtil.canDisplayLocaleUi(
                        userContext, app, resolveInfos
                    ),
                )
            }
        }

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<AppLanguagesRecord>>,
    ) = recordListFlow.filterItem { it.isAppLocaleSupported }

    @Composable
    override fun getSummary(option: Int, record: AppLanguagesRecord): () -> String {
        val summary by remember(record.app) {
            flow {
                emit(getSummary(record.app))
            }.flowOn(Dispatchers.IO)
        }.collectAsStateWithLifecycle(initialValue = stringResource(R.string.summary_placeholder))
        return { summary }
    }

    private fun getSummary(app: ApplicationInfo): String =
        AppLocaleDetails.getSummary(context, app).toString()

    @Composable
    override fun AppListItemModel<AppLanguagesRecord>.AppItem() {
        AppListItem {
            val intent = Intent(context, AppLocalePickerActivity::class.java).apply {
                data = Uri.parse("package:${record.app.packageName}")
            }
            context.startActivityAsUser(intent, record.app.userHandle)
        }
    }
}
