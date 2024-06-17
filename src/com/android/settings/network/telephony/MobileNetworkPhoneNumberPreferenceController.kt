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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preference controller for "Phone number"
 */
class MobileNetworkPhoneNumberPreferenceController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {

    private lateinit var lazyViewModel: Lazy<SubscriptionInfoListViewModel>
    private lateinit var preference: Preference

    private var phoneNumber = String()

    fun init(fragment: Fragment, subId: Int) {
        lazyViewModel = fragment.viewModels()
        mSubId = subId
    }

    override fun getAvailabilityStatus(subId: Int): Int = when {
        !Flags.isDualSimOnboardingEnabled() -> CONDITIONALLY_UNAVAILABLE
        SubscriptionManager.isValidSubscriptionId(subId)
                && SubscriptionUtil.isSimHardwareVisible(mContext) -> AVAILABLE
        else -> CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        val viewModel by lazyViewModel
        val coroutineScope = viewLifecycleOwner.lifecycleScope

        viewModel.subscriptionInfoListFlow
            .map { subscriptionInfoList ->
                subscriptionInfoList
                    .firstOrNull { subInfo -> subInfo.subscriptionId == mSubId }
            }
            .flowOn(Dispatchers.Default)
            .collectLatestWithLifecycle(viewLifecycleOwner) {
                it?.let {
                    coroutineScope.launch {
                        refreshData(it)
                    }
                }
            }
    }

    @VisibleForTesting
    suspend fun refreshData(subscriptionInfo: SubscriptionInfo){
        withContext(Dispatchers.Default) {
            phoneNumber = getFormattedPhoneNumber(subscriptionInfo)
        }
        refreshUi()
    }

    private fun refreshUi(){
        preference.summary = phoneNumber
    }

    private fun getFormattedPhoneNumber(subscriptionInfo: SubscriptionInfo?): String {
        val phoneNumber = SubscriptionUtil.getBidiFormattedPhoneNumber(
            mContext,
            subscriptionInfo
        )
        return phoneNumber
            ?.let { return it.ifEmpty { getStringUnknown() } }
            ?: getStringUnknown()
    }

    private fun getStringUnknown(): String {
        return mContext.getString(R.string.device_info_default)
    }
}
