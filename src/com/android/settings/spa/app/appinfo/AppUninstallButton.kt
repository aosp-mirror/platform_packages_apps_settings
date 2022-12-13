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

import android.content.om.OverlayManager
import android.content.pm.ApplicationInfo
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.isActiveAdmin
import com.android.settingslib.spaprivileged.model.app.userHandle

class AppUninstallButton(private val packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context
    private val appButtonRepository = AppButtonRepository(context)
    private val overlayManager = context.getSystemService(OverlayManager::class.java)!!
    private val userManager = context.getSystemService(UserManager::class.java)!!

    fun getActionButton(app: ApplicationInfo): ActionButton? {
        if (app.isSystemApp || app.isInstantApp) return null
        return uninstallButton(app = app, enabled = isUninstallButtonEnabled(app))
    }

    /** Gets whether a package can be uninstalled. */
    private fun isUninstallButtonEnabled(app: ApplicationInfo): Boolean = when {
        !app.hasFlag(ApplicationInfo.FLAG_INSTALLED) -> false

        // Not allow to uninstall DO/PO.
        app.isActiveAdmin(context) -> false

        appButtonRepository.isDisallowControl(app) -> false

        uninstallDisallowedDueToHomeApp(app.packageName) -> false

        // Resource overlays can be uninstalled iff they are public (installed on /data) and
        // disabled. ("Enabled" means they are in use by resource management.)
        app.isEnabledResourceOverlay() -> false

        else -> true
    }

    /**
     * Checks whether the given package cannot be uninstalled due to home app restrictions.
     *
     * Home launcher apps need special handling, we can't allow uninstallation of the only home
     * app, and we don't want to allow uninstallation of an explicitly preferred one -- the user
     * can go to Home settings and pick a different one, after which we'll permit uninstallation
     * of the now-not-default one.
     */
    private fun uninstallDisallowedDueToHomeApp(packageName: String): Boolean {
        val homePackageInfo = appButtonRepository.getHomePackageInfo()
        return when {
            packageName !in homePackageInfo.homePackages -> false

            // Disallow uninstall when this is the only home app.
            homePackageInfo.homePackages.size == 1 -> true

            // Disallow if this is the explicit default home app.
            else -> packageName == homePackageInfo.currentDefaultHome?.packageName
        }
    }

    private fun ApplicationInfo.isEnabledResourceOverlay(): Boolean =
        isResourceOverlay &&
            overlayManager.getOverlayInfo(packageName, userHandle)?.isEnabled == true

    private fun uninstallButton(app: ApplicationInfo, enabled: Boolean) = ActionButton(
        text = if (isCloneApp(app)) context.getString(R.string.delete) else
            context.getString(R.string.uninstall_text),
        imageVector = Icons.Outlined.Delete,
        enabled = enabled,
    ) { onUninstallClicked(app) }

    private fun onUninstallClicked(app: ApplicationInfo) {
        if (appButtonRepository.isUninstallBlockedByAdmin(app)) return
        packageInfoPresenter.startUninstallActivity()
    }

    private fun isCloneApp(app: ApplicationInfo): Boolean  {
        val userInfo = userManager.getUserInfo(UserHandle.getUserId(app.uid))
        return userInfo != null && userInfo.isCloneProfile
    }
}
