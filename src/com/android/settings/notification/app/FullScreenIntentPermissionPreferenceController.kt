/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.notification.app

import android.Manifest.permission.USE_FULL_SCREEN_INTENT
import android.app.AppOpsManager
import android.app.AppOpsManager.OP_USE_FULL_SCREEN_INTENT
import android.content.AttributionSource
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.os.UserHandle
import android.permission.PermissionManager
import android.util.Log
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settings.notification.NotificationBackend
import com.android.settingslib.RestrictedSwitchPreference

class FullScreenIntentPermissionPreferenceController(
    context: Context,
    backend: NotificationBackend
) : NotificationPreferenceController(context, backend), OnPreferenceChangeListener {
    private val packageManager = mPm!!
    private val permissionManager = context.getSystemService(PermissionManager::class.java)!!
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)!!

    private val packageName get() = mAppRow.pkg
    private val uid get() = mAppRow.uid
    private val userHandle get() = UserHandle.getUserHandleForUid(uid)

    override fun getPreferenceKey() = KEY_FSI_PERMISSION

    override fun isAvailable(): Boolean {
        val inAppWidePreferences = mChannelGroup == null && mChannel == null

        if (!inAppWidePreferences) {
            Log.wtf(TAG, "Belongs only in app-wide notification preferences!")
        }

        return super.isAvailable() && inAppWidePreferences && isPermissionRequested()
    }

    override fun isIncludedInFilter() = false

    override fun updateState(preference: Preference) {
        check(KEY_FSI_PERMISSION.equals(preference.key))
        check(preference is RestrictedSwitchPreference)

        preference.setDisabledByAdmin(mAdmin)
        preference.isEnabled = !preference.isDisabledByAdmin
        preference.isChecked = isPermissionGranted()
    }

    override fun onPreferenceChange(preference: Preference, value: Any): Boolean {
        check(KEY_FSI_PERMISSION.equals(preference.key))
        check(preference is RestrictedSwitchPreference)
        check(value is Boolean)

        if (isPermissionGranted() != value) {
            setPermissionGranted(value)
        }

        return true
    }

    private fun isPermissionRequested(): Boolean {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, GET_PERMISSIONS)

            for (requestedPermission in packageInfo.requestedPermissions.orEmpty()) {
                if (USE_FULL_SCREEN_INTENT.equals(requestedPermission)) {
                    return true
                }
            }
        } catch (exception: NameNotFoundException) {
            Log.e(TAG, "isPermissionRequested failed: $exception")
        }

        return false
    }

    private fun isPermissionGranted(): Boolean {
        val attributionSource = AttributionSource.Builder(uid).setPackageName(packageName).build()

        val permissionResult =
            permissionManager.checkPermissionForPreflight(USE_FULL_SCREEN_INTENT, attributionSource)

        return (permissionResult == PermissionManager.PERMISSION_GRANTED)
    }

    private fun setPermissionGranted(allowed: Boolean) {
        val mode = if (allowed) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_ERRORED
        appOpsManager.setUidMode(OP_USE_FULL_SCREEN_INTENT, uid, mode)
        packageManager.updatePermissionFlags(
            USE_FULL_SCREEN_INTENT,
            packageName,
            FLAG_PERMISSION_USER_SET,
            FLAG_PERMISSION_USER_SET,
            userHandle
        )
    }

    companion object {
        const val KEY_FSI_PERMISSION = "fsi_permission"
        const val TAG = "FsiPermPrefController"
    }
}
