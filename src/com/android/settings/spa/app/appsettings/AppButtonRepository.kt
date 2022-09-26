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

package com.android.settings.spa.app.appsettings

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserManager
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.Utils
import com.android.settingslib.spaprivileged.model.app.userId

class AppButtonRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)!!

    /**
     * Checks whether the given application is disallowed from modifying.
     */
    fun isDisallowControl(app: ApplicationInfo): Boolean = when {
        // Not allow to control the device provisioning package.
        Utils.isDeviceProvisioningPackage(context.resources, app.packageName) -> true

        // If the uninstallation intent is already queued, disable the button.
        devicePolicyManager.isUninstallInQueue(app.packageName) -> true

        RestrictedLockUtilsInternal.hasBaseUserRestriction(
            context, UserManager.DISALLOW_APPS_CONTROL, app.userId
        ) -> true

        else -> false
    }

    /**
     * Checks whether the given application is an active admin.
     */
    fun isActiveAdmin(app: ApplicationInfo): Boolean =
        devicePolicyManager.packageHasActiveAdmins(app.packageName, app.userId)

    fun getHomePackageInfo(): AppUninstallButton.HomePackages {
        val homePackages = mutableSetOf<String>()
        val homeActivities = ArrayList<ResolveInfo>()
        val currentDefaultHome = packageManager.getHomeActivities(homeActivities)
        homeActivities.map { it.activityInfo }.forEach {
            homePackages.add(it.packageName)
            // Also make sure to include anything proxying for the home app
            val metaPackageName = it.metaData?.getString(ActivityManager.META_HOME_ALTERNATE)
            if (metaPackageName != null && signaturesMatch(metaPackageName, it.packageName)) {
                homePackages.add(metaPackageName)
            }
        }
        return AppUninstallButton.HomePackages(homePackages, currentDefaultHome)
    }

    private fun signaturesMatch(packageName1: String, packageName2: String): Boolean = try {
        packageManager.checkSignatures(packageName1, packageName2) >= PackageManager.SIGNATURE_MATCH
    } catch (e: Exception) {
        // e.g. named alternate package not found during lookup; this is an expected case sometimes
        false
    }
}
