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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.OnPermissionsChangedListener
import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.android.settings.R
import com.android.settingslib.applications.PermissionsSummaryHelper
import com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.model.app.userHandle

data class AppPermissionSummaryState(
    val summary: String,
    val enabled: Boolean,
)

@Composable
fun rememberAppPermissionSummary(app: ApplicationInfo): AppPermissionSummaryLiveData {
    val context = LocalContext.current
    return remember(app) { AppPermissionSummaryLiveData(context, app) }
}

class AppPermissionSummaryLiveData(
    private val context: Context,
    private val app: ApplicationInfo,
) : LiveData<AppPermissionSummaryState>() {
    private val userContext = context.asUser(app.userHandle)
    private val userPackageManager = userContext.packageManager

    private val onPermissionsChangedListener = OnPermissionsChangedListener { uid ->
        if (uid == app.uid) update()
    }

    override fun onActive() {
        userPackageManager.addOnPermissionsChangeListener(onPermissionsChangedListener)
        if (app.isArchived) {
            postValue(noPermissionRequestedState())
        } else {
            update()
        }
    }

    override fun onInactive() {
        userPackageManager.removeOnPermissionsChangeListener(onPermissionsChangedListener)
    }

    private fun update() {
        PermissionsSummaryHelper.getPermissionSummary(
            userContext, app.packageName, permissionsCallback
        )
    }

    private val permissionsCallback = object : PermissionsResultCallback {
        override fun onPermissionSummaryResult(
            requestedPermissionCount: Int,
            additionalGrantedPermissionCount: Int,
            grantedGroupLabels: List<CharSequence>,
        ) {
            if (requestedPermissionCount == 0) {
                postValue(noPermissionRequestedState())
                return
            }
            val labels = getDisplayLabels(additionalGrantedPermissionCount, grantedGroupLabels)
            val summary = if (labels.isNotEmpty()) {
                ListFormatter.getInstance().format(labels)
            } else {
                context.getString(R.string.runtime_permissions_summary_no_permissions_granted)
            }
            postValue(AppPermissionSummaryState(summary = summary, enabled = true))
        }
    }

    private fun noPermissionRequestedState() = AppPermissionSummaryState(
        summary = context.getString(R.string.runtime_permissions_summary_no_permissions_requested),
        enabled = false,
    )

    private fun getDisplayLabels(
        additionalGrantedPermissionCount: Int,
        grantedGroupLabels: List<CharSequence>,
    ): List<CharSequence> = when (additionalGrantedPermissionCount) {
        0 -> grantedGroupLabels
        else -> {
            grantedGroupLabels +
                // N additional permissions.
                context.formatString(
                    R.string.runtime_permissions_additional_count,
                    "count" to additionalGrantedPermissionCount,
                )
        }
    }
}
