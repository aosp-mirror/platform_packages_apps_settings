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

import android.content.pm.ApplicationInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.isDisabledUntilUsed
import com.android.settingslib.Utils as SettingsLibUtils

class AppDisableButton(
    private val packageInfoPresenter: PackageInfoPresenter,
) {
    private val context = packageInfoPresenter.context
    private val appButtonRepository = AppButtonRepository(context)
    private val resources = context.resources
    private val packageManager = context.packageManager
    private val userManager = context.userManager
    private val devicePolicyManager = context.devicePolicyManager
    private val applicationFeatureProvider = featureFactory.applicationFeatureProvider

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton? {
        if (!app.isSystemApp) return null

        return when {
            app.enabled && !app.isDisabledUntilUsed -> {
                disableButton(app)
            }

            else -> enableButton()
        }
    }

    /**
     * Gets whether a package can be disabled.
     */
    private fun ApplicationInfo.canBeDisabled(): Boolean = when {
        // Try to prevent the user from bricking their phone by not allowing disabling of apps
        // signed with the system certificate.
        isSignedWithPlatformKey -> false

        // system/vendor resource overlays can never be disabled.
        isResourceOverlay -> false

        packageName in applicationFeatureProvider.keepEnabledPackages -> false

        // Home launcher apps need special handling. In system ones we don't risk downgrading
        // because that can interfere with home-key resolution.
        packageName in appButtonRepository.getHomePackageInfo().homePackages -> false

        SettingsLibUtils.isEssentialPackage(resources, packageManager, packageName) -> false

        // We don't allow disabling DO/PO on *any* users if it's a system app, because
        // "disabling" is actually "downgrade to the system version + disable", and "downgrade"
        // will clear data on all users.
        Utils.isProfileOrDeviceOwner(userManager, devicePolicyManager, packageName) -> false

        appButtonRepository.isDisallowControl(this) -> false

        else -> true
    }

    @Composable
    private fun disableButton(app: ApplicationInfo): ActionButton {
        val dialogPresenter = confirmDialogPresenter()
        return ActionButton(
            text = context.getString(R.string.disable_text),
            imageVector = Icons.Outlined.HideSource,
            enabled = app.canBeDisabled(),
        ) {
            // Currently we apply the same device policy for both the uninstallation and disable
            // button.
            if (!appButtonRepository.isUninstallBlockedByAdmin(app)) {
                dialogPresenter.open()
            }
        }
    }

    private fun enableButton() = ActionButton(
        text = context.getString(R.string.enable_text),
        imageVector = Icons.Outlined.ArrowCircleDown,
    ) { packageInfoPresenter.enable() }

    @Composable
    private fun confirmDialogPresenter() = rememberAlertDialogPresenter(
        confirmButton = AlertDialogButton(
            text = stringResource(R.string.app_disable_dlg_positive),
            onClick = packageInfoPresenter::disable,
        ),
        dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
        text = { Text(stringResource(R.string.app_disable_dlg_text)) },
    )
}
