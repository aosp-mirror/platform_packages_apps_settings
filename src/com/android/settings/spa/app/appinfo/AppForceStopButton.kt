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
import android.os.UserManager
import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.AlertDialogPresenter
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spaprivileged.model.app.userId

class AppForceStopButton(
    private val packageInfoPresenter: PackageInfoPresenter,
    private val appForceStopRepository: AppForceStopRepository =
        AppForceStopRepository(packageInfoPresenter),
) {
    private val context = packageInfoPresenter.context
    private val packageManager = context.packageManager

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton {
        val dialogPresenter = confirmDialogPresenter()
        return ActionButton(
            text = stringResource(R.string.force_stop),
            imageVector = Icons.Outlined.Report,
            enabled = remember(app) { appForceStopRepository.canForceStopFlow() }
                .collectAsStateWithLifecycle(false).value,
        ) { onForceStopButtonClicked(app, dialogPresenter) }
    }

    private fun onForceStopButtonClicked(
        app: ApplicationInfo,
        dialogPresenter: AlertDialogPresenter,
    ) {
        packageInfoPresenter.logAction(SettingsEnums.ACTION_APP_INFO_FORCE_STOP)
        getAdminRestriction(app)?.let { admin ->
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin)
            return
        }
        dialogPresenter.open()
    }

    @VisibleForTesting
    fun getAdminRestriction(app: ApplicationInfo): EnforcedAdmin? = when {
        packageManager.isPackageStateProtected(app.packageName, app.userId) -> {
            RestrictedLockUtilsInternal.getDeviceOwner(context) ?: EnforcedAdmin()
        }

        else -> RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
            context, UserManager.DISALLOW_APPS_CONTROL, app.userId
        )
    }

    @Composable
    private fun confirmDialogPresenter() = rememberAlertDialogPresenter(
        confirmButton = AlertDialogButton(
            text = stringResource(R.string.okay),
            onClick = packageInfoPresenter::forceStop,
        ),
        dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
        title = stringResource(R.string.force_stop_dlg_title),
        text = { Text(stringResource(R.string.force_stop_dlg_text)) },
    )
}
