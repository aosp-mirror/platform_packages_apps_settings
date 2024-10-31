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
import android.provider.Settings
import android.telephony.SubscriptionInfo
import com.android.settings.R
import com.android.settings.datausage.BillingCyclePreferenceController.Companion.BillingCycleSearchItem
import com.android.settings.network.telephony.CarrierSettingsVersionPreferenceController.Companion.CarrierSettingsVersionSearchItem
import com.android.settings.network.telephony.DataUsagePreferenceController.Companion.DataUsageSearchItem
import com.android.settings.network.telephony.MmsMessagePreferenceController.Companion.MmsMessageSearchItem
import com.android.settings.network.telephony.NrAdvancedCallingPreferenceController.Companion.NrAdvancedCallingSearchItem
import com.android.settings.network.telephony.RoamingPreferenceController.Companion.RoamingSearchItem
import com.android.settings.network.telephony.VideoCallingPreferenceController.Companion.VideoCallingSearchItem
import com.android.settings.network.telephony.WifiCallingPreferenceController.Companion.WifiCallingSearchItem
import com.android.settings.spa.SpaSearchLanding.BundleValue
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingFragment
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey
import com.android.settings.spa.search.SpaSearchRepository.Companion.createSearchIndexableRaw
import com.android.settings.spa.search.SpaSearchRepository.Companion.searchIndexProviderOf
import com.android.settingslib.search.SearchIndexableData
import com.android.settingslib.search.SearchIndexableRaw
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBoolean

class MobileNetworkSettingsSearchIndex(
    private val searchItemsFactory: (context: Context) -> List<MobileNetworkSettingsSearchItem> =
        ::createSearchItems,
) {
    data class MobileNetworkSettingsSearchResult(
        val key: String,
        val title: String,
        val keywords: String? = null,
    )

    interface MobileNetworkSettingsSearchItem {
        fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult?
    }

    fun createSearchIndexableData(): SearchIndexableData {
        val searchIndexProvider = searchIndexProviderOf { context ->
            if (!isMobileNetworkSettingsSearchable(context)) {
                return@searchIndexProviderOf emptyList()
            }
            val subInfos = context.requireSubscriptionManager().activeSubscriptionInfoList
            if (subInfos.isNullOrEmpty()) {
                return@searchIndexProviderOf emptyList()
            }
            searchItemsFactory(context).flatMap { searchItem ->
                searchIndexableRawList(context, searchItem, subInfos)
            }
        }
        return SearchIndexableData(MobileNetworkSettings::class.java, searchIndexProvider)
    }

    private fun searchIndexableRawList(
        context: Context,
        searchItem: MobileNetworkSettingsSearchItem,
        subInfos: List<SubscriptionInfo>
    ): List<SearchIndexableRaw> =
        subInfos.mapNotNull { subInfo ->
            searchItem.getSearchResult(subInfo.subscriptionId)?.let { searchResult ->
                searchIndexableRaw(context, searchResult, subInfo)
            }
        }

    private fun searchIndexableRaw(
        context: Context,
        searchResult: MobileNetworkSettingsSearchResult,
        subInfo: SubscriptionInfo,
    ): SearchIndexableRaw {
        val key =
            SpaSearchLandingKey.newBuilder()
                .setFragment(
                    SpaSearchLandingFragment.newBuilder()
                        .setFragmentName(MobileNetworkSettings::class.java.name)
                        .setPreferenceKey(searchResult.key)
                        .putArguments(
                            Settings.EXTRA_SUB_ID,
                            BundleValue.newBuilder().setIntValue(subInfo.subscriptionId).build()))
                .build()
        val simsTitle = context.getString(R.string.provider_network_settings_title)
        return createSearchIndexableRaw(
            context = context,
            spaSearchLandingKey = key,
            itemTitle = searchResult.title,
            keywords = searchResult.keywords,
            indexableClass = MobileNetworkSettings::class.java,
            pageTitle = "$simsTitle > ${subInfo.displayName}",
        )
    }

    companion object {
        /** suppress full page if user is not admin */
        @JvmStatic
        fun isMobileNetworkSettingsSearchable(context: Context): Boolean =
            SimRepository(context).canEnterMobileNetworkPage()

        fun createSearchItems(context: Context): List<MobileNetworkSettingsSearchItem> =
            listOf(
                BillingCycleSearchItem(context),
                CarrierSettingsVersionSearchItem(context),
                DataUsageSearchItem(context),
                MmsMessageSearchItem(context),
                NrAdvancedCallingSearchItem(context),
                PreferredNetworkModeSearchItem(context),
                RoamingSearchItem(context),
                VideoCallingSearchItem(context),
                WifiCallingSearchItem(context),
            )
    }
}
