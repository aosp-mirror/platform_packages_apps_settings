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

import android.content.Context
import android.net.NetworkStats
import android.net.NetworkTemplate
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Lib class for data usage
 */
object DataUsageLib {
    private const val TAG = "DataUsageLib"

    /**
     * Return mobile NetworkTemplate based on `subId`
     */
    @JvmStatic
    fun getMobileTemplate(context: Context, subId: Int): NetworkTemplate {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)!!
        val mobileDefaultSubId = telephonyManager.subscriptionId
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)!!
        val subInfoList = subscriptionManager.availableSubscriptionInfoList
        if (subInfoList == null) {
            Log.i(TAG, "Subscription is not inited: $subId")
            return getMobileTemplateForSubId(telephonyManager, mobileDefaultSubId)
        }
        for (subInfo in subInfoList) {
            if (subInfo?.subscriptionId == subId) {
                return getNormalizedMobileTemplate(telephonyManager, subId)
            }
        }
        Log.i(TAG, "Subscription is not active: $subId")
        return getMobileTemplateForSubId(telephonyManager, mobileDefaultSubId)
    }

    private fun getNormalizedMobileTemplate(
        telephonyManager: TelephonyManager,
        subId: Int,
    ): NetworkTemplate {
        val mobileTemplate = getMobileTemplateForSubId(telephonyManager, subId)
        val mergedSubscriberIds =
            telephonyManager.createForSubscriptionId(subId).mergedImsisFromGroup
        if (mergedSubscriberIds.isNullOrEmpty()) {
            Log.i(TAG, "mergedSubscriberIds is empty.")
            return mobileTemplate
        }
        return normalizeMobileTemplate(mobileTemplate, mergedSubscriberIds)
    }

    private fun normalizeMobileTemplate(
        template: NetworkTemplate,
        merged: Array<String?>,
    ): NetworkTemplate {
        val subscriberId = template.subscriberIds.firstOrNull() ?: return template
        // In some rare cases (e.g. b/243015487), merged subscriberId list might contain
        // duplicated items. Deduplication for better error handling.
        val mergedSet = merged.toSet()
        if (mergedSet.size != merged.size) {
            Log.wtf(TAG, "Duplicated merged list detected: " + merged.contentToString())
        }
        return if (mergedSet.contains(subscriberId)) {
            // Requested template subscriber is part of the merge group; return
            // a template that matches all merged subscribers.
            NetworkTemplate.Builder(template.matchRule)
                .setSubscriberIds(mergedSet)
                .setMeteredness(template.meteredness)
                .build()
        } else template
    }

    @JvmStatic
    fun getMobileTemplateForSubId(telephonyManager: TelephonyManager, subId: Int): NetworkTemplate {
        // Create template that matches any mobile network when the subscriberId is null.
        val subscriberId = telephonyManager.getSubscriberId(subId)
        return when (subscriberId) {
            null -> NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE)
            else -> NetworkTemplate.Builder(NetworkTemplate.MATCH_CARRIER)
                .setSubscriberIds(setOf(subscriberId))
        }.setMeteredness(NetworkStats.METERED_YES).build()
    }
}
