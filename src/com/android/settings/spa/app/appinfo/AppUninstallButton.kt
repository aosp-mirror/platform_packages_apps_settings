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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.model.app.isActiveAdmin
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AppUninstallButton(private val packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context
    private val appButtonRepository = AppButtonRepository(context)
    private val userManager = context.getSystemService(UserManager::class.java)!!

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton? {
        if (app.isSystemApp || app.isInstantApp) return null
        return uninstallButton(app)
    }

    @Composable
    private fun uninstallButton(app: ApplicationInfo) = ActionButton(
        text = if (isCloneApp(app)) context.getString(R.string.delete) else
            context.getString(R.string.uninstall_text),
        imageVector = ImageVector.vectorResource(R.drawable.ic_settings_delete),
        enabled = remember(app) {
            flow {
                emit(appButtonRepository.isAllowUninstallOrArchive(context, app))
            }.flowOn(Dispatchers.Default)
        }.collectAsStateWithLifecycle(false).value,
    ) { onUninstallClicked(app) }

    private fun onUninstallClicked(app: ApplicationInfo) {
        if (appButtonRepository.isUninstallBlockedByAdmin(app)) {
            return
        } else if (app.isActiveAdmin(context)) {
                val uninstallDaIntent = Intent(context, DeviceAdminAdd::class.java)
                uninstallDaIntent.putExtra(DeviceAdminAdd.EXTRA_DEVICE_ADMIN_PACKAGE_NAME,
                        app.packageName)
                packageInfoPresenter.logAction(
                    SettingsEnums.ACTION_SETTINGS_UNINSTALL_DEVICE_ADMIN)
                context.startActivityAsUser(uninstallDaIntent, app.userHandle)
                return
        }
        packageInfoPresenter.startUninstallActivity()
    }

    private fun isCloneApp(app: ApplicationInfo): Boolean  {
        val userInfo = userManager.getUserInfo(UserHandle.getUserId(app.uid))
        return userInfo != null && userInfo.isCloneProfile
    }
}
