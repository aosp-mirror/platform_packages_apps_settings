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

package com.android.settings.spa.app.specialaccess

import android.app.AppOpsManager
import android.app.AppOpsManager.OP_PICTURE_IN_PICTURE
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.pm.PackageManager.PackageInfoFlags
import android.util.Log
import androidx.compose.runtime.Composable
import com.android.settings.R
import com.android.settingslib.spa.lifecycle.collectAsCallbackWithLifecycle
import com.android.settingslib.spaprivileged.model.app.AppOpsController
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.installed
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object PictureInPictureListProvider : TogglePermissionAppListProvider {
    override val permissionType = "PictureInPicture"
    override fun createModel(context: Context) = PictureInPictureListModel(context)
}

data class PictureInPictureRecord(
    override val app: ApplicationInfo,
    val isSupport: Boolean,
    val appOpsController: AppOpsController,
) : AppRecord

class PictureInPictureListModel(private val context: Context) :
    TogglePermissionAppListModel<PictureInPictureRecord> {
    override val pageTitleResId = R.string.picture_in_picture_title
    override val switchTitleResId = R.string.picture_in_picture_app_detail_switch
    override val footerResId = R.string.picture_in_picture_app_detail_summary
    override val enhancedConfirmationKey: String = AppOpsManager.OPSTR_PICTURE_IN_PICTURE

    private val packageManager = context.packageManager

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.map(::getPictureInPicturePackages)
            .combine(appListFlow) { pictureInPicturePackages, appList ->
                appList.map { app ->
                    createPictureInPictureRecord(
                        app = app,
                        isSupport = app.packageName in pictureInPicturePackages,
                    )
                }
            }

    override fun transformItem(app: ApplicationInfo) = createPictureInPictureRecord(
        app = app,
        isSupport = app.installed &&
            getPackageAndActivityInfo(app)?.supportsPictureInPicture() == true,
    )

    private fun createPictureInPictureRecord(app: ApplicationInfo, isSupport: Boolean) =
        PictureInPictureRecord(
            app = app,
            isSupport = isSupport,
            appOpsController =
                AppOpsController(
                    context = context,
                    app = app,
                    op = OP_PICTURE_IN_PICTURE,
                ),
        )

    override fun filter(userIdFlow: Flow<Int>, recordListFlow: Flow<List<PictureInPictureRecord>>) =
        recordListFlow.map { recordList -> recordList.filter { it.isSupport } }

    @Composable
    override fun isAllowed(record: PictureInPictureRecord) =
        record.appOpsController.isAllowed.collectAsCallbackWithLifecycle()

    override fun isChangeable(record: PictureInPictureRecord) = record.isSupport

    override fun setAllowed(record: PictureInPictureRecord, newAllowed: Boolean) {
        record.appOpsController.setAllowed(newAllowed)
    }

    private fun getPictureInPicturePackages(userId: Int): Set<String> =
        getPackageAndActivityInfoList(userId)
            .filter { it.supportsPictureInPicture() }
            .map { it.packageName }
            .toSet()

    private fun getPackageAndActivityInfo(app: ApplicationInfo): PackageInfo? = try {
        packageManager.getPackageInfoAsUser(app.packageName, GET_ACTIVITIES_FLAGS, app.userId)
    } catch (e: Exception) {
        // Query PackageManager.getPackageInfoAsUser() with GET_ACTIVITIES_FLAGS could cause
        // exception sometimes. Since we reply on this flag to retrieve the Picture In Picture
        // packages, we need to catch the exception to alleviate the impact before PackageManager
        // fixing this issue or provide a better api.
        Log.e(TAG, "Exception while getPackageInfoAsUser", e)
        null
    }

    private fun getPackageAndActivityInfoList(userId: Int): List<PackageInfo> = try {
        packageManager.getInstalledPackagesAsUser(GET_ACTIVITIES_FLAGS, userId)
    } catch (e: Exception) {
        // Query PackageManager.getPackageInfoAsUser() with GET_ACTIVITIES_FLAGS could cause
        // exception sometimes. Since we reply on this flag to retrieve the Picture In Picture
        // packages, we need to catch the exception to alleviate the impact before PackageManager
        // fixing this issue or provide a better api.
        Log.e(TAG, "Exception while getInstalledPackagesAsUser", e)
        emptyList()
    }

    companion object {
        private const val TAG = "PictureInPictureListModel"

        private fun PackageInfo.supportsPictureInPicture() =
            activities?.any(ActivityInfo::supportsPictureInPicture) ?: false

        private val GET_ACTIVITIES_FLAGS = PackageInfoFlags.of(GET_ACTIVITIES.toLong())
    }
}
