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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

private const val TAG = "SubscriptionRepository"

class SubscriptionRepository(private val context: Context) {
    private val subscriptionManager = context.requireSubscriptionManager()

    /** A cold flow of a list of subscriptions that are available and visible to the user. */
    fun selectableSubscriptionInfoListFlow(): Flow<List<SubscriptionInfo>> =
        context
            .subscriptionsChangedFlow()
            .map { getSelectableSubscriptionInfoList() }
            .conflate()
            .flowOn(Dispatchers.Default)

    /**
     * Return a list of subscriptions that are available and visible to the user.
     *
     * @return list of user selectable subscriptions.
     */
    fun getSelectableSubscriptionInfoList(): List<SubscriptionInfo> {
        val availableList =
            subscriptionManager.getAvailableSubscriptionInfoList() ?: return emptyList()
        val visibleList =
            availableList.filter { subInfo ->
                // Opportunistic subscriptions are considered invisible to users so they should
                // never be returned.
                SubscriptionUtil.isSubscriptionVisible(subscriptionManager, context, subInfo)
            }
        return visibleList
            .groupBy { it.groupUuid }
            .flatMap { (groupUuid, subInfos) ->
                if (groupUuid == null) {
                    subInfos
                } else {
                    // Multiple subscriptions in a group should only have one representative.
                    // It should be the current active primary subscription if any, or the primary
                    // subscription with minimum subscription id.
                    subInfos
                        .filter { it.simSlotIndex != SubscriptionManager.INVALID_SIM_SLOT_INDEX }
                        .ifEmpty { subInfos.sortedBy { it.subscriptionId } }
                        .take(1)
                }
            }
            // Matching the sorting order in
            // SubscriptionManagerService.getAvailableSubscriptionInfoList
            .sortedWith(compareBy({ it.sortableSimSlotIndex }, { it.subscriptionId }))
            .also { Log.d(TAG, "getSelectableSubscriptionInfoList: $it") }
    }

    /** Flow of whether the subscription visible for the given [subId]. */
    fun isSubscriptionVisibleFlow(subId: Int): Flow<Boolean> {
        return subscriptionsChangedFlow()
            .map {
                val subInfo =
                    subscriptionManager.availableSubscriptionInfoList?.firstOrNull { subInfo ->
                        subInfo.subscriptionId == subId
                    }
                subInfo != null &&
                    SubscriptionUtil.isSubscriptionVisible(subscriptionManager, context, subInfo)
            }
            .conflate()
            .onEach { Log.d(TAG, "[$subId] isSubscriptionVisibleFlow: $it") }
            .flowOn(Dispatchers.Default)
    }

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun collectSubscriptionVisible(
        subId: Int,
        lifecycleOwner: LifecycleOwner,
        action: (Boolean) -> Unit,
    ) {
        isSubscriptionVisibleFlow(subId).collectLatestWithLifecycle(lifecycleOwner, action = action)
    }

    /** Flow of whether the subscription enabled for the given [subId]. */
    fun isSubscriptionEnabledFlow(subId: Int): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)
        return subscriptionsChangedFlow()
            .map { subscriptionManager.isSubscriptionEnabled(subId) }
            .conflate()
            .onEach { Log.d(TAG, "[$subId] isSubscriptionEnabledFlow: $it") }
            .flowOn(Dispatchers.Default)
    }

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun collectSubscriptionEnabled(
        subId: Int,
        lifecycleOwner: LifecycleOwner,
        action: (Boolean) -> Unit,
    ) {
        isSubscriptionEnabledFlow(subId).collectLatestWithLifecycle(lifecycleOwner, action = action)
    }

    fun canDisablePhysicalSubscription() = subscriptionManager.canDisablePhysicalSubscription()

    /** Flow for subscriptions changes. */
    fun subscriptionsChangedFlow() = getSharedSubscriptionsChangedFlow(context)

    /** Flow of active subscription ids. */
    fun activeSubscriptionIdListFlow(): Flow<List<Int>> =
        subscriptionsChangedFlow()
            .map { subscriptionManager.activeSubscriptionIdList.sorted() }
            .distinctUntilChanged()
            .conflate()
            .onEach { Log.d(TAG, "activeSubscriptionIdList: $it") }
            .flowOn(Dispatchers.Default)

    fun activeSubscriptionInfoFlow(subId: Int): Flow<SubscriptionInfo?> =
        subscriptionsChangedFlow()
            .map { subscriptionManager.getActiveSubscriptionInfo(subId) }
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun phoneNumberFlow(subId: Int): Flow<String?> =
        activeSubscriptionInfoFlow(subId).flatMapLatest { subInfo ->
            if (subInfo != null) {
                context.phoneNumberFlow(subInfo)
            } else {
                flowOf(null)
            }
        }

    companion object {
        private lateinit var SharedSubscriptionsChangedFlow: Flow<Unit>

        private fun getSharedSubscriptionsChangedFlow(context: Context): Flow<Unit> {
            if (!this::SharedSubscriptionsChangedFlow.isInitialized) {
                SharedSubscriptionsChangedFlow =
                    context.applicationContext
                        .requireSubscriptionManager()
                        .subscriptionsChangedFlow()
                        .shareIn(
                            scope = CoroutineScope(Dispatchers.Default),
                            started = SharingStarted.WhileSubscribed(),
                            replay = 1,
                        )
            }
            return SharedSubscriptionsChangedFlow
        }

        /**
         * Flow for subscriptions changes.
         *
         * Note: Even the SubscriptionManager.addOnSubscriptionsChangedListener's doc says the
         * SubscriptionManager.OnSubscriptionsChangedListener.onSubscriptionsChanged() method will
         * also be invoked once initially when calling it, there still case that the
         * onSubscriptionsChanged() method is not invoked initially. For example, when the
         * onSubscriptionsChanged event never happens before, on a device never ever has any
         * subscriptions.
         */
        private fun SubscriptionManager.subscriptionsChangedFlow() =
            callbackFlow {
                    val listener =
                        object : SubscriptionManager.OnSubscriptionsChangedListener() {
                            override fun onSubscriptionsChanged() {
                                trySend(Unit)
                            }

                            override fun onAddListenerFailed() {
                                close()
                            }
                        }

                    addOnSubscriptionsChangedListener(Dispatchers.Default.asExecutor(), listener)

                    awaitClose { removeOnSubscriptionsChangedListener(listener) }
                }
                .onStart { emit(Unit) } // Ensure this flow is never empty
                .conflate()
                .onEach { Log.d(TAG, "subscriptions changed") }
                .flowOn(Dispatchers.Default)
    }
}

val Context.subscriptionManager: SubscriptionManager?
    get() = getSystemService(SubscriptionManager::class.java)

fun Context.requireSubscriptionManager(): SubscriptionManager = subscriptionManager!!

fun Context.phoneNumberFlow(subscriptionInfo: SubscriptionInfo): Flow<String?> =
    subscriptionsChangedFlow()
        .map { SubscriptionUtil.getBidiFormattedPhoneNumber(this, subscriptionInfo) }
        .distinctUntilChanged()
        .conflate()
        .flowOn(Dispatchers.Default)

fun Context.subscriptionsChangedFlow(): Flow<Unit> =
    SubscriptionRepository(this).subscriptionsChangedFlow()

/** Subscription with invalid sim slot index has lowest sort order. */
private val SubscriptionInfo.sortableSimSlotIndex: Int
    get() = if (simSlotIndex != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
        simSlotIndex
    } else {
        Int.MAX_VALUE
    }
