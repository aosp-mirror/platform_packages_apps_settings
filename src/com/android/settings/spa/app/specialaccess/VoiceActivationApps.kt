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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spaprivileged.model.app.AppOpsController
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasGrantPermission
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.Dispatchers

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

    @Composable
    override fun InfoPageAdditionalContent(
        record: AppOpPermissionRecord,
        isAllowed: () -> Boolean?,
    ) {
        SwitchPreference(createReceiveDetectionTrainingDataOpSwitchModel(record, isAllowed))
    }

    @Composable
    private fun createReceiveDetectionTrainingDataOpSwitchModel(
        record: AppOpPermissionRecord,
        isReceiveSandBoxTriggerAudioOpAllowed: () -> Boolean?
    ): ReceiveDetectionTrainingDataOpSwitchModel {
        val context = LocalContext.current
        val ReceiveDetectionTrainingDataOpController = remember {
            AppOpsController(
                context = context,
                app = record.app,
                op = AppOpsManager.OP_RECEIVE_SANDBOXED_DETECTION_TRAINING_DATA,
            )
        }
        val isReceiveDetectionTrainingDataOpAllowed = isReceiveDetectionTrainingDataOpAllowed(record, ReceiveDetectionTrainingDataOpController)
        return remember(record) {
            ReceiveDetectionTrainingDataOpSwitchModel(
                context,
                record,
                isReceiveSandBoxTriggerAudioOpAllowed,
                ReceiveDetectionTrainingDataOpController,
                isReceiveDetectionTrainingDataOpAllowed,
            )
        }.also { model -> LaunchedEffect(model, Dispatchers.Default) { model.initState() } }
    }

    private inner class ReceiveDetectionTrainingDataOpSwitchModel(
        context: Context,
        private val record: AppOpPermissionRecord,
        isReceiveSandBoxTriggerAudioOpAllowed: () -> Boolean?,
        receiveDetectionTrainingDataOpController: AppOpsController,
        isReceiveDetectionTrainingDataOpAllowed: () -> Boolean?,
    ) : SwitchPreferenceModel {
        private var appChangeable by mutableStateOf(true)

        override val title: String = context.getString(R.string.permit_receive_sandboxed_detection_training_data)
        override val summary: () -> String = { context.getString(R.string.receive_sandboxed_detection_training_data_description) }
        override val checked = { isReceiveDetectionTrainingDataOpAllowed() == true && isReceiveSandBoxTriggerAudioOpAllowed() == true }
        override val changeable = { appChangeable && isReceiveSandBoxTriggerAudioOpAllowed() == true }

        fun initState() {
            appChangeable = isChangeable(record)
        }

        override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
            receiveDetectionTrainingDataOpController.setAllowed(newChecked)
        }
    }

    @Composable
    private fun isReceiveDetectionTrainingDataOpAllowed(
        record: AppOpPermissionRecord,
        controller: AppOpsController
    ): () -> Boolean? {
        if (record.hasRequestBroaderPermission) {
            // Broader permission trumps the specific permission.
            return { true }
        }

        val mode = controller.mode.observeAsState()
        return {
            when (mode.value) {
                null -> null
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_DEFAULT -> record.app.hasGrantPermission(
                    Manifest.permission.RECEIVE_SANDBOXED_DETECTION_TRAINING_DATA)
                else -> false
            }
        }
    }

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