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
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle

/**
 * Preference controller for "Mobile network" and showing the SPN.
 */
class MobileNetworkSpnPreferenceController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {

    private lateinit var lazyViewModel: Lazy<SubscriptionInfoListViewModel>
    private lateinit var preference: Preference

    private var spn = String()

    fun init(fragment: Fragment, subId: Int) {
        lazyViewModel = fragment.viewModels()
        mSubId = subId
    }

    override fun getAvailabilityStatus(subId: Int): Int = when {
        !Flags.isDualSimOnboardingEnabled() -> CONDITIONALLY_UNAVAILABLE
        SubscriptionManager.isValidSubscriptionId(subId)-> AVAILABLE
        else -> CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        val viewModel by lazyViewModel

        viewModel.subscriptionInfoListFlow
                .collectLatestWithLifecycle(viewLifecycleOwner) { subscriptionInfoList ->
                    refreshData(subscriptionInfoList)
                }
    }

    @VisibleForTesting
    fun refreshData(subscriptionInfoList: List<SubscriptionInfo>){
        spn = subscriptionInfoList
            .firstOrNull { subInfo -> subInfo.subscriptionId == mSubId }
            ?.let { info -> info.carrierName.toString() }
            ?: String()

        refreshUi()
    }

    private fun refreshUi(){
        preference.summary = spn
    }
}
