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

package com.android.settings.network.telephony

import android.content.Context
import android.content.Intent
import android.net.NetworkTemplate
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.datausage.DataUsageUtils
import com.android.settings.datausage.lib.DataUsageFormatter.FormattedDataUsage
import com.android.settings.datausage.lib.DataUsageLib
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.AllTimeRange
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settingslib.spaprivileged.framework.compose.getPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preference controller for "Data usage" */
class DataUsagePreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private lateinit var preference: Preference
    private var networkTemplate: NetworkTemplate? = null

    fun init(subId: Int) {
        this.subId = subId
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        preference.summary = mContext.getPlaceholder()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                update()
            }
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != preferenceKey || networkTemplate == null) return false
        val intent =
            Intent(Settings.ACTION_MOBILE_DATA_USAGE).apply {
                setPackage(mContext.packageName)
                putExtra(Settings.EXTRA_NETWORK_TEMPLATE, networkTemplate)
                putExtra(Settings.EXTRA_SUB_ID, subId)
            }
        mContext.startActivity(intent)
        return true
    }

    private suspend fun update() {
        val (summary, enabled) = withContext(Dispatchers.Default) {
            networkTemplate = getNetworkTemplate()
            getDataUsageSummaryAndEnabled()
        }
        preference.isEnabled = enabled
        preference.summary = summary?.displayText
    }

    private fun getNetworkTemplate(): NetworkTemplate? =
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            DataUsageLib.getMobileTemplate(mContext, subId)
        } else null

    @VisibleForTesting
    fun createNetworkCycleDataRepository(): NetworkCycleDataRepository? =
        networkTemplate?.let { NetworkCycleDataRepository(mContext, it) }

    private fun getDataUsageSummaryAndEnabled(): Pair<FormattedDataUsage?, Boolean> {
        val repository = createNetworkCycleDataRepository() ?: return null to false

        repository.loadFirstCycle()?.let { usageData ->
            val formattedDataUsage = usageData.formatUsage(mContext)
                .format(mContext, R.string.data_usage_template, usageData.formatDateRange(mContext))
            val hasUsage = usageData.usage > 0 || repository.queryUsage(AllTimeRange).usage > 0
            return formattedDataUsage to hasUsage
        }

        val allTimeUsage = repository.queryUsage(AllTimeRange)
        return allTimeUsage.getDataUsedString(mContext) to (allTimeUsage.usage > 0)
    }

    companion object {
        class DataUsageSearchItem(private val context: Context) : MobileNetworkSettingsSearchItem {
            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!DataUsageUtils.hasMobileData(context)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "data_usage_summary",
                    title = context.getString(R.string.app_cellular_data_usage),
                )
            }
        }
    }
}
