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

package com.android.settings

import android.content.Context
import android.content.Intent
import android.util.FeatureFlagUtils
import com.android.settings.applications.appinfo.AlarmsAndRemindersDetails
import com.android.settings.applications.appinfo.DrawOverlayDetails
import com.android.settings.applications.appinfo.ExternalSourcesDetails
import com.android.settings.applications.appinfo.ManageExternalStorageDetails
import com.android.settings.applications.appinfo.MediaManagementAppsDetails
import com.android.settings.applications.appinfo.WriteSettingsDetails
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureDetails
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureSettings
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.SpaAppBridgeActivity.Companion.getDestinationForApp
import com.android.settings.spa.app.specialaccess.AlarmsAndRemindersAppListProvider
import com.android.settings.spa.app.specialaccess.AllFilesAccessAppListProvider
import com.android.settings.spa.app.specialaccess.BackupTasksAppsListProvider
import com.android.settings.spa.app.specialaccess.DisplayOverOtherAppsAppListProvider
import com.android.settings.spa.app.specialaccess.InstallUnknownAppsListProvider
import com.android.settings.spa.app.specialaccess.MediaManagementAppsAppListProvider
import com.android.settings.spa.app.specialaccess.ModifySystemSettingsAppListProvider
import com.android.settings.spa.app.specialaccess.NfcTagAppsSettingsProvider
import com.android.settings.spa.app.specialaccess.PictureInPictureListProvider
import com.android.settings.spa.app.specialaccess.VoiceActivationAppsListProvider
import com.android.settings.spa.app.specialaccess.WifiControlAppListProvider
import com.android.settings.wifi.ChangeWifiStateDetails

object SettingsActivityUtil {
    private val FRAGMENT_TO_SPA_DESTINATION_MAP = mapOf(
        PictureInPictureSettings::class.qualifiedName to
            PictureInPictureListProvider.getAppListRoute(),
    )

    private val FRAGMENT_TO_SPA_APP_DESTINATION_PREFIX_MAP = mapOf(
        PictureInPictureDetails::class.qualifiedName to
            PictureInPictureListProvider.getAppInfoRoutePrefix(),
        DrawOverlayDetails::class.qualifiedName to
            DisplayOverOtherAppsAppListProvider.getAppInfoRoutePrefix(),
        WriteSettingsDetails::class.qualifiedName to
            ModifySystemSettingsAppListProvider.getAppInfoRoutePrefix(),
        AlarmsAndRemindersDetails::class.qualifiedName to
            AlarmsAndRemindersAppListProvider.getAppInfoRoutePrefix(),
        ExternalSourcesDetails::class.qualifiedName to
            InstallUnknownAppsListProvider.getAppInfoRoutePrefix(),
        ManageExternalStorageDetails::class.qualifiedName to
            AllFilesAccessAppListProvider.getAppInfoRoutePrefix(),
        MediaManagementAppsDetails::class.qualifiedName to
            MediaManagementAppsAppListProvider.getAppInfoRoutePrefix(),
        ChangeWifiStateDetails::class.qualifiedName to
            WifiControlAppListProvider.getAppInfoRoutePrefix(),
        NfcTagAppsSettingsProvider::class.qualifiedName to
            NfcTagAppsSettingsProvider.getAppInfoRoutePrefix(),
        VoiceActivationAppsListProvider::class.qualifiedName to
            VoiceActivationAppsListProvider.getAppInfoRoutePrefix(),
        BackupTasksAppsListProvider::class.qualifiedName to
            BackupTasksAppsListProvider.getAppInfoRoutePrefix(),
    )

    @JvmStatic
    fun Context.launchSpaActivity(fragmentName: String, intent: Intent): Boolean {
        if (FeatureFlagUtils.isEnabled(this, FeatureFlagUtils.SETTINGS_ENABLE_SPA)) {
            getDestination(fragmentName, intent)?.let { destination ->
                startSpaActivity(destination)
                return true
            }
        }
        return false
    }

    private fun getDestination(fragmentName: String, intent: Intent): String? =
        FRAGMENT_TO_SPA_DESTINATION_MAP[fragmentName]
            ?: FRAGMENT_TO_SPA_APP_DESTINATION_PREFIX_MAP[fragmentName]?.let { destinationPrefix ->
                getDestinationForApp(destinationPrefix, intent)
            }
}
