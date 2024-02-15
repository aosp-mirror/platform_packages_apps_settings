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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settingslib.applications.PermissionsSummaryHelper
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.model.app.permissionsChangedFlow
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

data class AppPermissionSummaryState(
    val summary: String,
    val enabled: Boolean,
)

@Composable
fun rememberAppPermissionSummary(app: ApplicationInfo): Flow<AppPermissionSummaryState> {
    val context = LocalContext.current
    return remember(app) { AppPermissionSummaryRepository(context, app).flow }
}

class AppPermissionSummaryRepository(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val userContext = context.asUser(app.userHandle)

    val flow = context.permissionsChangedFlow(app)
        .map { getPermissionSummary() }
        .flowOn(Dispatchers.Default)

    private suspend fun getPermissionSummary() = suspendCancellableCoroutine { continuation ->
        PermissionsSummaryHelper.getPermissionSummary(
            userContext,
            app.packageName,
        ) { requestedPermissionCount: Int,
            additionalGrantedPermissionCount: Int,
            grantedGroupLabels: List<CharSequence> ->
            val summaryState = if (requestedPermissionCount == 0) {
                noPermissionRequestedState()
            } else {
                val labels = getDisplayLabels(additionalGrantedPermissionCount, grantedGroupLabels)
                val summary = if (labels.isNotEmpty()) {
                    ListFormatter.getInstance().format(labels)
                } else {
                    context.getString(R.string.runtime_permissions_summary_no_permissions_granted)
                }
                AppPermissionSummaryState(summary = summary, enabled = true)
            }
            continuation.resume(summaryState)
        }
    }

    private fun noPermissionRequestedState() = AppPermissionSummaryState(
        summary = context.getString(R.string.runtime_permissions_summary_no_permissions_requested),
        enabled = false,
    )

    private fun getDisplayLabels(
        additionalGrantedPermissionCount: Int,
        grantedGroupLabels: List<CharSequence>,
    ): List<CharSequence> = if (additionalGrantedPermissionCount == 0) {
        grantedGroupLabels
    } else {
        grantedGroupLabels +
            // N additional permissions.
            context.formatString(
                R.string.runtime_permissions_additional_count,
                "count" to additionalGrantedPermissionCount,
            )
    }
}
