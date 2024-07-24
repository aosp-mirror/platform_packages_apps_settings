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
import com.android.settings.network.SimOnboardingActivity.Companion.CallbackType
import com.android.settings.spa.network.setAutomaticData
import com.android.settings.spa.network.setDefaultData
import com.android.settings.spa.network.setDefaultSms
import com.android.settings.spa.network.setDefaultVoice
import com.android.settingslib.utils.ThreadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SimOnboardingService"
private const val INVALID = SubscriptionManager.INVALID_SUBSCRIPTION_ID

class SimOnboardingService {
    var subscriptionManager:SubscriptionManager? = null
    var telephonyManager:TelephonyManager? = null

    var targetSubId: Int = INVALID
    var targetSubInfo: SubscriptionInfo? = null
    var availableSubInfoList: List<SubscriptionInfo> = listOf()
    var activeSubInfoList: List<SubscriptionInfo> = listOf()
    var slotInfoList: List<UiccSlotInfo> = listOf()
    var uiccCardInfoList: List<UiccCardInfo> = listOf()
    var targetPrimarySimCalls: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var targetPrimarySimTexts: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var targetPrimarySimMobileData: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var targetPrimarySimAutoDataSwitch: Boolean = false
    var targetNonDds: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        get() {
            if(targetPrimarySimMobileData == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.w(TAG, "No DDS")
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }
            return userSelectedSubInfoList
                .filter { info -> info.subscriptionId != targetPrimarySimMobileData }
                .map { it.subscriptionId }
                .firstOrNull() ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
    var callback: (CallbackType) -> Unit = {}

    var isMultipleEnabledProfilesSupported: Boolean = false
        get() {
            if (uiccCardInfoList.isEmpty()) {
                Log.w(TAG, "UICC cards info list is empty.")
                return false
            }
            return  uiccCardInfoList.any { it.isMultipleEnabledProfilesSupported }
        }
    var isRemovableSimEnabled: Boolean = false
        get() {
            if(slotInfoList.isEmpty()) {
                Log.w(TAG, "UICC Slot info list is empty.")
                return false
            }
            return UiccSlotUtil.isRemovableSimEnabled(slotInfoList)
        }

    var doesTargetSimHaveEsimOperation = false
        get() {
            return targetSubInfo?.isEmbedded ?: false
        }

    var isUsableTargetSubscriptionId = false
        get() {
            return SubscriptionManager.isUsableSubscriptionId(targetSubId)
        }
    var getActiveModemCount = 0
        get() {
            return telephonyManager?.getActiveModemCount() ?: 0
        }

    var renameMutableMap : MutableMap<Int, String> = mutableMapOf()
    var userSelectedSubInfoList : MutableList<SubscriptionInfo> = mutableListOf()

    var isSimSelectionFinished = false
        get() {
            return getActiveModemCount != 0 && userSelectedSubInfoList.size == getActiveModemCount
        }

    var isAllOfSlotAssigned = false
        get() {
            if(getActiveModemCount == 0){
                Log.e(TAG, "isAllOfSlotAssigned: getActiveModemCount is 0")
                return true
            }
            return getActiveModemCount != 0 && activeSubInfoList.size == getActiveModemCount
        }

    fun isValid(): Boolean {
        return targetSubId != INVALID
            && targetSubInfo != null
            && activeSubInfoList.isNotEmpty()
            && slotInfoList.isNotEmpty()
    }

    fun clear() {
        targetSubId = -1
        targetSubInfo = null
        availableSubInfoList = listOf()
        activeSubInfoList = listOf()
        slotInfoList = listOf()
        uiccCardInfoList = listOf()
        targetPrimarySimCalls = -1
        targetPrimarySimTexts = -1
        targetPrimarySimMobileData = -1
        clearUserRecord()
    }

    fun clearUserRecord(){
        renameMutableMap.clear()
        userSelectedSubInfoList.clear()
    }

    fun initData(inputTargetSubId: Int,
                 context: Context,
                 callback: (CallbackType) -> Unit) {
        this.callback = callback
        targetSubId = inputTargetSubId
        subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        telephonyManager = context.getSystemService(TelephonyManager::class.java)
        Log.d(
            TAG, "startInit: targetSubId:$targetSubId, activeSubInfoList: $activeSubInfoList"
        )
        activeSubInfoList = SubscriptionUtil.getActiveSubscriptions(subscriptionManager)

        ThreadUtils.postOnBackgroundThread {
            availableSubInfoList = SubscriptionUtil.getAvailableSubscriptions(context)
            targetSubInfo =
                availableSubInfoList.find { subInfo -> subInfo.subscriptionId == targetSubId }
            targetSubInfo?.let { userSelectedSubInfoList.add(it) }
            Log.d(TAG, "targetSubId: $targetSubId , targetSubInfo: $targetSubInfo")
            slotInfoList = telephonyManager?.uiccSlotsInfo?.toList() ?: listOf()
            Log.d(TAG, "slotInfoList: $slotInfoList.")
            uiccCardInfoList = telephonyManager?.uiccCardsInfo!!
            Log.d(TAG, "uiccCardInfoList: $uiccCardInfoList")

            targetPrimarySimCalls = SubscriptionManager.getDefaultVoiceSubscriptionId()
            targetPrimarySimTexts = SubscriptionManager.getDefaultSmsSubscriptionId()
            targetPrimarySimMobileData = SubscriptionManager.getDefaultDataSubscriptionId()
            Log.d(
                TAG,"doesTargetSimHaveEsimOperation: $doesTargetSimHaveEsimOperation" +
                    ", isRemovableSimEnabled: $isRemovableSimEnabled" +
                    ", isMultipleEnabledProfilesSupported: $isMultipleEnabledProfilesSupported" +
                    ", targetPrimarySimCalls: $targetPrimarySimCalls" +
                    ", targetPrimarySimTexts: $targetPrimarySimTexts" +
                    ", targetPrimarySimMobileData: $targetPrimarySimMobileData")
        }
    }

    /**
     * Return the subscriptionInfo list which has
     * the target subscriptionInfo + active subscriptionInfo.
     */
    fun getSelectableSubscriptionInfoList(): List<SubscriptionInfo> {
        var list: MutableList<SubscriptionInfo> = mutableListOf()
        list.addAll(activeSubInfoList)
        if (!list.contains(targetSubInfo)) {
            targetSubInfo?.let { list.add(it) }
        }

        return list.toList()
    }

    /**
     * Return the user selected SubscriptionInfo list.
     */
    fun getSelectedSubscriptionInfoList(): List<SubscriptionInfo> {
        if (userSelectedSubInfoList.isEmpty()){
            Log.d(TAG, "userSelectedSubInfoList is empty")
            return activeSubInfoList
        }
        return userSelectedSubInfoList.toList()
    }

    fun getSelectedSubscriptionInfoListWithRenaming(): List<SubscriptionInfo> {
        if (userSelectedSubInfoList.isEmpty()){
            Log.d(TAG, "userSelectedSubInfoList is empty")
            return activeSubInfoList
        }
        return userSelectedSubInfoList.map {
            SubscriptionInfo.Builder(it).setDisplayName(getSubscriptionInfoDisplayName(it)).build()
        }.toList()
    }

    fun addItemForRenaming(subInfo: SubscriptionInfo, newName: String) {
        if (subInfo.displayName == newName) {
            return
        }
        renameMutableMap[subInfo.subscriptionId] = newName
        Log.d(
            TAG,
            "renameMutableMap add ${subInfo.subscriptionId} & $newName into: $renameMutableMap"
        )
    }

    fun getSubscriptionInfoDisplayName(subInfo: SubscriptionInfo): String {
        return renameMutableMap[subInfo.subscriptionId] ?: subInfo.displayName.toString()
    }

    fun addCurrentItemForSelectedSim() {
        if (userSelectedSubInfoList.size < getActiveModemCount) {
            userSelectedSubInfoList.addAll(activeSubInfoList)
            Log.d(TAG, "addCurrentItemForSelectedSim: userSelectedSubInfoList:" +
                    ", $userSelectedSubInfoList")
        }
    }

    fun addItemForSelectedSim(selectedSubInfo: SubscriptionInfo) {
        userSelectedSubInfoList.add(selectedSubInfo)
    }

    fun removeItemForSelectedSim(selectedSubInfo: SubscriptionInfo) {
        if (userSelectedSubInfoList.contains(selectedSubInfo)) {
            userSelectedSubInfoList.remove(selectedSubInfo)
        }
    }

    /**
     * Return the subscriptionInfo which will be removed in the slot during the sim onboarding.
     * If return Null, then no subscriptionInfo will be removed in the slot.
     */
    fun getRemovedSim():SubscriptionInfo?{
        return activeSubInfoList.find { !userSelectedSubInfoList.contains(it) }
    }

    fun handleTogglePsimAction() {
        val canDisablePhysicalSubscription =
            subscriptionManager?.canDisablePhysicalSubscription() == true
        if (targetSubInfo != null && canDisablePhysicalSubscription) {
            // TODO: to support disable case.
            subscriptionManager?.setUiccApplicationsEnabled(
                    targetSubInfo!!.subscriptionId, /*enabled=*/true)
        } else {
            Log.i(TAG, "The device does not support toggling pSIM. It is enough to just "
                    + "enable the removable slot."
            )
        }
    }

    fun startActivatingSim(){
        // TODO: start to activate sim
        callback(CallbackType.CALLBACK_FINISH)
    }

    suspend fun startSetupName() {
        withContext(Dispatchers.Default) {
            renameMutableMap.forEach {
                subscriptionManager?.setDisplayName(
                    it.value, it.key,
                    SubscriptionManager.NAME_SOURCE_USER_INPUT
                )
            }
            // next action is SETUP_PRIMARY_SIM
            callback(CallbackType.CALLBACK_SETUP_PRIMARY_SIM)
        }
    }

    suspend fun startSetupPrimarySim(context: Context) {
        withContext(Dispatchers.Default) {
            setDefaultVoice(subscriptionManager,targetPrimarySimCalls)
            setDefaultSms(subscriptionManager,targetPrimarySimTexts)
            setDefaultData(
                context,
                subscriptionManager,
                null,
                targetPrimarySimMobileData
            )

            var nonDds = targetNonDds
            Log.d(
                TAG,
                "setAutomaticData: targetNonDds: $nonDds," +
                    " targetPrimarySimAutoDataSwitch: $targetPrimarySimAutoDataSwitch"
            )
            if (nonDds != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                val telephonyManagerForNonDds: TelephonyManager? =
                    context.getSystemService(TelephonyManager::class.java)
                        ?.createForSubscriptionId(nonDds)
                setAutomaticData(telephonyManagerForNonDds, targetPrimarySimAutoDataSwitch)
            }

            // no next action, send finish
            callback(CallbackType.CALLBACK_FINISH)
        }
    }
}