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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.app.settings.SettingsEnums
import android.companion.AssociationRequest
import android.content.Context
import com.android.media.flags.Flags;
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object MediaRoutingControlAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "MediaRoutingControl"
    override fun createModel(context: Context) = MediaRoutingControlAppsListModel(context)
}

class MediaRoutingControlAppsListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.media_routing_control_title
    override val switchTitleResId = R.string.allow_media_routing_control
    override val footerResId = R.string.allow_media_routing_description
    override val appOp = AppOpsManager.OP_MEDIA_ROUTING_CONTROL
    override val permission = Manifest.permission.MEDIA_ROUTING_CONTROL
    override val setModeByUid = true
    private val roleManager = context.getSystemService(RoleManager::class.java)

    override fun setAllowed(record: AppOpPermissionRecord, newAllowed: Boolean) {
        super.setAllowed(record, newAllowed)
        logPermissionToggleAction(newAllowed)
    }

    override fun isChangeable(record: AppOpPermissionRecord): Boolean {
        return Flags.enablePrivilegedRoutingForMediaRoutingControl()
                && super.isChangeable(record)
                && (this.roleManager?.getRoleHolders(AssociationRequest.DEVICE_PROFILE_WATCH)
                ?.contains(record.app.packageName) == true)
    }

    private fun logPermissionToggleAction(newAllowed: Boolean) {
        featureFactory.metricsFeatureProvider.action(
                context,
                SettingsEnums.MEDIA_ROUTING_CONTROL,
                if (newAllowed)
                    VALUE_LOGGING_ALLOWED
                else
                    VALUE_LOGGING_DENIED
        )
    }

    companion object {
        const val VALUE_LOGGING_ALLOWED = 1
        const val VALUE_LOGGING_DENIED = 0
    }
}