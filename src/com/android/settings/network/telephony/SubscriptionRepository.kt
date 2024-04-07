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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private const val TAG = "SubscriptionRepository"

class SubscriptionRepository(private val context: Context) {
    /**
     * Return a list of subscriptions that are available and visible to the user.
     *
     * @return list of user selectable subscriptions.
     */
    fun getSelectableSubscriptionInfoList(): List<SubscriptionInfo> =
        context.getSelectableSubscriptionInfoList()

    fun isSubscriptionEnabledFlow(subId: Int) = context.isSubscriptionEnabledFlow(subId)

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun collectSubscriptionEnabled(
        subId: Int,
        lifecycleOwner: LifecycleOwner,
        action: (Boolean) -> Unit,
    ) {
        isSubscriptionEnabledFlow(subId).collectLatestWithLifecycle(lifecycleOwner, action = action)
    }

}

val Context.subscriptionManager: SubscriptionManager?
    get() = getSystemService(SubscriptionManager::class.java)

fun Context.requireSubscriptionManager(): SubscriptionManager = subscriptionManager!!

fun Context.isSubscriptionEnabledFlow(subId: Int) = subscriptionsChangedFlow().map {
    subscriptionManager?.isSubscriptionEnabled(subId) ?: false
}.conflate().onEach { Log.d(TAG, "[$subId] isSubscriptionEnabledFlow: $it") }
    .flowOn(Dispatchers.Default)

fun Context.phoneNumberFlow(subscriptionInfo: SubscriptionInfo) = subscriptionsChangedFlow().map {
    SubscriptionUtil.getFormattedPhoneNumber(this, subscriptionInfo)
}.filterNot { it.isNullOrEmpty() }.flowOn(Dispatchers.Default)

fun Context.subscriptionsChangedFlow() = callbackFlow {
    val subscriptionManager = requireSubscriptionManager()

    val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            trySend(Unit)
        }
    }

    subscriptionManager.addOnSubscriptionsChangedListener(
        Dispatchers.Default.asExecutor(),
        listener,
    )

    awaitClose { subscriptionManager.removeOnSubscriptionsChangedListener(listener) }
}.conflate().onEach { Log.d(TAG, "subscriptions changed") }.flowOn(Dispatchers.Default)

/**
 * Return a list of subscriptions that are available and visible to the user.
 *
 * @return list of user selectable subscriptions.
 */
fun Context.getSelectableSubscriptionInfoList(): List<SubscriptionInfo> {
    val subscriptionManager = requireSubscriptionManager()
    val availableList = subscriptionManager.getAvailableSubscriptionInfoList() ?: return emptyList()
    val visibleList = availableList.filter { subInfo ->
        // Opportunistic subscriptions are considered invisible
        // to users so they should never be returned.
        SubscriptionUtil.isSubscriptionVisible(subscriptionManager, this, subInfo)
    }
    // Multiple subscriptions in a group should only have one representative.
    // It should be the current active primary subscription if any, or any primary subscription.
    val groupUuidToSelectedIdMap = visibleList
        .groupBy { it.groupUuid }
        .mapValues { (_, subInfos) ->
            subInfos.filter { it.simSlotIndex != SubscriptionManager.INVALID_SIM_SLOT_INDEX }
                .ifEmpty { subInfos }
                .minOf { it.subscriptionId }
        }

    return visibleList
        .filter { subInfo ->
            val groupUuid = subInfo.groupUuid ?: return@filter true
            groupUuidToSelectedIdMap[groupUuid] == subInfo.subscriptionId
        }
        .sortedBy { it.subscriptionId }
        .also { Log.d(TAG, "getSelectableSubscriptionInfoList: $it") }
}
