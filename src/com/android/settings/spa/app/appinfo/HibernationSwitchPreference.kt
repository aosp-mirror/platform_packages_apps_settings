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

import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.MODE_DEFAULT
import android.app.AppOpsManager.MODE_IGNORED
import android.app.AppOpsManager.OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.Flags as PmFlags
import android.os.Build
import android.os.SystemProperties
import android.permission.PermissionControllerManager.HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM
import android.permission.PermissionControllerManager.HIBERNATION_ELIGIBILITY_UNKNOWN
import android.provider.DeviceConfig
import android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED
import com.android.settings.Utils.PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS
import com.android.settings.flags.Flags
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spaprivileged.framework.common.appHibernationManager
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.framework.common.permissionControllerManager
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@Composable
fun HibernationSwitchPreference(
    app: ApplicationInfo,
    isHibernationSwitchEnabledStateFlow: MutableStateFlow<Boolean>
) {
    val context = LocalContext.current
    val presenter = remember(app) { HibernationSwitchPresenter(context, app) }
    if (!presenter.isAvailable()) return

    val isEligibleState by presenter.isEligibleFlow.collectAsStateWithLifecycle(initialValue = false)
    val isCheckedState = presenter.isCheckedFlow.collectAsStateWithLifecycle(initialValue = null)
    SwitchPreference(remember {
        object : SwitchPreferenceModel {
            override val title =
                if (isArchivingEnabled())
                    context.getString(R.string.unused_apps_switch_v2)
                else
                    context.getString(R.string.unused_apps_switch)
            override val summary = {
                if (isArchivingEnabled())
                    context.getString(R.string.unused_apps_switch_summary_v2)
                else
                    context.getString(R.string.unused_apps_switch_summary)
            }
            override val changeable = { isEligibleState }
            override val checked = {
                val result = if (changeable()) isCheckedState.value else false
                result.also { isChecked ->
                    isChecked?.let {
                        isHibernationSwitchEnabledStateFlow.value = it
                    }
                }
            }
            override val onCheckedChange = presenter::onCheckedChange
        }
    })
}

private fun isArchivingEnabled() =
        PmFlags.archiving() || SystemProperties.getBoolean("pm.archiving.enabled", false)
                || Flags.appArchiving()

private class HibernationSwitchPresenter(context: Context, private val app: ApplicationInfo) {
    private val appOpsManager = context.appOpsManager
    private val permissionControllerManager =
        context.asUser(app.userHandle).permissionControllerManager
    private val appHibernationManager = context.appHibernationManager
    private val executor = Dispatchers.IO.asExecutor()

    fun isAvailable() =
        DeviceConfig.getBoolean(NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED, true)

    val isEligibleFlow = flow {
        if (app.isArchived) {
            emit(false)
            return@flow
        }
        val eligibility = getEligibility()
        emit(
            eligibility != HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM &&
                eligibility != HIBERNATION_ELIGIBILITY_UNKNOWN
        )
    }

    private suspend fun getEligibility(): Int = suspendCoroutine { continuation ->
        permissionControllerManager.getHibernationEligibility(app.packageName, executor) {
            continuation.resume(it)
        }
    }

    private val isChecked = OverridableFlow(flow {
        emit(!isExempt())
    })

    val isCheckedFlow = isChecked.flow

    private suspend fun isExempt(): Boolean = withContext(Dispatchers.IO) {
        val mode = appOpsManager.checkOpNoThrow(
            OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, app.uid, app.packageName
        )
        if (mode == MODE_DEFAULT) isExemptByDefault() else mode != MODE_ALLOWED
    }

    private fun isExemptByDefault() =
        !hibernationTargetsPreSApps() && app.targetSdkVersion <= Build.VERSION_CODES.Q

    private fun hibernationTargetsPreSApps() = DeviceConfig.getBoolean(
        NAMESPACE_APP_HIBERNATION, PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS, false
    )

    fun onCheckedChange(newChecked: Boolean) {
        try {
            appOpsManager.setUidMode(
                OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                app.uid,
                if (newChecked) MODE_ALLOWED else MODE_IGNORED,
            )
            if (!newChecked) {
                appHibernationManager.setHibernatingForUser(app.packageName, false)
                appHibernationManager.setHibernatingGlobally(app.packageName, false)
            }
            isChecked.override(newChecked)
        } catch (_: RuntimeException) {
        }
    }
}
