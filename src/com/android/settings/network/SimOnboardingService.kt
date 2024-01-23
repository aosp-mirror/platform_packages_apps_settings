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

package com.android.settings.network

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccCardInfo
import android.telephony.UiccSlotInfo
import android.util.Log
import com.android.settingslib.utils.ThreadUtils


private const val TAG = "SimOnboardingService"
private const val INVALID = -1

class SimOnboardingService {
    var subscriptionManager:SubscriptionManager? = null
    var telephonyManager:TelephonyManager? = null

    var targetSubId: Int = INVALID
    var targetSubInfo: SubscriptionInfo? = null
    var availableSubInfoList: List<SubscriptionInfo> = listOf()
    var activeSubInfoList: List<SubscriptionInfo> = listOf()
    var slotInfoList: List<UiccSlotInfo> = listOf()
    var uiccCardInfoList: List<UiccCardInfo> = listOf()
    var selectedSubInfoList: MutableList<SubscriptionInfo> = mutableListOf()
    var targetPrimarySimCalls: Int = -1
    var targetPrimarySimTexts: Int = -1
    var targetPrimarySimMobileData: Int = -1
    var isMultipleEnabledProfilesSupported: Boolean = false
        get() {
            if (uiccCardInfoList.isEmpty()) {
                Log.w(TAG, "UICC cards info list is empty.")
                return false
            }
            return uiccCardInfoList.stream()
                .anyMatch { cardInfo: UiccCardInfo -> cardInfo.isMultipleEnabledProfilesSupported }
        }
    var renameMutableMap : MutableMap<Int, String> = mutableMapOf()

    fun isValid(): Boolean {
        return targetSubId != INVALID
            && targetSubInfo != null
            && activeSubInfoList.isNotEmpty()
            && slotInfoList.isNotEmpty()
            && selectedSubInfoList.isNotEmpty()
    }

    fun clear() {
        targetSubId = -1
        targetSubInfo = null
        availableSubInfoList = listOf()
        activeSubInfoList = listOf()
        slotInfoList = listOf()
        uiccCardInfoList = listOf()
        selectedSubInfoList = mutableListOf()
        targetPrimarySimCalls = -1
        targetPrimarySimTexts = -1
        targetPrimarySimMobileData = -1
        renameMutableMap.clear()
    }

    fun initData(inputTargetSubId:Int,context: Context) {
        targetSubId = inputTargetSubId
        subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        telephonyManager = context.getSystemService(TelephonyManager::class.java)

        ThreadUtils.postOnBackgroundThread {
            activeSubInfoList = SubscriptionUtil.getActiveSubscriptions(subscriptionManager)
            availableSubInfoList = SubscriptionUtil.getAvailableSubscriptions(context)
            targetSubInfo = availableSubInfoList.find { subInfo -> subInfo.subscriptionId == targetSubId }
            Log.d(
                TAG, "targetSubId: $targetSubId" + ", targetSubInfo: $targetSubInfo" +
                    ". activeSubInfoList: $activeSubInfoList"
            )
            slotInfoList = telephonyManager?.uiccSlotsInfo?.toList() ?: listOf()
            Log.d(TAG, "slotInfoList: $slotInfoList.")
            uiccCardInfoList = telephonyManager?.uiccCardsInfo!!
            Log.d(TAG, "uiccCardInfoList: $uiccCardInfoList")

            Log.d(TAG, "isMultipleEnabledProfilesSupported: $isMultipleEnabledProfilesSupported")
        }
    }

    fun getSelectableSubscriptionInfo(): List<SubscriptionInfo> {
        var list: MutableList<SubscriptionInfo> = mutableListOf()
        list.addAll(activeSubInfoList)
        if (!list.contains(targetSubInfo)) {
            targetSubInfo?.let { list.add(it) }
        }

        Log.d(TAG, "list: $list")
        return list.toList()
    }

    fun addItemForRenaming(subInfo: SubscriptionInfo, newName: String) {
        if (subInfo.displayName == newName) {
            return
        }
        renameMutableMap[subInfo.subscriptionId] = newName
    }

    fun getSubscriptionInfoDisplayName(subInfo: SubscriptionInfo): String {
        return renameMutableMap[subInfo.subscriptionId] ?: subInfo.displayName.toString()
    }

    fun startActivatingSim(callback:() -> Unit){
        // TODO: start to activate sim
    }
}