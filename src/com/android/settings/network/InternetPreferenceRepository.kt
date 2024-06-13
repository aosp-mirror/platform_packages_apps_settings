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
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.DrawableRes
import com.android.settings.R
import com.android.settings.network.telephony.DataSubscriptionRepository
import com.android.settings.wifi.WifiSummaryRepository
import com.android.settings.wifi.repository.WifiRepository
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
class InternetPreferenceRepository(
    private val context: Context,
    private val connectivityRepository: ConnectivityRepository = ConnectivityRepository(context),
    private val wifiSummaryRepository: WifiSummaryRepository = WifiSummaryRepository(context),
    private val dataSubscriptionRepository: DataSubscriptionRepository =
        DataSubscriptionRepository(context),
    private val wifiRepository: WifiRepository = WifiRepository(context),
    private val airplaneModeOnFlow: Flow<Boolean> =
        context.settingsGlobalBooleanFlow(Settings.Global.AIRPLANE_MODE_ON),
) {

    data class DisplayInfo(
        val summary: String,
        @DrawableRes val iconResId: Int,
    )

    fun displayInfoFlow(): Flow<DisplayInfo> =
        connectivityRepository
            .networkCapabilitiesFlow()
            .flatMapLatest { capabilities -> capabilities.displayInfoFlow() }
            .onEach { Log.d(TAG, "displayInfoFlow: $it") }
            .conflate()
            .flowOn(Dispatchers.Default)

    private fun NetworkCapabilities.displayInfoFlow(): Flow<DisplayInfo> {
        if (
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            val transportInfo = transportInfo
            if (transportInfo is WifiInfo && transportInfo.isCarrierMerged) {
                Log.i(TAG, "Detect a merged carrier Wi-Fi connected.")
                return cellularDisplayInfoFlow()
            }
            for (transportType in transportTypes) {
                when (transportType) {
                    NetworkCapabilities.TRANSPORT_WIFI -> return wifiDisplayInfoFlow()
                    NetworkCapabilities.TRANSPORT_CELLULAR -> return cellularDisplayInfoFlow()
                    NetworkCapabilities.TRANSPORT_ETHERNET -> return ethernetDisplayInfoFlow()
                }
            }
        }
        return defaultDisplayInfoFlow()
    }

    private fun wifiDisplayInfoFlow() =
        wifiSummaryRepository.summaryFlow().map { summary ->
            DisplayInfo(
                summary = summary,
                iconResId = R.drawable.ic_wifi_signal_4,
            )
        }

    private fun cellularDisplayInfoFlow() =
        dataSubscriptionRepository.dataSummaryFlow().map { summary ->
            DisplayInfo(
                summary = summary,
                iconResId = R.drawable.ic_network_cell,
            )
        }

    private fun ethernetDisplayInfoFlow() =
        flowOf(
            DisplayInfo(
                summary = context.getString(R.string.to_switch_networks_disconnect_ethernet),
                iconResId = R.drawable.ic_settings_ethernet,
            )
        )

    private fun defaultDisplayInfoFlow(): Flow<DisplayInfo> =
        combine(
            airplaneModeOnFlow,
            wifiRepository.wifiStateFlow(),
        ) { airplaneModeOn: Boolean, wifiState: Int ->
            if (airplaneModeOn && wifiState != WifiManager.WIFI_STATE_ENABLED) {
                DisplayInfo(
                    summary = context.getString(R.string.condition_airplane_title),
                    iconResId = R.drawable.ic_no_internet_unavailable,
                )
            } else {
                DisplayInfo(
                    summary = context.getString(R.string.networks_available),
                    iconResId = R.drawable.ic_no_internet_available,
                )
            }
        }

    private companion object {
        private const val TAG = "InternetPreferenceRepo"
    }
}
