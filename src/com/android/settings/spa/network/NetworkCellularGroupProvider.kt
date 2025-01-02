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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.telephony.DataSubscriptionRepository
import com.android.settings.network.telephony.MobileDataRepository
import com.android.settings.network.telephony.SimRepository
import com.android.settings.network.telephony.requireSubscriptionManager
import com.android.settings.spa.network.PrimarySimRepository.PrimarySimInfo
import com.android.settings.spa.search.SearchablePage
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
open class NetworkCellularGroupProvider : SettingsPageProvider, SearchablePage {
    override val name = fileName
    override val metricsCategory = SettingsEnums.MOBILE_NETWORK_LIST
    private val owner = createSettingsPage()

    var defaultVoiceSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var defaultSmsSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var defaultDataSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    var nonDds: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    open fun buildInjectEntry() = SettingsEntryBuilder.createInject(owner = owner)
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
        val mobileDataSelectedId = rememberSaveable { mutableStateOf<Int?>(null) }
        var nonDdsRemember = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        val subscriptionViewModel = viewModel<SubscriptionInfoListViewModel>()

        CollectAirplaneModeAndFinishIfOn()

        LaunchedEffect(Unit) {
            allOfFlows(context, subscriptionViewModel.selectableSubscriptionInfoListFlow).collect {
                callsSelectedId.intValue = defaultVoiceSubId
                textsSelectedId.intValue = defaultSmsSubId
                mobileDataSelectedId.value = defaultDataSubId
                nonDdsRemember.intValue = nonDds
            }
        }

        val selectableSubscriptionInfoList by subscriptionViewModel
                .selectableSubscriptionInfoListFlow
                .collectAsStateWithLifecycle(initialValue = emptyList())

        RegularScaffold(title = stringResource(R.string.provider_network_settings_title)) {
            SimsSection(selectableSubscriptionInfoList)
            val mobileDataSelectedIdValue = mobileDataSelectedId.value
            // Avoid draw mobile data UI before data ready to reduce flaky
            if (mobileDataSelectedIdValue != null) {
                val showMobileDataSection =
                    selectableSubscriptionInfoList.any { subInfo -> subInfo.simSlotIndex > -1 }
                if (showMobileDataSection) {
                    MobileDataSectionImpl(mobileDataSelectedIdValue, nonDdsRemember.intValue)
                }

                PrimarySimSectionImpl(
                    subscriptionViewModel.selectableSubscriptionInfoListFlow,
                    callsSelectedId,
                    textsSelectedId,
                    remember(mobileDataSelectedIdValue) {
                        mutableIntStateOf(mobileDataSelectedIdValue)
                    },
                )
            }

            OtherSection()
        }
    }

    private fun allOfFlows(context: Context,
                           selectableSubscriptionInfoListFlow: Flow<List<SubscriptionInfo>>) =
            combine(
                    selectableSubscriptionInfoListFlow,
                    context.defaultVoiceSubscriptionFlow(),
                    context.defaultSmsSubscriptionFlow(),
                    DataSubscriptionRepository(context).defaultDataSubscriptionIdFlow(),
                    this::refreshUiStates,
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
    @Composable
    open fun OtherSection(){
        // Do nothing
    }

    override fun getPageTitleForSearch(context: Context): String =
        context.getString(R.string.provider_network_settings_title)

    override fun getSearchableTitles(context: Context): List<String> {
        if (!isPageSearchable(context)) return emptyList()
        return buildList {
            if (context.requireSubscriptionManager().activeSubscriptionInfoCount > 0) {
                add(context.getString(R.string.mobile_data_settings_title))
            }
        }
    }

    companion object {
        const val fileName = "NetworkCellularGroupProvider"

        private fun isPageSearchable(context: Context) =
            Flags.isDualSimOnboardingEnabled() && SimRepository(context).canEnterMobileNetworkPage()
    }
}

@Composable
fun MobileDataSectionImpl(mobileDataSelectedId: Int, nonDds: Int) {
    val mobileDataRepository = rememberContext(::MobileDataRepository)

    Category(title = stringResource(id = R.string.mobile_data_settings_title)) {
        MobileDataSwitchPreference(subId = mobileDataSelectedId)

        val isAutoDataEnabled by remember(nonDds) {
            mobileDataRepository.isMobileDataPolicyEnabledFlow(
                subId = nonDds,
                policy = TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH
            )
        }.collectAsStateWithLifecycle(initialValue = null)
        if (SubscriptionManager.isValidSubscriptionId(nonDds)) {
            AutomaticDataSwitchingPreference(
                isAutoDataEnabled = { isAutoDataEnabled },
                setAutoDataEnabled = { newEnabled ->
                    mobileDataRepository.setAutoDataSwitch(nonDds, newEnabled)
                },
            )
        }
    }
}

@Composable
fun PrimarySimImpl(
    primarySimInfo: PrimarySimInfo,
    callsSelectedId: MutableIntState,
    textsSelectedId: MutableIntState,
    mobileDataSelectedId: MutableIntState,
    wifiPickerTrackerHelper: WifiPickerTrackerHelper? = null,
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
        coroutineScope.launch {
            setDefaultData(
                context,
                subscriptionManager,
                wifiPickerTrackerHelper,
                it
            )
        }
    },
) {
    CreatePrimarySimListPreference(
        stringResource(id = R.string.primary_sim_calls_title),
        primarySimInfo.callsList,
        callsSelectedId,
        ImageVector.vectorResource(R.drawable.ic_phone),
        actionSetCalls
    )
    CreatePrimarySimListPreference(
        stringResource(id = R.string.primary_sim_texts_title),
        primarySimInfo.smsList,
        textsSelectedId,
        Icons.AutoMirrored.Outlined.Message,
        actionSetTexts
    )
    CreatePrimarySimListPreference(
        stringResource(id = R.string.mobile_data_settings_title),
        primarySimInfo.dataList,
        mobileDataSelectedId,
        Icons.Outlined.DataUsage,
        actionSetMobileData
    )
}

@Composable
fun PrimarySimSectionImpl(
    subscriptionInfoListFlow: Flow<List<SubscriptionInfo>>,
    callsSelectedId: MutableIntState,
    textsSelectedId: MutableIntState,
    mobileDataSelectedId: MutableIntState,
) {
    val context = LocalContext.current
    val primarySimInfo = remember(subscriptionInfoListFlow) {
        subscriptionInfoListFlow
            .map { subscriptionInfoList ->
                subscriptionInfoList.filter { subInfo -> subInfo.simSlotIndex != -1 }
            }
            .map(PrimarySimRepository(context)::getPrimarySimInfo)
            .flowOn(Dispatchers.Default)
    }.collectAsStateWithLifecycle(initialValue = null).value ?: return

    Category(title = stringResource(id = R.string.primary_sim_title)) {
        PrimarySimImpl(
            primarySimInfo,
            callsSelectedId,
            textsSelectedId,
            mobileDataSelectedId,
            rememberWifiPickerTrackerHelper()
        )
    }
}

@Composable
fun CollectAirplaneModeAndFinishIfOn() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.settingsGlobalBooleanFlow(Settings.Global.AIRPLANE_MODE_ON).collect {
            isAirplaneModeOn ->
            if (isAirplaneModeOn) {
                context.getActivity()?.finish()
            }
        }
    }
}

@Composable
fun rememberWifiPickerTrackerHelper(): WifiPickerTrackerHelper {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember { WifiPickerTrackerHelper(LifecycleRegistry(lifecycleOwner), context, null) }
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
    setMobileData(
        context,
        subscriptionManager,
        wifiPickerTrackerHelper,
        subId,
        true
    )

suspend fun setMobileData(
    context: Context,
    subscriptionManager: SubscriptionManager?,
    wifiPickerTrackerHelper: WifiPickerTrackerHelper?,
    subId: Int,
    enabled: Boolean,
): Unit =
    withContext(Dispatchers.Default) {
        Log.d(NetworkCellularGroupProvider.fileName, "setMobileData[$subId]: $enabled")

        var targetSubId = subId
        val activeSubIdList = subscriptionManager?.activeSubscriptionIdList
        if (activeSubIdList?.size == 1) {
            targetSubId = activeSubIdList[0]
            Log.d(
                NetworkCellularGroupProvider.fileName,
                "There is only one sim in the device, correct dds as $targetSubId"
            )
        }

        if (enabled) {
            Log.d(NetworkCellularGroupProvider.fileName, "setDefaultData: [$targetSubId]")
            subscriptionManager?.setDefaultDataSubId(targetSubId)
        }
        MobileDataRepository(context)
            .setMobileDataEnabled(targetSubId, enabled, wifiPickerTrackerHelper)
    }