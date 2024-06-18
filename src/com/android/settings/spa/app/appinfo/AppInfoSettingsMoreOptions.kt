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

import android.app.AppOpsManager
import android.app.ecm.EnhancedConfirmationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.UserManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settingslib.spa.widget.scaffold.MoreOptionsAction
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.scaffold.RestrictedMenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@Composable
fun AppInfoSettingsMoreOptions(
    packageInfoPresenter: PackageInfoPresenter,
    app: ApplicationInfo,
    packageManagers: IPackageManagers = PackageManagers,
) {
    val state = app.produceState(packageManagers).value ?: return
    var restrictedSettingsAllowed by rememberSaveable { mutableStateOf(false) }
    if (!state.shownUninstallUpdates &&
        !state.shownUninstallForAllUsers &&
        !(state.shouldShowAccessRestrictedSettings && !restrictedSettingsAllowed)
    ) return
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
        if (state.shouldShowAccessRestrictedSettings && !restrictedSettingsAllowed) {
            MenuItem(text = stringResource(R.string.app_restricted_settings_lockscreen_title)) {
                app.allowRestrictedSettings(packageInfoPresenter.context) {
                    restrictedSettingsAllowed = true
                }
            }
        }
    }
}

private fun ApplicationInfo.allowRestrictedSettings(context: Context, onSuccess: () -> Unit) {
    AppInfoDashboardFragment.showLockScreen(context) {
        if (android.permission.flags.Flags.enhancedConfirmationModeApisEnabled()
                && android.security.Flags.extendEcmToAllSettings()) {
            val manager = context.getSystemService(EnhancedConfirmationManager::class.java)!!
            manager.clearRestriction(packageName)
        } else {
            context.appOpsManager.setMode(
                AppOpsManager.OP_ACCESS_RESTRICTED_SETTINGS,
                uid,
                packageName,
                AppOpsManager.MODE_ALLOWED,
            )
        }
        onSuccess()
        val toastString = context.getString(
            R.string.toast_allows_restricted_settings_successfully,
            loadLabel(context.packageManager),
        )
        Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
    }
}

private data class AppInfoSettingsMoreOptionsState(
    val shownUninstallUpdates: Boolean,
    val shownUninstallForAllUsers: Boolean,
    val shouldShowAccessRestrictedSettings: Boolean,
)

@Composable
private fun ApplicationInfo.produceState(
    packageManagers: IPackageManagers,
): State<AppInfoSettingsMoreOptionsState?> {
    val context = LocalContext.current
    return produceState<AppInfoSettingsMoreOptionsState?>(initialValue = null, this) {
        withContext(Dispatchers.IO) {
            value = getMoreOptionsState(context, packageManagers)
        }
    }
}

private suspend fun ApplicationInfo.getMoreOptionsState(
    context: Context,
    packageManagers: IPackageManagers,
) = coroutineScope {
    val shownUninstallUpdatesDeferred = async {
        isShowUninstallUpdates(context)
    }
    val shownUninstallForAllUsersDeferred = async {
        isShowUninstallForAllUsers(
            userManager = context.userManager,
            packageManagers = packageManagers,
        )
    }
    val shouldShowAccessRestrictedSettingsDeferred = async {
        shouldShowAccessRestrictedSettings(context)
    }
    val isProfileOrDeviceOwner =
        Utils.isProfileOrDeviceOwner(context.userManager, context.devicePolicyManager, packageName)
    AppInfoSettingsMoreOptionsState(
        // We don't allow uninstalling update for DO/PO if it's a system app, because it will clear
        // data on all users.
        shownUninstallUpdates = !isProfileOrDeviceOwner && shownUninstallUpdatesDeferred.await(),
        // We also don't allow uninstalling for all users if it's DO/PO for any user.
        shownUninstallForAllUsers =
            !isProfileOrDeviceOwner && shownUninstallForAllUsersDeferred.await(),
        shouldShowAccessRestrictedSettings = shouldShowAccessRestrictedSettingsDeferred.await(),
    )
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

private fun ApplicationInfo.shouldShowAccessRestrictedSettings(context: Context): Boolean {
    return if (android.permission.flags.Flags.enhancedConfirmationModeApisEnabled()
            && android.security.Flags.extendEcmToAllSettings()) {
        val manager = context.getSystemService(EnhancedConfirmationManager::class.java)!!
        manager.isClearRestrictionAllowed(packageName)
    } else {
        context.appOpsManager.noteOpNoThrow(
            AppOpsManager.OP_ACCESS_RESTRICTED_SETTINGS, uid, packageName, null, null
        ) == AppOpsManager.MODE_IGNORED
    }
}
