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

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.om.OverlayManager
import android.content.pm.ApplicationInfo
import android.content.pm.Flags
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.Utils
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.isDisallowControl
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId

class AppButtonRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val devicePolicyManager = context.devicePolicyManager

    /**
     * Checks whether the given application is disallowed from modifying.
     */
    fun isDisallowControl(app: ApplicationInfo): Boolean = when {
        // Not allow to control the device provisioning package.
        Utils.isDeviceProvisioningPackage(context.resources, app.packageName) -> true

        // If the uninstallation intent is already queued, disable the button.
        devicePolicyManager.isUninstallInQueue(app.packageName) -> true

        else -> app.isDisallowControl(context)
    }

    /**
     * Checks whether uninstall is blocked by admin.
     */
    fun isUninstallBlockedByAdmin(app: ApplicationInfo): Boolean =
        RestrictedLockUtilsInternal.checkIfUninstallBlocked(context, app.packageName, app.userId)
            ?.let { admin ->
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin)
                true
            } ?: false

    fun getHomePackageInfo(): HomePackages {
        val homePackages = mutableSetOf<String>()
        val homeActivities = ArrayList<ResolveInfo>()
        val currentDefaultHome = packageManager.getHomeActivities(homeActivities)
        homeActivities.mapNotNull { it.activityInfo }.forEach { activityInfo ->
            homePackages.add(activityInfo.packageName)
            // Also make sure to include anything proxying for the home app
            activityInfo.metaData?.getString(ActivityManager.META_HOME_ALTERNATE)
                ?.takeIf { signaturesMatch(it, activityInfo.packageName) }
                ?.let { homePackages.add(it) }
        }
        return HomePackages(homePackages, currentDefaultHome)
    }

    private fun signaturesMatch(packageName1: String, packageName2: String): Boolean = try {
        packageManager.checkSignatures(packageName1, packageName2) >= PackageManager.SIGNATURE_MATCH
    } catch (e: Exception) {
        // e.g. named alternate package not found during lookup; this is an expected case sometimes
        false
    }

    /** Gets whether a package can be uninstalled or archived. */
    fun isAllowUninstallOrArchive(
        context: Context, app: ApplicationInfo
    ): Boolean {
        val overlayManager = checkNotNull(context.getSystemService(OverlayManager::class.java))
        when {
            !app.hasFlag(ApplicationInfo.FLAG_INSTALLED) && !app.isArchived -> return false

            com.android.settings.Utils.isProfileOrDeviceOwner(
                context.devicePolicyManager, app.packageName, app.userId
            ) -> return false

            isDisallowControl(app) -> return false

            uninstallDisallowedDueToHomeApp(app) -> return false

            // Resource overlays can be uninstalled iff they are public (installed on /data) and
            // disabled. ("Enabled" means they are in use by resource management.)
            app.isEnabledResourceOverlay(overlayManager) -> return false

            else -> return true
        }
    }

    /**
     * Checks whether the given package cannot be uninstalled due to home app restrictions.
     *
     * Home launcher apps need special handling, we can't allow uninstallation of the only home
     * app, and we don't want to allow uninstallation of an explicitly preferred one -- the user
     * can go to Home settings and pick a different one, after which we'll permit uninstallation
     * of the now-not-default one.
     */
    fun uninstallDisallowedDueToHomeApp(applicationInfo: ApplicationInfo): Boolean {
        val packageName = applicationInfo.packageName
        val homePackageInfo = getHomePackageInfo()
        return when {
            packageName !in homePackageInfo.homePackages -> false

            // Disallow uninstall when this is the only home app.
            homePackageInfo.homePackages.size == 1 -> true

            packageName == homePackageInfo.currentDefaultHome?.packageName -> {
                if (Flags.improveHomeAppBehavior()) {
                    // Disallow the uninstallation of the current home app if it is a system app.
                    return applicationInfo.isSystemApp()
                } else {
                    // Disallow if this is the explicit default home app.
                    return true
                }
            }

            else -> false
        }
    }

    private fun ApplicationInfo.isEnabledResourceOverlay(overlayManager: OverlayManager): Boolean =
        isResourceOverlay &&
            overlayManager.getOverlayInfo(packageName, userHandle)?.isEnabled == true

    data class HomePackages(
        val homePackages: Set<String>,
        val currentDefaultHome: ComponentName?,
    )
}
