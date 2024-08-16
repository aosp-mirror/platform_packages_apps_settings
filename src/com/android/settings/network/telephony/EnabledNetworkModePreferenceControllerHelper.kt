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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun TelephonyManager.setAllowedNetworkTypes(
    viewLifecycleOwner: LifecycleOwner,
    newPreferredNetworkMode: Int,
) {
    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        setAllowedNetworkTypesForReason(
            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
            MobileNetworkUtils.getRafFromNetworkType(newPreferredNetworkMode),
        )
    }
}

enum class NetworkModePreferenceType {
    EnabledNetworkMode,
    PreferredNetworkMode,
    None,
}

fun getNetworkModePreferenceType(context: Context, subId: Int): NetworkModePreferenceType {
    if (!SubscriptionManager.isValidSubscriptionId(subId)) return NetworkModePreferenceType.None
    data class Config(
        val carrierConfigApplied: Boolean,
        val hideCarrierNetworkSettings: Boolean,
        val hidePreferredNetworkType: Boolean,
        val worldPhone: Boolean,
    )

    val config =
        CarrierConfigRepository(context).transformConfig(subId) {
            Config(
                carrierConfigApplied =
                    getBoolean(CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL),
                hideCarrierNetworkSettings =
                    getBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL),
                hidePreferredNetworkType =
                    getBoolean(CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL),
                worldPhone = getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL),
            )
        }

    return when {
        !config.carrierConfigApplied ||
            config.hideCarrierNetworkSettings ||
            config.hidePreferredNetworkType -> NetworkModePreferenceType.None
        config.worldPhone -> NetworkModePreferenceType.PreferredNetworkMode
        else -> NetworkModePreferenceType.EnabledNetworkMode
    }
}

class PreferredNetworkModeSearchItem(private val context: Context) :
    MobileNetworkSettingsSearchItem {
    private val title: String = context.getString(R.string.preferred_network_mode_title)

    override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? =
        when (getNetworkModePreferenceType(context, subId)) {
            NetworkModePreferenceType.PreferredNetworkMode ->
                MobileNetworkSettingsSearchResult(
                    key = "preferred_network_mode_key",
                    title = title,
                )

            NetworkModePreferenceType.EnabledNetworkMode ->
                MobileNetworkSettingsSearchResult(
                    key = "enabled_networks_key",
                    title = title,
                )

            else -> null
        }
}
