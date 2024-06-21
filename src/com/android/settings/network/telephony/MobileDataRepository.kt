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
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.MobileDataPolicy
import android.util.Log
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class MobileDataRepository(
    private val context: Context,
    private val subscriptionsChangedFlow: Flow<Unit> = context.subscriptionsChangedFlow(),
) {
    fun isMobileDataPolicyEnabledFlow(subId: Int, @MobileDataPolicy policy: Int): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)
        val telephonyManager = context.telephonyManager(subId)
        return subscriptionsChangedFlow
            .map { telephonyManager.isMobileDataPolicyEnabled(policy) }
            .distinctUntilChanged()
            .conflate()
            .onEach { Log.d(TAG, "[$subId] isMobileDataPolicyEnabled($policy): $it") }
            .flowOn(Dispatchers.Default)
    }

    fun setMobileDataPolicyEnabled(subId: Int, @MobileDataPolicy policy: Int, enabled: Boolean) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return
        Log.d(TAG, "[$subId] setMobileDataPolicyEnabled($policy): $enabled")
        context.telephonyManager(subId).setMobileDataPolicyEnabled(policy, enabled)
    }

    fun setAutoDataSwitch(subId: Int, newEnabled: Boolean) {
        setMobileDataPolicyEnabled(
            subId = subId,
            policy = TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
            enabled = newEnabled,
        )
    }

    /**
     * Flow for mobile data enabled changed event.
     *
     * Note: This flow can only notify enabled status changes, cannot provide the latest status.
     */
    fun mobileDataEnabledChangedFlow(subId: Int, sendInitialValue: Boolean = true): Flow<Unit> =
        mobileSettingsGlobalChangedFlow(Settings.Global.MOBILE_DATA, subId, sendInitialValue)

    private fun mobileSettingsGlobalChangedFlow(
        name: String,
        subId: Int,
        sendInitialValue: Boolean = true,
    ): Flow<Unit> {
        val flow = context.settingsGlobalChangeFlow(name, sendInitialValue)
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flow
        val subIdFlow =
            context.settingsGlobalChangeFlow(name = name + subId, sendInitialValue = false)
        return merge(flow, subIdFlow)
    }

    fun isMobileDataEnabledFlow(subId: Int): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)
        val telephonyManager = context.telephonyManager(subId)
        return mobileDataEnabledChangedFlow(subId)
            .map {
                telephonyManager.isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER)
            }
            .catch { e ->
                Log.w(TAG, "[$subId] isMobileDataEnabledFlow: exception", e)
                emit(false)
            }
            .distinctUntilChanged()
            .conflate()
            .onEach { Log.d(TAG, "[$subId] isMobileDataEnabledFlow: $it") }
            .flowOn(Dispatchers.Default)
    }

    fun setMobileDataEnabled(
        subId: Int,
        enabled: Boolean,
        wifiPickerTrackerHelper: WifiPickerTrackerHelper? = null,
    ) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return

        Log.d(TAG, "setMobileDataEnabled: $enabled")
        MobileNetworkUtils.setMobileDataEnabled(
            context, subId, enabled, /* disableOtherSubscriptions= */ true)

        if (wifiPickerTrackerHelper != null &&
            !wifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(subId)) {
            wifiPickerTrackerHelper.setCarrierNetworkEnabled(enabled)
        }
    }

    /** Creates an instance of a cold Flow for whether data roaming is enabled of given [subId]. */
    fun isDataRoamingEnabledFlow(subId: Int): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)
        val telephonyManager = context.telephonyManager(subId)
        return mobileSettingsGlobalChangedFlow(Settings.Global.DATA_ROAMING, subId)
            .map { telephonyManager.isDataRoamingEnabled }
            .distinctUntilChanged()
            .conflate()
            .onEach { Log.d(TAG, "[$subId] isDataRoamingEnabledFlow: $it") }
            .flowOn(Dispatchers.Default)
    }

    private companion object {
        private const val TAG = "MobileDataRepository"
    }
}
