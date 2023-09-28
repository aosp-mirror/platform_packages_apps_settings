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

package com.android.settings.datausage.lib

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settingslib.AppItem
import com.android.settingslib.net.UidDetailProvider
import com.android.settingslib.spaprivileged.framework.common.userManager

class AppDataUsageRepository(
    private val context: Context,
    private val currentUserId: Int,
    private val template: NetworkTemplate,
    private val getPackageName: (AppItem) -> String?,
) {
    private val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)!!

    fun getAppPercent(carrierId: Int?, startTime: Long, endTime: Long): List<Pair<AppItem, Int>> {
        val networkStats = querySummary(startTime, endTime) ?: return emptyList()
        return getAppPercent(carrierId, convertToBuckets(networkStats))
    }

    @VisibleForTesting
    fun getAppPercent(carrierId: Int?, buckets: List<Bucket>): List<Pair<AppItem, Int>> {
        val items = ArrayList<AppItem>()
        val knownItems = SparseArray<AppItem>()
        val profiles = context.userManager.userProfiles
        bindStats(buckets, profiles, knownItems, items)
        val restrictedUids = context.getSystemService(NetworkPolicyManager::class.java)!!
            .getUidsWithPolicy(NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND)
        for (uid in restrictedUids) {
            // Only splice in restricted state for current user or managed users
            if (!profiles.contains(UserHandle.getUserHandleForUid(uid))) {
                continue
            }
            var item = knownItems[uid]
            if (item == null) {
                item = AppItem(uid)
                item.total = 0
                item.addUid(uid)
                items.add(item)
                knownItems.put(item.key, item)
            }
            item.restricted = true
        }

        val filteredItems = filterItems(carrierId, items).sorted()
        val largest: Long = filteredItems.maxOfOrNull { it.total } ?: 0
        return filteredItems.map { item ->
            val percentTotal = if (largest > 0) (item.total * 100 / largest).toInt() else 0
            item to percentTotal
        }
    }

    private fun querySummary(startTime: Long, endTime: Long): NetworkStats? = try {
        networkStatsManager.querySummary(template, startTime, endTime)
    } catch (e: RuntimeException) {
        Log.e(TAG, "Exception querying network detail.", e)
        null
    }

    private fun filterItems(carrierId: Int?, items: List<AppItem>): List<AppItem> {
        // When there is no specified SubscriptionInfo, Wi-Fi data usage will be displayed.
        // In this case, the carrier service package also needs to be hidden.
        if (carrierId != null && carrierId !in context.resources.getIntArray(
                R.array.datausage_hiding_carrier_service_carrier_id
            )
        ) {
            return items
        }
        val hiddenPackageNames = context.resources.getStringArray(
            R.array.datausage_hiding_carrier_service_package_names
        )
        return items.filter { item ->
            // Do not show carrier service package in data usage list if it should be hidden for
            // the carrier.
            getPackageName(item) !in hiddenPackageNames
        }
    }

    private fun bindStats(
        buckets: List<Bucket>,
        profiles: MutableList<UserHandle>,
        knownItems: SparseArray<AppItem>,
        items: ArrayList<AppItem>,
    ) {
        for (bucket in buckets) {
            // Decide how to collapse items together
            val uid = bucket.uid
            val collapseKey: Int
            val category: Int
            val userId = UserHandle.getUserId(uid)
            if (UserHandle.isApp(uid) || Process.isSdkSandboxUid(uid)) {
                if (profiles.contains(UserHandle(userId))) {
                    if (userId != currentUserId) {
                        // Add to a managed user item.
                        accumulate(
                            collapseKey = UidDetailProvider.buildKeyForUser(userId),
                            knownItems = knownItems,
                            bucket = bucket,
                            itemCategory = AppItem.CATEGORY_USER,
                            items = items,
                        )
                    }
                    collapseKey = getAppUid(uid)
                    category = AppItem.CATEGORY_APP
                } else {
                    // If it is a removed user add it to the removed users' key
                    if (context.userManager.getUserInfo(userId) == null) {
                        collapseKey = NetworkStats.Bucket.UID_REMOVED
                        category = AppItem.CATEGORY_APP
                    } else {
                        // Add to other user item.
                        collapseKey = UidDetailProvider.buildKeyForUser(userId)
                        category = AppItem.CATEGORY_USER
                    }
                }
            } else if (uid == NetworkStats.Bucket.UID_REMOVED ||
                uid == NetworkStats.Bucket.UID_TETHERING ||
                uid == Process.OTA_UPDATE_UID
            ) {
                collapseKey = uid
                category = AppItem.CATEGORY_APP
            } else {
                collapseKey = Process.SYSTEM_UID
                category = AppItem.CATEGORY_APP
            }
            accumulate(
                collapseKey = collapseKey,
                knownItems = knownItems,
                bucket = bucket,
                itemCategory = category,
                items = items,
            )
        }
    }

    /**
     * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
     * Creates the item if needed.
     *
     * @param collapseKey  the collapse key used to map the item.
     * @param knownItems   collection of known (already existing) items.
     * @param bucket       the network stats bucket to extract data usage from.
     * @param itemCategory the item is categorized on the list view by this category. Must be
     */
    private fun accumulate(
        collapseKey: Int,
        knownItems: SparseArray<AppItem>,
        bucket: Bucket,
        itemCategory: Int,
        items: ArrayList<AppItem>,
    ) {
        var item = knownItems[collapseKey]
        if (item == null) {
            item = AppItem(collapseKey)
            item.category = itemCategory
            items.add(item)
            knownItems.put(item.key, item)
        }
        item.addUid(bucket.uid)
        item.total += bucket.bytes
    }

    companion object {
        private const val TAG = "AppDataUsageRepository"

        @VisibleForTesting
        data class Bucket(
            val uid: Int,
            val bytes: Long,
        )

        @JvmStatic
        fun getAppUid(uid: Int): Int {
            if (Process.isSdkSandboxUid(uid)) {
                // For a sandbox process, get the associated app UID
                return Process.getAppUidForSdkSandboxUid(uid)
            }
            return uid
        }

        private fun convertToBuckets(stats: NetworkStats): List<Bucket> {
            val buckets = mutableListOf<Bucket>()
            stats.use {
                val bucket = NetworkStats.Bucket()
                while (it.getNextBucket(bucket)) {
                    buckets += Bucket(uid = bucket.uid, bytes = bucket.rxBytes + bucket.txBytes)
                }
            }
            return buckets
        }
    }
}
