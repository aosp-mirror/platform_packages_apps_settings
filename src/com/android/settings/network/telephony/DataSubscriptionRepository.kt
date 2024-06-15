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
import android.content.IntentFilter
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class DataSubscriptionRepository(
    private val context: Context,
    private val getDisplayName: (subId: Int) -> String = { subId ->
        SubscriptionUtil.getUniqueSubscriptionDisplayName(subId, context).toString()
    },
) {
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)!!
    private val subscriptionManager = context.requireSubscriptionManager()

    fun defaultDataSubscriptionIdFlow(): Flow<Int> =
        context
            .broadcastReceiverFlow(
                IntentFilter(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)
            )
            .map { it.getIntExtra(SUBSCRIPTION_KEY, SubscriptionManager.INVALID_SUBSCRIPTION_ID) }
            .onStart { emit(SubscriptionManager.getDefaultDataSubscriptionId()) }
            .conflate()
            .flowOn(Dispatchers.Default)

    fun activeDataSubscriptionIdFlow(): Flow<Int> =
        telephonyManager.telephonyCallbackFlow {
            object : TelephonyCallback(), TelephonyCallback.ActiveDataSubscriptionIdListener {
                override fun onActiveDataSubscriptionIdChanged(subId: Int) {
                    trySend(subId)
                    Log.d(TAG, "activeDataSubscriptionIdFlow: $subId")
                }
            }
        }

    fun dataSummaryFlow(): Flow<String> =
        combine(defaultDataSubscriptionIdFlow(), activeDataSubscriptionIdFlow()) {
                defaultSubId,
                activeSubId ->
                DataSubscriptionIds(defaultSubId, activeSubId)
            }
            .distinctUntilChanged()
            .map { it.getDataSummary() }
            .conflate()
            .flowOn(Dispatchers.Default)

    private data class DataSubscriptionIds(
        val defaultSubId: Int,
        val activeSubId: Int,
    )

    private fun DataSubscriptionIds.getDataSummary(): String {
        val activeSubInfo = subscriptionManager.getActiveSubscriptionInfo(activeSubId) ?: return ""
        if (!SubscriptionUtil.isSubscriptionVisible(subscriptionManager, context, activeSubInfo)) {
            return getDisplayName(defaultSubId)
        }
        val uniqueName = getDisplayName(activeSubId)
        return if (activeSubId == defaultSubId) {
            uniqueName
        } else {
            context.getString(R.string.mobile_data_temp_using, uniqueName)
        }
    }

    companion object {
        private const val TAG = "DataSubscriptionRepo"

        @VisibleForTesting const val SUBSCRIPTION_KEY = "subscription"
    }
}
