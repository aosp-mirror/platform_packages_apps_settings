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

package com.android.settings.spa.network

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.settings.R
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Showing the sim onboarding which is the process flow of sim switching on.
 */
object NetworkCellularGroupProvider : SettingsPageProvider {
    override val name = "NetworkCellularGroupProvider"

    private val owner = createSettingsPage()

    var defaultVoiceSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var defaultSmsSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var defaultDataSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var nonDds: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    fun buildInjectEntry() = SettingsEntryBuilder.createInject(owner = owner)
            .setUiLayoutFn {
                // never using
                Preference(object : PreferenceModel {
                    override val title = name
                    override val onClick = navigator(name)
                })
            }

    @Composable
    override fun Page(arguments: Bundle?) {
        val context = LocalContext.current
        var callsSelectedId = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        var textsSelectedId = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        var mobileDataSelectedId = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        var nonDdsRemember = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }

        val subscriptionViewModel = viewModel<SubscriptionInfoListViewModel>()

        remember {
            allOfFlows(context, subscriptionViewModel.selectableSubscriptionInfoListFlow)
        }.collectLatestWithLifecycle(LocalLifecycleOwner.current) {
            callsSelectedId.intValue = defaultVoiceSubId
            textsSelectedId.intValue = defaultSmsSubId
            mobileDataSelectedId.intValue = defaultDataSubId
            nonDdsRemember.intValue = nonDds
        }

        PageImpl(
            subscriptionViewModel.selectableSubscriptionInfoListFlow,
            callsSelectedId,
            textsSelectedId,
            mobileDataSelectedId,
            nonDdsRemember
        )
    }

    private fun allOfFlows(context: Context,
                           selectableSubscriptionInfoListFlow: Flow<List<SubscriptionInfo>>) =
            combine(
                    selectableSubscriptionInfoListFlow,
                    context.defaultVoiceSubscriptionFlow(),
                    context.defaultSmsSubscriptionFlow(),
                    context.defaultDefaultDataSubscriptionFlow(),
                    NetworkCellularGroupProvider::refreshUiStates,
            ).flowOn(Dispatchers.Default)

    private fun refreshUiStates(
        selectableSubscriptionInfoList: List<SubscriptionInfo>,
        inputDefaultVoiceSubId: Int,
        inputDefaultSmsSubId: Int,
        inputDefaultDateSubId: Int
    ) {
        defaultVoiceSubId = inputDefaultVoiceSubId
        defaultSmsSubId = inputDefaultSmsSubId
        defaultDataSubId = inputDefaultDateSubId
        nonDds = if (defaultDataSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } else {
            selectableSubscriptionInfoList
                    .filter { info ->
                        (info.simSlotIndex != -1) && (info.subscriptionId != defaultDataSubId)
                    }
                    .map { it.subscriptionId }
                    .firstOrNull() ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }

        Log.d(name, "defaultDataSubId: $defaultDataSubId, nonDds: $nonDds")
    }
}

@Composable
fun PageImpl(
    selectableSubscriptionInfoListFlow: StateFlow<List<SubscriptionInfo>>,
    defaultVoiceSubId: MutableIntState,
    defaultSmsSubId: MutableIntState,
    defaultDataSubId: MutableIntState,
    nonDds: MutableIntState
) {
    val selectableSubscriptionInfoList by selectableSubscriptionInfoListFlow
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSubscriptionInfoList: List<SubscriptionInfo> =
        selectableSubscriptionInfoList.filter { subscriptionInfo ->
            subscriptionInfo.simSlotIndex != -1
        }

    val stringSims = stringResource(R.string.provider_network_settings_title)
    RegularScaffold(title = stringSims) {
        SimsSection(selectableSubscriptionInfoList)
        PrimarySimSectionImpl(
                activeSubscriptionInfoList,
                defaultVoiceSubId,
                defaultSmsSubId,
                defaultDataSubId,
                nonDds
        )
    }
}

@Composable
fun PrimarySimImpl(
    subscriptionInfoList: List<SubscriptionInfo>,
    callsSelectedId: MutableIntState,
    textsSelectedId: MutableIntState,
    mobileDataSelectedId: MutableIntState,
    nonDds: MutableIntState,
    subscriptionManager: SubscriptionManager? =
        LocalContext.current.getSystemService(SubscriptionManager::class.java),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
    actionSetCalls: (Int) -> Unit = {
        callsSelectedId.intValue = it
        coroutineScope.launch {
            setDefaultVoice(subscriptionManager, it)
        }
    },
    actionSetTexts: (Int) -> Unit = {
        textsSelectedId.intValue = it
        coroutineScope.launch {
            setDefaultSms(subscriptionManager, it)
        }
    },
    actionSetMobileData: (Int) -> Unit = {
        mobileDataSelectedId.intValue = it
        coroutineScope.launch {
            // TODO: to fix the WifiPickerTracker crash when create
            //       the wifiPickerTrackerHelper
            setDefaultData(
                context,
                subscriptionManager,
                null/*wifiPickerTrackerHelper*/,
                it
            )
        }
    },
    actionSetAutoDataSwitch: (Boolean) -> Unit = { newState ->
        coroutineScope.launch {
            val telephonyManagerForNonDds: TelephonyManager? =
                context.getSystemService(TelephonyManager::class.java)
                    ?.createForSubscriptionId(nonDds.intValue)
            Log.d(NetworkCellularGroupProvider.name, "NonDds:${nonDds.intValue} setAutomaticData")
            setAutomaticData(telephonyManagerForNonDds, newState)
        }
    },
) {
    var state = rememberSaveable { mutableStateOf(false) }
    var callsAndSmsList = remember {
        mutableListOf(ListPreferenceOption(id = -1, text = "Loading"))
    }
    var dataList = remember {
        mutableListOf(ListPreferenceOption(id = -1, text = "Loading"))
    }

    if (subscriptionInfoList.size >= 2) {
        state.value = true
        callsAndSmsList.clear()
        dataList.clear()
        for (info in subscriptionInfoList) {
            var item = ListPreferenceOption(
                    id = info.subscriptionId,
                    text = "${info.displayName}",
                    summary = "${info.number}"
            )
            callsAndSmsList.add(item)
            dataList.add(item)
        }
        callsAndSmsList.add(
            ListPreferenceOption(
                id = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                text = stringResource(id = R.string.sim_calls_ask_first_prefs_title)
            )
        )
    } else {
        // hide the primary sim
        state.value = false
        Log.d(NetworkCellularGroupProvider.name, "Hide primary sim")
    }

    if (state.value) {
        val telephonyManagerForNonDds: TelephonyManager? =
                context.getSystemService(TelephonyManager::class.java)
                        ?.createForSubscriptionId(nonDds.intValue)
        val automaticDataChecked = rememberSaveable() {
            mutableStateOf(false)
        }

        CreatePrimarySimListPreference(
            stringResource(id = R.string.primary_sim_calls_title),
            callsAndSmsList,
            callsSelectedId,
            ImageVector.vectorResource(R.drawable.ic_phone),
            actionSetCalls
        )
        CreatePrimarySimListPreference(
            stringResource(id = R.string.primary_sim_texts_title),
            callsAndSmsList,
            textsSelectedId,
            Icons.AutoMirrored.Outlined.Message,
            actionSetTexts
        )
        CreatePrimarySimListPreference(
            stringResource(id = R.string.mobile_data_settings_title),
            dataList,
            mobileDataSelectedId,
            Icons.Outlined.DataUsage,
            actionSetMobileData
        )

        val autoDataTitle = stringResource(id = R.string.primary_sim_automatic_data_title)
        val autoDataSummary = stringResource(id = R.string.primary_sim_automatic_data_msg)
        SwitchPreference(
            object : SwitchPreferenceModel {
                override val title = autoDataTitle
                override val summary = { autoDataSummary }
                override val checked = {
                    if (nonDds.intValue != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        coroutineScope.launch {
                            automaticDataChecked.value = getAutomaticData(telephonyManagerForNonDds)
                            Log.d(
                                NetworkCellularGroupProvider.name,
                                "NonDds:${nonDds.intValue}" +
                                    "getAutomaticData:${automaticDataChecked.value}"
                            )
                        }
                    }
                    automaticDataChecked.value
                }
                override val onCheckedChange: ((Boolean) -> Unit)? = {
                    automaticDataChecked.value = it
                    actionSetAutoDataSwitch(it)
                }
            }
        )
    }
}

@Composable
fun PrimarySimSectionImpl(
    subscriptionInfoList: List<SubscriptionInfo>,
    callsSelectedId: MutableIntState,
    textsSelectedId: MutableIntState,
    mobileDataSelectedId: MutableIntState,
    nonDds: MutableIntState,
) {
    Category(title = stringResource(id = R.string.primary_sim_title)) {
        PrimarySimImpl(
            subscriptionInfoList,
            callsSelectedId,
            textsSelectedId,
            mobileDataSelectedId,
            nonDds
        )
    }
}

private fun Context.defaultVoiceSubscriptionFlow(): Flow<Int> =
        merge(
                flowOf(null), // kick an initial value
                broadcastReceiverFlow(
                        IntentFilter(TelephonyManager.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED)
                ),
        ).map { SubscriptionManager.getDefaultVoiceSubscriptionId() }
                .conflate().flowOn(Dispatchers.Default)

private fun Context.defaultSmsSubscriptionFlow(): Flow<Int> =
        merge(
                flowOf(null), // kick an initial value
                broadcastReceiverFlow(
                        IntentFilter(SubscriptionManager.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED)
                ),
        ).map { SubscriptionManager.getDefaultSmsSubscriptionId() }
                .conflate().flowOn(Dispatchers.Default)

private fun Context.defaultDefaultDataSubscriptionFlow(): Flow<Int> =
        merge(
                flowOf(null), // kick an initial value
                broadcastReceiverFlow(
                        IntentFilter(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)
                ),
        ).map { SubscriptionManager.getDefaultDataSubscriptionId() }
                .conflate().flowOn(Dispatchers.Default)

suspend fun setDefaultVoice(
    subscriptionManager: SubscriptionManager?,
    subId: Int
): Unit =
    withContext(Dispatchers.Default) {
        subscriptionManager?.setDefaultVoiceSubscriptionId(subId)
    }

suspend fun setDefaultSms(
    subscriptionManager: SubscriptionManager?,
    subId: Int
): Unit =
    withContext(Dispatchers.Default) {
        subscriptionManager?.setDefaultSmsSubId(subId)
    }

suspend fun setDefaultData(
    context: Context,
    subscriptionManager: SubscriptionManager?,
    wifiPickerTrackerHelper: WifiPickerTrackerHelper?,
    subId: Int
): Unit =
    withContext(Dispatchers.Default) {
        subscriptionManager?.setDefaultDataSubId(subId)
        MobileNetworkUtils.setMobileDataEnabled(
            context,
            subId,
            true /* enabled */,
            true /* disableOtherSubscriptions */
        )
        if (wifiPickerTrackerHelper != null
            && !wifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(subId)
        ) {
            wifiPickerTrackerHelper.setCarrierNetworkEnabled(true)
        }
    }

suspend fun getAutomaticData(telephonyManagerForNonDds: TelephonyManager?): Boolean =
    withContext(Dispatchers.Default) {
        telephonyManagerForNonDds != null
            && telephonyManagerForNonDds.isMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
    }

suspend fun setAutomaticData(telephonyManager: TelephonyManager?, newState: Boolean): Unit =
    withContext(Dispatchers.Default) {
        Log.d(
            NetworkCellularGroupProvider.name,
            "setAutomaticData: MOBILE_DATA_POLICY_AUTO_DATA_SWITCH as $newState"
        )
        telephonyManager?.setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
            newState
        )
        //TODO: setup backup calling
    }