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
import android.content.Context
import android.content.pm.UserProperties
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.SparseArray
import android.util.SparseBooleanArray
import androidx.annotation.VisibleForTesting
import androidx.core.util.keyIterator
import androidx.core.util.valueIterator
import com.android.settings.R
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.Bucket
import com.android.settingslib.AppItem
import com.android.settingslib.net.UidDetailProvider
import com.android.settingslib.spaprivileged.framework.common.userManager

class AppDataUsageRepository(
    private val context: Context,
    private val currentUserId: Int,
    template: NetworkTemplate,
    private val getPackageName: (AppItem) -> String?,
) {
    private val networkStatsRepository = NetworkStatsRepository(context, template)

    fun getAppPercent(carrierId: Int?, startTime: Long, endTime: Long): List<Pair<AppItem, Int>> {
        val buckets = networkStatsRepository.queryBuckets(startTime, endTime)
        return getAppPercent(carrierId, buckets)
    }

    @VisibleForTesting
    fun getAppPercent(carrierId: Int?, buckets: List<Bucket>): List<Pair<AppItem, Int>> {
        val knownItems = SparseArray<AppItem>()
        val profiles = context.userManager.userProfiles
        val userManager : UserManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val userIdToIsHiddenMap = profiles.associate { profile ->
            profile.identifier to shouldSkipProfile(userManager, profile)
        }
        bindStats(buckets, userIdToIsHiddenMap, knownItems)
        val restrictedUids = context.getSystemService(NetworkPolicyManager::class.java)!!
            .getUidsWithPolicy(NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND)
        for (uid in restrictedUids) {
            // Only splice in restricted state for current user or managed users
            if (UserHandle.getUserHandleForUid(uid) !in profiles) continue
            val item =
                knownItems[uid]
                    ?: AppItem(uid).apply {
                        category = AppItem.CATEGORY_APP
                        addUid(uid)
                        knownItems.put(uid, this)
                    }
            item.restricted = true
        }

        val filteredItems =
            filterItems(carrierId, knownItems.valueIterator().asSequence().toList()).sorted()
        val largest: Long = filteredItems.maxOfOrNull { it.total } ?: 0
        return filteredItems.map { item ->
            val percentTotal = if (largest > 0) (item.total * 100 / largest).toInt() else 0
            item to percentTotal
        }
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
        userIdToIsHiddenMap: Map<Int, Boolean>,
        knownItems: SparseArray<AppItem>,
    ) {
        for (bucket in buckets) {
            // Decide how to collapse items together
            val uid = bucket.uid
            val collapseKey: Int
            val category: Int
            val userId = UserHandle.getUserId(uid)
            if(userIdToIsHiddenMap[userId] == true) {
                continue
            }
            if (UserHandle.isApp(uid) || Process.isSdkSandboxUid(uid)) {
                if (userIdToIsHiddenMap.keys.contains(userId)) {
                    if (userId != currentUserId) {
                        // Add to a managed user item.
                        accumulate(
                            collapseKey = UidDetailProvider.buildKeyForUser(userId),
                            knownItems = knownItems,
                            bucket = bucket,
                            itemCategory = AppItem.CATEGORY_USER,
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
            )
        }
    }

    private fun shouldSkipProfile(userManager : UserManager, userHandle: UserHandle): Boolean {
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()
                && android.multiuser.Flags.handleInterleavedSettingsForPrivateSpace()) {
            return (userManager.isQuietModeEnabled(userHandle)
                    && userManager.getUserProperties(userHandle).showInQuietMode
                    == UserProperties.SHOW_IN_QUIET_MODE_HIDDEN)
        }
        return false
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
    ) {
        val item =
            knownItems[collapseKey]
                ?: AppItem(collapseKey).apply {
                    category = itemCategory
                    knownItems.put(collapseKey, this)
                }
        item.addUid(bucket.uid)
        item.total += bucket.bytes
    }

    companion object {
        @JvmStatic
        fun getAppUidList(uids: SparseBooleanArray) =
            uids.keyIterator().asSequence().map { getAppUid(it) }.distinct().toList()

        @JvmStatic
        fun getAppUid(uid: Int): Int {
            if (Process.isSdkSandboxUid(uid)) {
                // For a sandbox process, get the associated app UID
                return Process.getAppUidForSdkSandboxUid(uid)
            }
            return uid
        }

        /**
         * Gets the apps' uids, also add the apps' SDK sandboxes' uids.
         *
         * In case we've been asked data usage for an app, include data usage of the corresponding
         * SDK sandbox.
         */
        fun withSdkSandboxUids(uids: List<Int>): List<Int> {
            val set = uids.toMutableSet()
            for (uid in uids) {
                if (Process.isApplicationUid(uid)) {
                    set += Process.toSdkSandboxUid(uid)
                }
            }
            return set.toList()
        }
    }
}
