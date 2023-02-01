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

import android.app.AppOpsManager.OP_PICTURE_IN_PICTURE
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.pm.PackageManager.PackageInfoFlags
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.AppOpsController
import com.android.settingslib.spaprivileged.model.app.AppRecord
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

    private val packageManager = context.packageManager

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.map(::getPictureInPicturePackages).combine(appListFlow) {
            pictureInPicturePackages,
            appList ->
            appList.map { app ->
                createPictureInPictureRecord(
                    app = app,
                    isSupport = app.packageName in pictureInPicturePackages,
                )
            }
        }

    override fun transformItem(app: ApplicationInfo): PictureInPictureRecord {
        val packageInfo =
            packageManager.getPackageInfoAsUser(app.packageName, GET_ACTIVITIES_FLAGS, app.userId)
        return createPictureInPictureRecord(
            app = app,
            isSupport = packageInfo.supportsPictureInPicture(),
        )
    }

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
        record.appOpsController.isAllowed.observeAsState()

    override fun isChangeable(record: PictureInPictureRecord) = record.isSupport

    override fun setAllowed(record: PictureInPictureRecord, newAllowed: Boolean) {
        record.appOpsController.setAllowed(newAllowed)
    }

    private fun getPictureInPicturePackages(userId: Int): Set<String> =
        packageManager
            .getInstalledPackagesAsUser(GET_ACTIVITIES_FLAGS, userId)
            .filter { it.supportsPictureInPicture() }
            .map { it.packageName }
            .toSet()

    companion object {
        private fun PackageInfo.supportsPictureInPicture() =
            activities?.any(ActivityInfo::supportsPictureInPicture) ?: false

        private val GET_ACTIVITIES_FLAGS = PackageInfoFlags.of(GET_ACTIVITIES.toLong())
    }
}
