/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.wifi

import android.content.Context
import android.net.wifi.WifiInfo
import com.android.settings.wifi.repository.SharedConnectivityRepository
import com.android.settings.wifi.repository.WifiPickerRepository
import com.android.settings.wifi.repository.WifiStatusRepository
import com.android.settingslib.R
import com.android.settingslib.wifi.WifiStatusTracker
import com.android.wifitrackerlib.HotspotNetworkEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Repository that listeners to wifi callback and provide wifi summary flow to client. */
class WifiSummaryRepository(
    private val context: Context,
    private val wifiStatusRepository: WifiStatusRepository = WifiStatusRepository(context),
    private val wifiPickerRepository: WifiPickerRepository? =
        if (SharedConnectivityRepository.isDeviceConfigEnabled()) WifiPickerRepository(context)
        else null,
) {

    fun summaryFlow(): Flow<String> {
        if (wifiPickerRepository == null) return wifiStatusSummaryFlow()
        return combine(
            wifiStatusSummaryFlow(),
            wifiPickerRepository.connectedWifiEntryFlow(),
        ) { wifiStatusSummary, wifiEntry ->
            if (wifiEntry is HotspotNetworkEntry) wifiEntry.alternateSummary else wifiStatusSummary
        }
    }

    private fun wifiStatusSummaryFlow() =
        wifiStatusRepository
            .wifiStatusTrackerFlow()
            .map { wifiStatusTracker -> wifiStatusTracker.getSummary() }
            .conflate()
            .flowOn(Dispatchers.Default)

    private fun WifiStatusTracker.getSummary(): String {
        if (!enabled) return context.getString(com.android.settings.R.string.switch_off_text)
        if (!connected) return context.getString(com.android.settings.R.string.disconnected)
        val sanitizedSsid = WifiInfo.sanitizeSsid(ssid) ?: ""
        if (statusLabel.isNullOrEmpty()) return sanitizedSsid
        return context.getString(
            R.string.preference_summary_default_combination,
            sanitizedSsid,
            statusLabel,
        )
    }
}
