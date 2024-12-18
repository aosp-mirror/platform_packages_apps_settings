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
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult

class CarrierSettingsVersionPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private val searchItem = CarrierSettingsVersionSearchItem(context)

    fun init(subId: Int) {
        this.subId = subId
    }

    override fun getSummary() = searchItem.getSummary(subId)

    override fun getAvailabilityStatus() =
        if (searchItem.isAvailable(subId)) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    companion object {
        class CarrierSettingsVersionSearchItem(private val context: Context) :
            MobileNetworkSettingsSearchItem {
            private val carrierConfigRepository = CarrierConfigRepository(context)

            fun getSummary(subId: Int): String? =
                carrierConfigRepository.getString(
                    subId, CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING)

            fun isAvailable(subId: Int): Boolean = !getSummary(subId).isNullOrEmpty()

            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!isAvailable(subId)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "carrier_settings_version_key",
                    title = context.getString(R.string.carrier_settings_version),
                )
            }
        }
    }
}
