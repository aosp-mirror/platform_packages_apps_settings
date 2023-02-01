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
import android.os.UserManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.spa.widget.scaffold.MoreOptionsAction
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.scaffold.RestrictedMenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppInfoSettingsMoreOptions(
    packageInfoPresenter: PackageInfoPresenter,
    app: ApplicationInfo,
    packageManagers: IPackageManagers = PackageManagers,
) {
    val state = app.produceState(packageManagers).value ?: return
    when {
        // We don't allow uninstalling update for DO/PO if it's a system app, because it will clear
        // data on all users. We also don't allow uninstalling for all users if it's DO/PO for any
        // user.
        state.isProfileOrDeviceOwner -> return
        !state.shownUninstallUpdates && !state.shownUninstallForAllUsers -> return
    }
    MoreOptionsAction {
        val restrictions =
            Restrictions(userId = app.userId, keys = listOf(UserManager.DISALLOW_APPS_CONTROL))
        if (state.shownUninstallUpdates) {
            RestrictedMenuItem(
                text = stringResource(R.string.app_factory_reset),
                restrictions = restrictions,
            ) {
                packageInfoPresenter.startUninstallActivity(forAllUsers = false)
            }
        }
        if (state.shownUninstallForAllUsers) {
            RestrictedMenuItem(
                text = stringResource(R.string.uninstall_all_users_text),
                restrictions = restrictions,
            ) {
                packageInfoPresenter.startUninstallActivity(forAllUsers = true)
            }
        }
    }
}

private data class AppInfoSettingsMoreOptionsState(
    val isProfileOrDeviceOwner: Boolean,
    val shownUninstallUpdates: Boolean,
    val shownUninstallForAllUsers: Boolean,
)

@Composable
private fun ApplicationInfo.produceState(
    packageManagers: IPackageManagers,
): State<AppInfoSettingsMoreOptionsState?> {
    val context = LocalContext.current
    return produceState<AppInfoSettingsMoreOptionsState?>(initialValue = null, this) {
        withContext(Dispatchers.IO) {
            value = AppInfoSettingsMoreOptionsState(
                isProfileOrDeviceOwner = Utils.isProfileOrDeviceOwner(
                    context.userManager, context.devicePolicyManager, packageName
                ),
                shownUninstallUpdates = isShowUninstallUpdates(context),
                shownUninstallForAllUsers = isShowUninstallForAllUsers(
                    userManager = context.userManager,
                    packageManagers = packageManagers,
                ),
            )
        }
    }
}

private fun ApplicationInfo.isShowUninstallUpdates(context: Context): Boolean =
    isUpdatedSystemApp && context.userManager.isUserAdmin(userId) &&
        !context.resources.getBoolean(R.bool.config_disable_uninstall_update)

private fun ApplicationInfo.isShowUninstallForAllUsers(
    userManager: UserManager,
    packageManagers: IPackageManagers,
): Boolean = userId == 0 && !isSystemApp && !isInstantApp &&
    isOtherUserHasInstallPackage(userManager, packageManagers)

private fun ApplicationInfo.isOtherUserHasInstallPackage(
    userManager: UserManager,
    packageManagers: IPackageManagers,
): Boolean = userManager.aliveUsers
    .filter { it.id != userId }
    .any { packageManagers.isPackageInstalledAsUser(packageName, it.id) }
