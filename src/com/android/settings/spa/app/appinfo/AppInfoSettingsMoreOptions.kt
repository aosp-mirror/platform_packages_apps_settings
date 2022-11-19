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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.spa.widget.scaffold.MoreOptionsAction
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.isDisallowControl
import com.android.settingslib.spaprivileged.model.app.userId

@Composable
fun AppInfoSettingsMoreOptions(packageInfoPresenter: PackageInfoPresenter, app: ApplicationInfo) {
    val context = LocalContext.current
    // We don't allow uninstalling update for DO/PO if it's a system app, because it will clear data
    // on all users. We also don't allow uninstalling for all users if it's DO/PO for any user.
    val isProfileOrDeviceOwner = remember(app) {
        Utils.isProfileOrDeviceOwner(
            context.userManager, context.devicePolicyManager, app.packageName
        )
    }
    if (isProfileOrDeviceOwner) return
    val shownUninstallUpdates = remember(app) { isShowUninstallUpdates(context, app) }
    val shownUninstallForAllUsers = remember(app) { isShowUninstallForAllUsers(context, app) }
    if (!shownUninstallUpdates && !shownUninstallForAllUsers) return
    MoreOptionsAction {
        if (shownUninstallUpdates) {
            MenuItem(text = stringResource(R.string.app_factory_reset)) {
                packageInfoPresenter.startUninstallActivity(forAllUsers = false)
            }
        }
        if (shownUninstallForAllUsers) {
            MenuItem(text = stringResource(R.string.uninstall_all_users_text)) {
                packageInfoPresenter.startUninstallActivity(forAllUsers = true)
            }
        }
    }
}

private fun isShowUninstallUpdates(context: Context, app: ApplicationInfo): Boolean =
    app.isUpdatedSystemApp && context.userManager.isUserAdmin(app.userId) &&
        !app.isDisallowControl(context) &&
        !context.resources.getBoolean(R.bool.config_disable_uninstall_update)

private fun isShowUninstallForAllUsers(context: Context, app: ApplicationInfo): Boolean =
    app.userId == 0 && !app.isSystemApp && !app.isInstantApp &&
        isOtherUserHasInstallPackage(context, app)

private fun isOtherUserHasInstallPackage(context: Context, app: ApplicationInfo): Boolean =
    context.userManager.aliveUsers
        .filter { it.id != app.userId }
        .any { PackageManagers.isPackageInstalledAsUser(app.packageName, it.id) }
