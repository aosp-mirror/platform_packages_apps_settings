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

package com.android.settings.network

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.network.telephony.SimRepository
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.spa.network.startAddSimFlow
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow
import kotlinx.coroutines.flow.Flow

/**
 * This controls the summary text and click behavior of the "Mobile network" item on the Network &
 * internet page. There are 2 separate cases depending on the number of mobile network
 * subscriptions:
 * - No subscription: click action begins a UI flow to add a network subscription, and the summary
 *   text indicates this
 * - Has subscriptions: click action takes you to a page listing the subscriptions, and the summary
 *   text gives the count of SIMs
 */
class MobileNetworkSummaryController
@JvmOverloads
constructor(
    private val context: Context,
    preferenceKey: String,
    private val repository: MobileNetworkSummaryRepository =
        MobileNetworkSummaryRepository(context),
    private val airplaneModeOnFlow: Flow<Boolean> =
        context.settingsGlobalBooleanFlow(Settings.Global.AIRPLANE_MODE_ON),
) : BasePreferenceController(context, preferenceKey) {
    private val metricsFeatureProvider = featureFactory.metricsFeatureProvider
    private var preference: RestrictedPreference? = null

    private var isAirplaneModeOn = false

    override fun getAvailabilityStatus() =
        if (SimRepository(mContext).showMobileNetworkPageEntrance()) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        repository
            .subscriptionsStateFlow()
            .collectLatestWithLifecycle(viewLifecycleOwner, action = ::update)
        airplaneModeOnFlow.collectLatestWithLifecycle(viewLifecycleOwner) {
            isAirplaneModeOn = it
            updateEnabled()
        }
    }

    private fun update(state: MobileNetworkSummaryRepository.SubscriptionsState) {
        val preference = preference ?: return
        preference.onPreferenceClickListener = null
        preference.fragment = null
        when (state) {
            MobileNetworkSummaryRepository.AddNetwork -> {
                preference.summary =
                    context.getString(R.string.mobile_network_summary_add_a_network)
                preference.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        logPreferenceClick()
                        startAddSimFlow(context)
                        true
                    }
            }

            MobileNetworkSummaryRepository.NoSubscriptions -> {
                preference.summary = null
            }

            is MobileNetworkSummaryRepository.HasSubscriptions -> {
                preference.summary = state.displayNames.joinToString(", ")
                preference.fragment = MobileNetworkListFragment::class.java.canonicalName
            }
        }
        updateEnabled()
    }

    private fun updateEnabled() {
        val preference = preference ?: return
        if (preference.isDisabledByAdmin) return
        preference.isEnabled =
            (preference.onPreferenceClickListener != null || preference.fragment != null) &&
                !isAirplaneModeOn
    }

    private fun logPreferenceClick() {
        val preference = preference ?: return
        metricsFeatureProvider.logClickedPreference(
            preference,
            preference.extras.getInt(DashboardFragment.CATEGORY),
        )
    }
}
