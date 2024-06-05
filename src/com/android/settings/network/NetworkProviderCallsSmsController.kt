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
import android.content.IntentFilter
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.Utils
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * The summary text and click behavior of the "Calls & SMS" item on the Network & internet page.
 */
open class NetworkProviderCallsSmsController @JvmOverloads constructor(
    context: Context,
    preferenceKey: String,
    private val getDisplayName: (SubscriptionInfo) -> CharSequence = { subInfo ->
        SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, context)
    },
    private val isInService: (Int) -> Boolean = IsInServiceImpl(context)::isInService,
) : BasePreferenceController(context, preferenceKey) {

    private lateinit var lazyViewModel: Lazy<SubscriptionInfoListViewModel>
    private lateinit var preference: RestrictedPreference

    fun init(fragment: Fragment) {
        lazyViewModel = fragment.viewModels()
    }

    override fun getAvailabilityStatus() = when {
        Flags.isDualSimOnboardingEnabled() -> UNSUPPORTED_ON_DEVICE
        !SubscriptionUtil.isSimHardwareVisible(mContext) -> UNSUPPORTED_ON_DEVICE
        !mContext.userManager.isAdminUser -> DISABLED_FOR_USER
        else -> AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        val viewModel by lazyViewModel

        summaryFlow(viewModel.subscriptionInfoListFlow)
            .collectLatestWithLifecycle(viewLifecycleOwner) { preference.summary = it }

        viewModel.subscriptionInfoListFlow
            .collectLatestWithLifecycle(viewLifecycleOwner) { subscriptionInfoList ->
                if (!preference.isDisabledByAdmin) {
                    preference.isEnabled = subscriptionInfoList.isNotEmpty()
                }
            }
    }

    private fun summaryFlow(subscriptionInfoListFlow: Flow<List<SubscriptionInfo>>) = combine(
        subscriptionInfoListFlow,
        mContext.defaultVoiceSubscriptionFlow(),
        mContext.defaultSmsSubscriptionFlow(),
        ::getSummary,
    ).flowOn(Dispatchers.Default)

    @VisibleForTesting
    fun getSummary(
        activeSubscriptionInfoList: List<SubscriptionInfo>,
        defaultVoiceSubscriptionId: Int,
        defaultSmsSubscriptionId: Int,
    ): String {
        if (activeSubscriptionInfoList.isEmpty()) {
            return mContext.getString(R.string.calls_sms_no_sim)
        }

        activeSubscriptionInfoList.singleOrNull()?.let {
            // Set displayName as summary if there is only one valid SIM.
            if (isInService(it.subscriptionId)) return it.displayName.toString()
        }

        return activeSubscriptionInfoList.joinToString { subInfo ->
            val displayName = getDisplayName(subInfo)

            val subId = subInfo.subscriptionId
            val statusResId = getPreferredStatus(
                subId = subId,
                subsSize = activeSubscriptionInfoList.size,
                isCallPreferred = subId == defaultVoiceSubscriptionId,
                isSmsPreferred = subId == defaultSmsSubscriptionId,
            )
            if (statusResId == null) {
                // If there are 2 or more SIMs and one of these has no preferred status,
                // set only its displayName as summary.
                displayName
            } else {
                "$displayName (${mContext.getString(statusResId)})"
            }
        }
    }

    private fun getPreferredStatus(
        subId: Int,
        subsSize: Int,
        isCallPreferred: Boolean,
        isSmsPreferred: Boolean,
    ): Int? = when {
        !isInService(subId) -> {
            if (subsSize > 1) {
                R.string.calls_sms_unavailable
            } else {
                R.string.calls_sms_temp_unavailable
            }
        }

        isCallPreferred && isSmsPreferred -> R.string.calls_sms_preferred
        isCallPreferred -> R.string.calls_sms_calls_preferred
        isSmsPreferred -> R.string.calls_sms_sms_preferred
        else -> null
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

private class IsInServiceImpl(context: Context) {
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)!!

    fun isInService(subId: Int): Boolean {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return false

        val serviceState = telephonyManager.createForSubscriptionId(subId).serviceState
        return Utils.isInService(serviceState)
    }
}
