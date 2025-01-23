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
package com.android.settings.wifi.calling

import android.content.Context
import android.os.Bundle
import android.telephony.SubscriptionManager.getDefaultSubscriptionId
import android.telephony.SubscriptionManager.isValidSubscriptionId
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.wificalling.WifiCallingRepository
import com.android.settings.wifi.calling.WifiCallingSettingsForSub.EXTRA_SUB_ID
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@ProvidePreferenceScreen(WifiCallingScreen.KEY, parameterized = true, parameterizedMigration = true)
class WifiCallingScreen(override val arguments: Bundle) :
    PreferenceScreenCreator, PreferenceAvailabilityProvider {

    private val subId = arguments.getInt(EXTRA_SUB_ID, getDefaultSubscriptionId())

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.wifi_calling_settings_title

    override val summary: Int
        get() = R.string.wifi_calling_summary

    override fun isAvailable(context: Context) = isValidSubscriptionId(subId)

    override fun isFlagEnabled(context: Context) = Flags.catalystWifiCalling()

    override fun fragmentClass() = WifiCallingSettingsForSub::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) { +WifiCallingMainSwitchPreference(subId) }

    companion object {
        const val KEY = "wifi_calling"

        /**
         * Provides arguments to generate [WifiCallingScreen].
         *
         * This method is used by annotation processor to produce
         * `PreferenceScreenMetadataParameterizedFactory`).
         */
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            fun Int.toArguments() = Bundle(1).also { it.putInt(EXTRA_SUB_ID, this) }
            // handle backward compatibility with default subscription id
            val defaultSubId = getDefaultSubscriptionId()
            val flow =
                SubscriptionUtil.getSelectableSubscriptionInfoList(context)
                    .asFlow()
                    .filter {
                        val subId = it.subscriptionId
                        subId != defaultSubId &&
                            WifiCallingRepository(context, subId).wifiCallingReadyFlow().first()
                    }
                    .map { it.subscriptionId.toArguments() }
            // Bundle.EMPTY is for backward compatibility
            return when {
                isValidSubscriptionId(defaultSubId) -> merge(flowOf(Bundle.EMPTY), flow)
                else -> flow
            }
        }
    }
}
