/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.sim.receivers

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccCardInfo
import android.telephony.UiccSlotInfo
import android.util.Log
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.UiccSlotUtil
import com.android.settings.network.UiccSlotsException
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settings.sim.ChooseSimActivity
import com.android.settings.sim.DsdsDialogActivity
import com.android.settings.sim.SimActivationNotifier
import com.android.settings.sim.SimNotificationService
import com.android.settings.sim.SwitchToEsimConfirmDialogActivity
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.stream.Collectors
import kotlin.concurrent.Volatile

/** Perform actions after a slot change event is triggered.  */
class SimSlotChangeHandler private constructor() {
    private var mSubMgr: SubscriptionManager? = null
    private var mTelMgr: TelephonyManager? = null
    private var mContext: Context? = null

    fun onSlotsStatusChange(context: Context) {
        init(context)

        check(Looper.myLooper() != Looper.getMainLooper()) { "Cannot be called from main thread." }

        val removableSlotInfo = removableUiccSlotInfo
        if (removableSlotInfo == null) {
            Log.e(TAG, "Unable to find the removable slot. Do nothing.")
            return
        }
        Log.i(
            TAG,
            "The removableSlotInfo: $removableSlotInfo"
        )
        val lastRemovableSlotState = getLastRemovableSimSlotState(mContext!!)
        val currentRemovableSlotState = removableSlotInfo.cardStateInfo
        Log.d(
            TAG,
            ("lastRemovableSlotState: " + lastRemovableSlotState + ",currentRemovableSlotState: "
                    + currentRemovableSlotState)
        )
        val isRemovableSimInserted =
            lastRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_ABSENT
                    && currentRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_PRESENT
        val isRemovableSimRemoved =
            lastRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_PRESENT
                    && currentRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_ABSENT

        // Sets the current removable slot state.
        setRemovableSimSlotState(mContext!!, currentRemovableSlotState)

        if (mTelMgr!!.activeModemCount > 1) {
            if (!isRemovableSimInserted) {
                Log.d(TAG, "Removable Sim is not inserted in DSDS mode. Do nothing.")
                return
            }

            if (Flags.isDualSimOnboardingEnabled()) {
                // ForNewUi, when the user inserts the psim, showing the sim onboarding for the user
                // to setup the sim switching or the default data subscription in DSDS.
                // Will show dialog for below case.
                // 1. the psim slot is not active.
                // 2. there are one or more active sim.
                handleRemovableSimInsertWhenDsds(removableSlotInfo)
                return
            } else if (!isMultipleEnabledProfilesSupported) {
                Log.d(TAG, "The device is already in DSDS mode and no MEP. Do nothing.")
                return
            } else if (isMultipleEnabledProfilesSupported) {
                handleRemovableSimInsertUnderDsdsMep(removableSlotInfo)
                return
            }
        }

        if (isRemovableSimInserted) {
            handleSimInsert(removableSlotInfo)
            return
        }
        if (isRemovableSimRemoved) {
            handleSimRemove(removableSlotInfo)
            return
        }
        Log.i(TAG, "Do nothing on slot status changes.")
    }

    fun onSuwFinish(context: Context) {
        init(context)

        check(Looper.myLooper() != Looper.getMainLooper()) { "Cannot be called from main thread." }

        if (mTelMgr!!.activeModemCount > 1) {
            Log.i(TAG, "The device is already in DSDS mode. Do nothing.")
            return
        }

        val removableSlotInfo = removableUiccSlotInfo
        if (removableSlotInfo == null) {
            Log.e(TAG, "Unable to find the removable slot. Do nothing.")
            return
        }

        val embeddedSimExist = groupedEmbeddedSubscriptions.size != 0
        val removableSlotAction = getSuwRemovableSlotAction(mContext!!)
        setSuwRemovableSlotAction(mContext!!, LAST_USER_ACTION_IN_SUW_NONE)

        if (embeddedSimExist
            && removableSlotInfo.cardStateInfo == UiccSlotInfo.CARD_STATE_INFO_PRESENT
        ) {
            if (mTelMgr!!.isMultiSimSupported() == TelephonyManager.MULTISIM_ALLOWED) {
                Log.i(TAG, "DSDS condition satisfied. Show notification.")
                SimNotificationService.scheduleSimNotification(
                    mContext, SimActivationNotifier.NotificationType.ENABLE_DSDS
                )
            } else if (removableSlotAction == LAST_USER_ACTION_IN_SUW_INSERT) {
                Log.i(
                    TAG,
                    ("Both removable SIM and eSIM are present. DSDS condition doesn't"
                            + " satisfied. User inserted pSIM during SUW. Show choose SIM"
                            + " screen.")
                )
                startChooseSimActivity(true)
            }
        } else if (removableSlotAction == LAST_USER_ACTION_IN_SUW_REMOVE) {
            handleSimRemove(removableSlotInfo)
        }
    }

    private fun init(context: Context) {
        mSubMgr =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        mTelMgr = context.getSystemService(TelephonyManager::class.java)
        mContext = context
    }

    private fun handleSimInsert(removableSlotInfo: UiccSlotInfo) {
        Log.i(TAG, "Handle SIM inserted.")
        if (!isSuwFinished(mContext!!)) {
            Log.i(TAG, "Still in SUW. Handle SIM insertion after SUW is finished")
            setSuwRemovableSlotAction(mContext!!, LAST_USER_ACTION_IN_SUW_INSERT)
            return
        }
        if (removableSlotInfo.ports.stream().findFirst().get().isActive) {
            Log.i(TAG, "The removable slot is already active. Do nothing.")
            return
        }

        if (hasActiveEsimSubscription()) {
            if (mTelMgr!!.isMultiSimSupported() == TelephonyManager.MULTISIM_ALLOWED) {
                Log.i(TAG, "Enabled profile exists. DSDS condition satisfied.")
                if (Flags.isDualSimOnboardingEnabled()) {
                    // enable dsds by sim onboarding flow
                    handleRemovableSimInsertWhenDsds(removableSlotInfo)
                } else {
                    startDsdsDialogActivity()
                }
            } else {
                Log.i(TAG, "Enabled profile exists. DSDS condition not satisfied.")
                startChooseSimActivity(true)
            }
            return
        }

        Log.i(
            TAG,
            "No enabled eSIM profile. Ready to switch to removable slot and show"
                    + " notification."
        )
        try {
            UiccSlotUtil.switchToRemovableSlot(
                UiccSlotUtil.INVALID_PHYSICAL_SLOT_ID, mContext!!.applicationContext
            )
        } catch (e: UiccSlotsException) {
            Log.e(TAG, "Failed to switch to removable slot.")
            return
        }
        SimNotificationService.scheduleSimNotification(
            mContext, SimActivationNotifier.NotificationType.SWITCH_TO_REMOVABLE_SLOT
        )
    }

    private fun handleSimRemove(removableSlotInfo: UiccSlotInfo) {
        Log.i(TAG, "Handle SIM removed.")

        if (!isSuwFinished(mContext!!)) {
            Log.i(TAG, "Still in SUW. Handle SIM removal after SUW is finished")
            setSuwRemovableSlotAction(mContext!!, LAST_USER_ACTION_IN_SUW_REMOVE)
            return
        }

        val groupedEmbeddedSubscriptions =
            groupedEmbeddedSubscriptions
        if (groupedEmbeddedSubscriptions.isEmpty() || !removableSlotInfo.ports.stream()
                .findFirst().get().isActive
        ) {
            Log.i(
                TAG, ("eSIM slot is active or no subscriptions exist. Do nothing."
                        + " The removableSlotInfo: " + removableSlotInfo
                        + ", groupedEmbeddedSubscriptions: " + groupedEmbeddedSubscriptions)
            )
            return
        }

        // If there is only 1 eSIM profile exists, we ask the user if they want to switch to that
        // profile.
        if (groupedEmbeddedSubscriptions.size == 1) {
            Log.i(TAG, "Only 1 eSIM profile found. Ask user's consent to switch.")
            startSwitchSlotConfirmDialogActivity(groupedEmbeddedSubscriptions[0])
            return
        }

        // If there are more than 1 eSIM profiles installed, we show a screen to let users to choose
        // the number they want to use.
        Log.i(TAG, "Multiple eSIM profiles found. Ask user which subscription to use.")
        startChooseSimActivity(false)
    }

    private fun hasOtherActiveSubInfo(psimSubId: Int): Boolean {
        val activeSubs = SubscriptionUtil.getActiveSubscriptions(mSubMgr)
        return activeSubs.stream()
            .anyMatch { subscriptionInfo -> subscriptionInfo.subscriptionId != psimSubId }
    }

    private fun hasAnyPortActiveInSlot(removableSlotInfo: UiccSlotInfo): Boolean {
        return removableSlotInfo.ports.stream().anyMatch { slot -> slot.isActive }
    }

    private fun handleRemovableSimInsertWhenDsds(removableSlotInfo: UiccSlotInfo) {
        Log.i(TAG, "ForNewUi: Handle Removable SIM inserted")
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            withContext(Dispatchers.Default) {
                val subscriptionInfos =
                    withTimeoutOrNull(DEFAULT_WAIT_AFTER_SIM_INSERTED_TIMEOUT_MILLIS) {
                        SubscriptionRepository(mContext!!)
                            .removableSubscriptionInfoListFlow()
                            .firstOrNull { it.isNotEmpty() }
                    }

                if (subscriptionInfos.isNullOrEmpty()) {
                    Log.e(TAG, "Unable to find the removable subscriptionInfo. Do nothing.")
                    return@withContext
                }
                Log.d(
                    TAG,
                    "getAvailableRemovableSubscription:$subscriptionInfos"
                )
                val psimSubId = subscriptionInfos[0].subscriptionId
                if (!hasAnyPortActiveInSlot(removableSlotInfo)
                        || hasOtherActiveSubInfo(psimSubId)) {
                    Log.d(TAG, "ForNewUi Start Setup flow")
                    startSimConfirmDialogActivity(psimSubId)
                }
            }
        }
    }

    private fun handleRemovableSimInsertUnderDsdsMep(removableSlotInfo: UiccSlotInfo) {
        Log.i(TAG, "Handle Removable SIM inserted under DSDS+Mep.")

        if (removableSlotInfo.ports.stream().findFirst().get().isActive) {
            Log.i(
                TAG, "The removable slot is already active. Do nothing. removableSlotInfo: "
                        + removableSlotInfo
            )
            return
        }

        val subscriptionInfos =
            availableRemovableSubscription
        if (subscriptionInfos.isEmpty()) {
            Log.e(TAG, "Unable to find the removable subscriptionInfo. Do nothing.")
            return
        }
        Log.d(
            TAG,
            "getAvailableRemovableSubscription:$subscriptionInfos"
        )
        startSimConfirmDialogActivity(subscriptionInfos[0].subscriptionId)
    }

    private fun getLastRemovableSimSlotState(context: Context): Int {
        val prefs = context.getSharedPreferences(EUICC_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REMOVABLE_SLOT_STATE, UiccSlotInfo.CARD_STATE_INFO_ABSENT)
    }

    private fun setRemovableSimSlotState(context: Context, state: Int) {
        val prefs = context.getSharedPreferences(EUICC_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REMOVABLE_SLOT_STATE, state).apply()
        Log.d(TAG, "setRemovableSimSlotState: $state")
    }

    private fun getSuwRemovableSlotAction(context: Context): Int {
        val prefs = context.getSharedPreferences(EUICC_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SUW_PSIM_ACTION, LAST_USER_ACTION_IN_SUW_NONE)
    }

    private fun setSuwRemovableSlotAction(context: Context, action: Int) {
        val prefs = context.getSharedPreferences(EUICC_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SUW_PSIM_ACTION, action).apply()
    }

    private val removableUiccSlotInfo: UiccSlotInfo?
        get() {
            val slotInfos = mTelMgr!!.uiccSlotsInfo
            if (slotInfos == null) {
                Log.e(
                    TAG,
                    "slotInfos is null. Unable to get slot infos."
                )
                return null
            }
            for (slotInfo in slotInfos) {
                if (slotInfo != null && slotInfo.isRemovable) {
                    return slotInfo
                }
            }
            return null
        }

    private fun hasActiveEsimSubscription(): Boolean {
        val activeSubs = SubscriptionUtil.getActiveSubscriptions(mSubMgr)
        return activeSubs.stream().anyMatch { subscriptionInfo -> subscriptionInfo.isEmbedded }
    }

    private val groupedEmbeddedSubscriptions: List<SubscriptionInfo>
        get() {
            val groupedSubscriptions =
                SubscriptionUtil.getSelectableSubscriptionInfoList(mContext)
                    ?: return ImmutableList.of()
            return ImmutableList.copyOf(
                groupedSubscriptions.stream()
                    .filter { sub: SubscriptionInfo -> sub.isEmbedded }
                    .collect(Collectors.toList()))
        }

    protected val availableRemovableSubscription: List<SubscriptionInfo>
        get() {
            val removableSubscriptions =
                SubscriptionUtil.getAvailableSubscriptions(mContext)
            return ImmutableList.copyOf(
                removableSubscriptions.stream()
                    // ToDo: This condition is for psim only. If device supports removable
                    //  esim, it needs an new condition.
                    .filter { sub: SubscriptionInfo -> !sub.isEmbedded }
                    .collect(Collectors.toList()))
        }

    private fun startChooseSimActivity(psimInserted: Boolean) {
        val intent = ChooseSimActivity.getIntent(mContext)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(ChooseSimActivity.KEY_HAS_PSIM, psimInserted)
        mContext!!.startActivityAsUser(intent, UserHandle.SYSTEM)
    }

    private fun startSwitchSlotConfirmDialogActivity(subscriptionInfo: SubscriptionInfo) {
        val intent = Intent(
            mContext,
            SwitchToEsimConfirmDialogActivity::class.java
        )
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(SwitchToEsimConfirmDialogActivity.KEY_SUB_TO_ENABLE, subscriptionInfo)
        mContext!!.startActivityAsUser(intent, UserHandle.SYSTEM)
    }

    private fun startDsdsDialogActivity() {
        val intent = Intent(mContext, DsdsDialogActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext!!.startActivityAsUser(intent, UserHandle.SYSTEM)
    }

    private fun startSimConfirmDialogActivity(subId: Int) {
        if (!isSuwFinished(mContext!!)) {
            Log.d(TAG, "Still in SUW. Do nothing")
            return
        }
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.i(TAG, "Unable to enable subscription due to invalid subscription ID.")
            return
        }
        Log.d(
            TAG,
            "Start ToggleSubscriptionDialogActivity with $subId under DSDS+Mep."
        )
        SubscriptionUtil.startToggleSubscriptionDialogActivity(mContext, subId, true, true)
    }

    private val isMultipleEnabledProfilesSupported: Boolean
        get() {
            val cardInfos = mTelMgr!!.uiccCardsInfo
            if (cardInfos == null) {
                Log.d(
                    TAG,
                    "UICC cards info list is empty."
                )
                return false
            }
            return cardInfos.stream()
                .anyMatch { cardInfo: UiccCardInfo -> cardInfo.isMultipleEnabledProfilesSupported }
        }

    private fun isSuwFinished(context: Context): Boolean {
        try {
            // DEVICE_PROVISIONED is 0 if still in setup wizard. 1 if setup completed.
            return (Settings.Global.getInt(
                    context.contentResolver, Settings.Global.DEVICE_PROVISIONED)
                    == 1)
        } catch (e: SettingNotFoundException) {
            Log.e(TAG, "Cannot get DEVICE_PROVISIONED from the device.", e)
            return false
        }
    }

    companion object {
        private const val TAG = "SimSlotChangeHandler"

        private const val EUICC_PREFS = "euicc_prefs"

        // Shared preference keys
        private const val KEY_REMOVABLE_SLOT_STATE = "removable_slot_state"
        private const val KEY_SUW_PSIM_ACTION = "suw_psim_action"

        // User's last removable SIM insertion / removal action during SUW.
        private const val LAST_USER_ACTION_IN_SUW_NONE = 0
        private const val LAST_USER_ACTION_IN_SUW_INSERT = 1
        private const val LAST_USER_ACTION_IN_SUW_REMOVE = 2

        private const val DEFAULT_WAIT_AFTER_SIM_INSERTED_TIMEOUT_MILLIS: Long = 25 * 1000L

        @Volatile
        private var slotChangeHandler: SimSlotChangeHandler? = null

        /** Returns a SIM slot change handler singleton.  */
        @JvmStatic
        fun get(): SimSlotChangeHandler? {
            if (slotChangeHandler == null) {
                synchronized(SimSlotChangeHandler::class.java) {
                    if (slotChangeHandler == null) {
                        slotChangeHandler = SimSlotChangeHandler()
                    }
                }
            }
            return slotChangeHandler
        }
    }
}
