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

import android.app.settings.SettingsEnums
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.UserManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.isActiveAdmin
import com.android.settingslib.spaprivileged.model.app.userId

class AppForceStopButton(
    private val packageInfoPresenter: PackageInfoPresenter,
) {
    private val context = packageInfoPresenter.context
    private val appButtonRepository = AppButtonRepository(context)
    private val packageManager = context.packageManager

    private var openConfirmDialog by mutableStateOf(false)

    fun getActionButton(app: ApplicationInfo): ActionButton {
        return ActionButton(
            text = context.getString(R.string.force_stop),
            imageVector = Icons.Outlined.WarningAmber,
            enabled = isForceStopButtonEnable(app),
        ) { onForceStopButtonClicked(app) }
    }

    /**
     * Gets whether a package can be force stopped.
     */
    private fun isForceStopButtonEnable(app: ApplicationInfo): Boolean = when {
        // User can't force stop device admin.
        app.isActiveAdmin(context) -> false

        appButtonRepository.isDisallowControl(app) -> false

        // If the app isn't explicitly stopped, then always show the force stop button.
        else -> !app.hasFlag(ApplicationInfo.FLAG_STOPPED)
    }

    private fun onForceStopButtonClicked(app: ApplicationInfo) {
        packageInfoPresenter.logAction(SettingsEnums.ACTION_APP_INFO_FORCE_STOP)
        getAdminRestriction(app)?.let { admin ->
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin)
            return
        }
        openConfirmDialog = true
    }

    private fun getAdminRestriction(app: ApplicationInfo): EnforcedAdmin? = when {
        packageManager.isPackageStateProtected(app.packageName, app.userId) -> {
            RestrictedLockUtilsInternal.getDeviceOwner(context)
        }

        else -> RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
            context, UserManager.DISALLOW_APPS_CONTROL, app.userId
        )
    }

    @Composable
    fun ForceStopConfirmDialog() {
        if (!openConfirmDialog) return
        AlertDialog(
            onDismissRequest = { openConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        openConfirmDialog = false
                        packageInfoPresenter.forceStop()
                    },
                ) {
                    Text(stringResource(R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { openConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.force_stop_dlg_title))
            },
            text = {
                Text(stringResource(R.string.force_stop_dlg_text))
            },
        )
    }
}
