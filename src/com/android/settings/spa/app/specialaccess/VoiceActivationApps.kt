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
import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasGrantPermission
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

/**
 * This class builds an App List under voice activation apps and the individual page which
 * allows the user to toggle voice activation related permissions on / off for the apps displayed
 * in the list.
 */
object VoiceActivationAppsListProvider : TogglePermissionAppListProvider {
    override val permissionType = "VoiceActivationApps"
    override fun createModel(context: Context) = VoiceActivationAppsListModel(context)
}

class VoiceActivationAppsListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.voice_activation_apps_title
    override val switchTitleResId = R.string.permit_voice_activation_apps
    override val footerResId = R.string.allow_voice_activation_apps_description
    override val appOp = AppOpsManager.OP_RECEIVE_SANDBOX_TRIGGER_AUDIO
    override val permission = Manifest.permission.RECEIVE_SANDBOX_TRIGGER_AUDIO
    override val setModeByUid = true

    override fun setAllowed(record: AppOpPermissionRecord, newAllowed: Boolean) {
        super.setAllowed(record, newAllowed)
        logPermissionChange(newAllowed)
    }

    override fun isChangeable(record: AppOpPermissionRecord): Boolean =
        super.isChangeable(record) && record.app.hasGrantPermission(permission)

    private fun logPermissionChange(newAllowed: Boolean) {
        val category = when {
            newAllowed -> SettingsEnums.APP_SPECIAL_PERMISSION_RECEIVE_SANDBOX_TRIGGER_AUDIO_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_RECEIVE_SANDBOX_TRIGGER_AUDIO_DENY
        }
        /**
         * Leave the package string empty as we should not log the package names for the collected
         * metrics.
         */
        FeatureFactory.featureFactory.metricsFeatureProvider.action(context, category, "")
    }
}