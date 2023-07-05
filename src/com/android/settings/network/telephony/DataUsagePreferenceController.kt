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
import com.android.settings.datausage.DataUsageUtils
import com.android.settings.datausage.lib.DataUsageLib
import com.android.settingslib.net.DataUsageController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preference controller for "Data usage"
 */
class DataUsagePreferenceController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {

    private lateinit var preference: Preference
    private var networkTemplate: NetworkTemplate? = null

    @VisibleForTesting
    var dataUsageControllerFactory: (Context) -> DataUsageController = { DataUsageController(it) }

    fun init(subId: Int) {
        mSubId = subId
    }

    override fun getAvailabilityStatus(subId: Int): Int = when {
        SubscriptionManager.isValidSubscriptionId(subId) &&
            DataUsageUtils.hasMobileData(mContext) -> AVAILABLE

        else -> AVAILABLE_UNSEARCHABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                update()
            }
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != preferenceKey || networkTemplate == null) return false
        val intent = Intent(Settings.ACTION_MOBILE_DATA_USAGE).apply {
            putExtra(Settings.EXTRA_NETWORK_TEMPLATE, networkTemplate)
            putExtra(Settings.EXTRA_SUB_ID, mSubId)
        }
        mContext.startActivity(intent)
        return true
    }

    private suspend fun update() {
        val summary = withContext(Dispatchers.Default) {
            networkTemplate = getNetworkTemplate()
            getDataUsageSummary()
        }
        if (summary == null) {
            preference.isEnabled = false
        } else {
            preference.isEnabled = true
            preference.summary = summary
        }
    }

    private fun getNetworkTemplate(): NetworkTemplate? = when {
        SubscriptionManager.isValidSubscriptionId(mSubId) -> {
            DataUsageLib.getMobileTemplate(mContext, mSubId)
        }

        else -> null
    }

    private fun getDataUsageSummary(): String? {
        val networkTemplate = networkTemplate ?: return null
        val controller = dataUsageControllerFactory(mContext).apply {
            setSubscriptionId(mSubId)
        }
        val usageInfo = controller.getDataUsageInfo(networkTemplate)
        if (usageInfo != null && usageInfo.usageLevel > 0) {
            return mContext.getString(
                R.string.data_usage_template,
                DataUsageUtils.formatDataUsage(mContext, usageInfo.usageLevel),
                usageInfo.period,
            )
        }

        return controller.getHistoricalUsageLevel(networkTemplate).takeIf { it > 0 }?.let {
            mContext.getString(
                R.string.data_used_template,
                DataUsageUtils.formatDataUsage(mContext, it),
            )
        }
    }
}
