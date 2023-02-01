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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import android.util.Log
import androidx.compose.runtime.Composable
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.app.startUninstallActivity
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spaprivileged.framework.common.activityManager
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.framework.compose.DisposableBroadcastReceiverAsUser
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PackageInfoPresenter"

/**
 * Presenter which helps to present the status change of [PackageInfo].
 */
class PackageInfoPresenter(
    val context: Context,
    val packageName: String,
    val userId: Int,
    private val coroutineScope: CoroutineScope,
    private val packageManagers: IPackageManagers = PackageManagers,
) {
    private val metricsFeatureProvider = FeatureFactory.getFactory(context).metricsFeatureProvider
    private val userHandle = UserHandle.of(userId)
    val userContext by lazy { context.asUser(userHandle) }
    val userPackageManager: PackageManager by lazy { userContext.packageManager }
    private val _flow: MutableStateFlow<PackageInfo?> = MutableStateFlow(null)

    val flow: StateFlow<PackageInfo?> = _flow

    fun reloadPackageInfo() {
        coroutineScope.launch(Dispatchers.IO) {
            _flow.value = getPackageInfo()
        }
    }

    /**
     * Detects the package removed event.
     */
    @Composable
    fun PackageRemoveDetector() {
        val intentFilter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        val navController = LocalNavController.current
        DisposableBroadcastReceiverAsUser(intentFilter, userHandle) { intent ->
            if (packageName == intent.data?.schemeSpecificPart) {
                val packageInfo = flow.value
                if (packageInfo != null && packageInfo.applicationInfo.isSystemApp) {
                    // System app still exists after uninstalling the updates, refresh the page.
                    reloadPackageInfo()
                } else {
                    navController.navigateBack()
                }
            }
        }
    }

    /** Enables this package. */
    fun enable() {
        logAction(SettingsEnums.ACTION_SETTINGS_ENABLE_APP)
        coroutineScope.launch(Dispatchers.IO) {
            userPackageManager.setApplicationEnabledSetting(
                packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0
            )
            reloadPackageInfo()
        }
    }

    /** Disables this package. */
    fun disable() {
        logAction(SettingsEnums.ACTION_SETTINGS_DISABLE_APP)
        coroutineScope.launch(Dispatchers.IO) {
            userPackageManager.setApplicationEnabledSetting(
                packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0
            )
            reloadPackageInfo()
        }
    }

    /** Starts the uninstallation activity. */
    fun startUninstallActivity(forAllUsers: Boolean = false) {
        logAction(SettingsEnums.ACTION_SETTINGS_UNINSTALL_APP)
        context.startUninstallActivity(packageName, userHandle, forAllUsers)
    }

    /** Clears this instant app. */
    fun clearInstantApp() {
        logAction(SettingsEnums.ACTION_SETTINGS_CLEAR_INSTANT_APP)
        coroutineScope.launch(Dispatchers.IO) {
            userPackageManager.deletePackageAsUser(packageName, null, 0, userId)
            reloadPackageInfo()
        }
    }

    /** Force stops this package. */
    fun forceStop() {
        logAction(SettingsEnums.ACTION_APP_FORCE_STOP)
        coroutineScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Stopping package $packageName")
            context.activityManager.forceStopPackageAsUser(packageName, userId)
            reloadPackageInfo()
        }
    }

    fun logAction(category: Int) {
        metricsFeatureProvider.action(context, category, packageName)
    }

    private fun getPackageInfo() =
        packageManagers.getPackageInfoAsUser(
            packageName = packageName,
            flags = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.GET_PERMISSIONS,
            userId = userId,
        )
}
