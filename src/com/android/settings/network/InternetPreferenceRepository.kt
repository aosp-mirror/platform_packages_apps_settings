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

package com.android.settings.network

import android.content.Context
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.android.settings.R
import com.android.settings.wifi.WifiSummaryRepository
import com.android.settings.wifi.repository.WifiRepository
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
class InternetPreferenceRepository(
    private val context: Context,
    private val connectivityRepository: ConnectivityRepository = ConnectivityRepository(context),
    private val wifiSummaryRepository: WifiSummaryRepository = WifiSummaryRepository(context),
    private val wifiRepository: WifiRepository = WifiRepository(context),
    private val airplaneModeOnFlow: Flow<Boolean> =
        context.settingsGlobalBooleanFlow(Settings.Global.AIRPLANE_MODE_ON),
) {

    fun summaryFlow(): Flow<String> = connectivityRepository.networkCapabilitiesFlow()
        .flatMapLatest { capabilities -> capabilities.summaryFlow() }
        .onEach { Log.d(TAG, "summaryFlow: $it") }
        .conflate()
        .flowOn(Dispatchers.Default)

    private fun NetworkCapabilities.summaryFlow(): Flow<String> {
        if (hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            for (transportType in transportTypes) {
                if (transportType == NetworkCapabilities.TRANSPORT_WIFI) {
                    return wifiSummaryRepository.summaryFlow()
                }
            }
        }
        return defaultSummaryFlow()
    }

    private fun defaultSummaryFlow(): Flow<String> = combine(
        airplaneModeOnFlow,
        wifiRepository.wifiStateFlow(),
    ) { airplaneModeOn: Boolean, wifiState: Int ->
        context.getString(
            if (airplaneModeOn && wifiState != WifiManager.WIFI_STATE_ENABLED) {
                R.string.condition_airplane_title
            } else {
                R.string.networks_available
            }
        )
    }

    private companion object {
        private const val TAG = "InternetPreferenceRepo"
    }
}
