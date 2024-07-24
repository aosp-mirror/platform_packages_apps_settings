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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.pm.PackageManager.PackageInfoFlags
import android.nfc.NfcAdapter
import android.util.Log
import androidx.compose.runtime.Composable
import com.android.settings.R
import com.android.settingslib.spa.livedata.observeAsCallback
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object NfcTagAppsSettingsProvider : TogglePermissionAppListProvider {
    override val permissionType = "NfcTagAppsSettings"
    override fun createModel(context: Context) = NfcTagAppsSettingsListModel(context)
}

data class NfcTagAppsSettingsRecord(
    override val app: ApplicationInfo,
    val controller: NfcTagAppsSettingsController,
    val isSupported: Boolean,
) : AppRecord

class NfcTagAppsSettingsListModel(private val context: Context) :
    TogglePermissionAppListModel<NfcTagAppsSettingsRecord> {
    override val pageTitleResId = R.string.change_nfc_tag_apps_title
    override val switchTitleResId = R.string.change_nfc_tag_apps_detail_switch
    override val footerResId = R.string.change_nfc_tag_apps_detail_summary

    private val packageManager = context.packageManager

    override fun transform(
        userIdFlow: Flow<Int>,
        appListFlow: Flow<List<ApplicationInfo>>
    ): Flow<List<NfcTagAppsSettingsRecord>> =
        userIdFlow.combine(appListFlow) { userId, appList ->
            // The appListFlow always refreshed on resume, need to update nfcTagAppsSettingsPackages
            // here to handle status change.
            val nfcTagAppsSettingsPackages = getNfcTagAppsSettingsPackages(userId)
            appList.map { app ->
                createNfcTagAppsSettingsRecord(
                    app = app,
                    isAllowed = nfcTagAppsSettingsPackages[app.packageName],
                )
            }
        }

    private fun getNfcTagAppsSettingsPackages(userId: Int): Map<String, Boolean> {
        NfcAdapter.getDefaultAdapter(context)?.let { nfcAdapter ->
            if (nfcAdapter.isTagIntentAppPreferenceSupported) {
                return nfcAdapter.getTagIntentAppPreferenceForUser(userId)
            }
        }
        return emptyMap()
    }

    override fun transformItem(app: ApplicationInfo) =
        createNfcTagAppsSettingsRecord(
            app = app,
            isAllowed = getNfcTagAppsSettingsPackages(app.userId)[app.packageName],
        )

    private fun createNfcTagAppsSettingsRecord(
        app: ApplicationInfo,
        isAllowed: Boolean?,
    ) =
        NfcTagAppsSettingsRecord(
            app = app,
            isSupported = isAllowed != null,
            controller = NfcTagAppsSettingsController(isAllowed == true),
        )

    override fun filter(
        userIdFlow: Flow<Int>,
        recordListFlow: Flow<List<NfcTagAppsSettingsRecord>>
    ) = recordListFlow.map { recordList -> recordList.filter { it.isSupported } }

    @Composable
    override fun isAllowed(record: NfcTagAppsSettingsRecord) =
        record.controller.isAllowed.observeAsCallback()

    override fun isChangeable(record: NfcTagAppsSettingsRecord) = true

    override fun setAllowed(record: NfcTagAppsSettingsRecord, newAllowed: Boolean) {
        NfcAdapter.getDefaultAdapter(context)?.let {
            if (
                it.setTagIntentAppPreferenceForUser(
                    record.app.userId,
                    record.app.packageName,
                    newAllowed
                ) == NfcAdapter.TAG_INTENT_APP_PREF_RESULT_SUCCESS
            ) {
                record.controller.setAllowed(newAllowed)
            } else {
                Log.e(TAG, "Error updating TagIntentAppPreference")
            }
        }
    }

    private companion object {
        const val TAG = "NfcTagAppsSettingsListModel"
        val GET_ACTIVITIES_FLAGS = PackageInfoFlags.of(GET_ACTIVITIES.toLong())
    }
}
