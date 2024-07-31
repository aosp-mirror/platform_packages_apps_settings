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
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.MmsMessagePreferenceController.Companion.MmsMessageSearchItem
import com.android.settings.spa.SpaSearchLanding.BundleValue
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingFragment
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey
import com.android.settings.spa.search.SpaSearchRepository.Companion.createSearchIndexableRaw
import com.android.settings.spa.search.SpaSearchRepository.Companion.searchIndexProviderOf
import com.android.settingslib.search.SearchIndexableData
import com.android.settingslib.search.SearchIndexableRaw
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBoolean

class MobileNetworkSettingsSearchIndex(
    private val searchItemsFactory: (context: Context) -> List<MobileNetworkSettingsSearchItem> =
        ::createSearchItems,
) {
    interface MobileNetworkSettingsSearchItem {
        val key: String

        val title: String

        fun isAvailable(subId: Int): Boolean
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
        subInfos
            .filter { searchItem.isAvailable(it.subscriptionId) }
            .map { subInfo -> searchIndexableRaw(context, searchItem, subInfo) }

    private fun searchIndexableRaw(
        context: Context,
        searchItem: MobileNetworkSettingsSearchItem,
        subInfo: SubscriptionInfo,
    ): SearchIndexableRaw {
        val key =
            SpaSearchLandingKey.newBuilder()
                .setFragment(
                    SpaSearchLandingFragment.newBuilder()
                        .setFragmentName(MobileNetworkSettings::class.java.name)
                        .setPreferenceKey(searchItem.key)
                        .putArguments(
                            Settings.EXTRA_SUB_ID,
                            BundleValue.newBuilder().setIntValue(subInfo.subscriptionId).build()))
                .build()
        val simsTitle = context.getString(R.string.provider_network_settings_title)
        return createSearchIndexableRaw(
            context = context,
            spaSearchLandingKey = key,
            itemTitle = searchItem.title,
            indexableClass = MobileNetworkSettings::class.java,
            pageTitle = "$simsTitle > ${subInfo.displayName}",
        )
    }

    companion object {
        /** suppress full page if user is not admin */
        @JvmStatic
        fun isMobileNetworkSettingsSearchable(context: Context): Boolean {
            val isAirplaneMode by context.settingsGlobalBoolean(Settings.Global.AIRPLANE_MODE_ON)
            return SubscriptionUtil.isSimHardwareVisible(context) &&
                !isAirplaneMode &&
                context.userManager.isAdminUser
        }

        fun createSearchItems(context: Context): List<MobileNetworkSettingsSearchItem> =
            listOf(
                MmsMessageSearchItem(context),
            )
    }
}
