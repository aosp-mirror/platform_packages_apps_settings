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
import android.os.UserManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.AirplaneModePreference.Companion.isAirplaneModeOn
import com.android.settings.network.SubscriptionUtil.getUniqueSubscriptionDisplayName
import com.android.settings.network.telephony.SimRepository
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settings.network.telephony.euicc.EuiccRepository
import com.android.settings.spa.network.getAddSimIntent
import com.android.settings.spa.network.startAddSimFlow
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenBinding
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(MobileNetworkListScreen.KEY)
class MobileNetworkListScreen :
    PreferenceScreenCreator,
    PreferenceScreenBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    PreferenceRestrictionMixin,
    OnPreferenceClickListener {

    private var airplaneModeObserver: KeyedObserver<String>? = null
    private var subscriptionInfoList: List<SubscriptionInfo>? = null
    private var onSubscriptionsChangedListener: OnSubscriptionsChangedListener? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.provider_network_settings_title

    override val icon: Int
        get() = R.drawable.ic_sim_card

    override val keywords: Int
        get() = R.string.keywords_more_mobile_networks

    override fun intent(context: Context) = getAddSimIntent()

    override fun getSummary(context: Context): CharSequence? {
        val list = getSelectableSubscriptionInfoList(context)
        return when {
            list.isNotEmpty() ->
                list
                    .map { getUniqueSubscriptionDisplayName(it, context).toString() }
                    .distinct()
                    .joinToString(", ")
            EuiccRepository(context).showEuiccSettings() ->
                context.getString(R.string.mobile_network_summary_add_a_network)
            else -> null
        }
    }

    override fun isAvailable(context: Context) =
        SimRepository(context).showMobileNetworkPageEntrance()

    override fun isEnabled(context: Context) =
        super<PreferenceRestrictionMixin>.isEnabled(context) &&
            !context.isAirplaneModeOn() &&
            (getSelectableSubscriptionInfoList(context).isNotEmpty() ||
                EuiccRepository(context).showEuiccSettings())

    private fun getSelectableSubscriptionInfoList(context: Context): List<SubscriptionInfo> =
        subscriptionInfoList
            ?: SubscriptionRepository(context).getSelectableSubscriptionInfoList().also {
                subscriptionInfoList = it
            }

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)

    override val useAdminDisabledSummary
        get() = true

    override fun createWidget(context: Context) = RestrictedPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val summary = preference.summary ?: return true // no-op
        val context = preference.context
        if (summary == context.getString(R.string.mobile_network_summary_add_a_network)) {
            startAddSimFlow(context) // start intent
            return true
        }
        return false // start fragment
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        val executor = HandlerExecutor.main
        val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(KEY) }
        airplaneModeObserver = observer
        SettingsGlobalStore.get(context).addObserver(AirplaneModePreference.KEY, observer, executor)
        context.getSystemService(SubscriptionManager::class.java)?.let {
            val listener =
                object : OnSubscriptionsChangedListener() {
                    override fun onSubscriptionsChanged() {
                        subscriptionInfoList = null // invalid cache
                        context.notifyPreferenceChange(KEY)
                    }
                }
            it.addOnSubscriptionsChangedListener(executor, listener)
            onSubscriptionsChangedListener = listener
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        airplaneModeObserver?.let {
            SettingsGlobalStore.get(context).removeObserver(AirplaneModePreference.KEY, it)
        }
        context.getSystemService(SubscriptionManager::class.java)?.apply {
            onSubscriptionsChangedListener?.let { removeOnSubscriptionsChangedListener(it) }
        }
    }

    override fun isFlagEnabled(context: Context) = Flags.catalystMobileNetworkList()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = MobileNetworkListFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) { +MobileDataPreference() }

    companion object {
        const val KEY = "mobile_network_list"
    }
}
