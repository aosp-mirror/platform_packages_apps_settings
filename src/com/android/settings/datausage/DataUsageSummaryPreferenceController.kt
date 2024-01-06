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

package com.android.settings.datausage

import android.content.Context
import android.net.NetworkPolicy
import android.net.NetworkTemplate
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.datausage.lib.DataUsageLib.getMobileTemplate
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.network.ProxySubscriptionManager
import com.android.settings.network.telephony.TelephonyBasePreferenceController
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This is the controller for a data usage header that retrieves carrier data from the new
 * subscriptions framework API if available. The controller reads subscription information from the
 * framework and falls back to legacy usage data if none are available.
 */
open class DataUsageSummaryPreferenceController @JvmOverloads constructor(
    context: Context,
    subId: Int,
    private val proxySubscriptionManager: ProxySubscriptionManager =
        ProxySubscriptionManager.getInstance(context),
    private val networkCycleDataRepositoryFactory: (
        template: NetworkTemplate,
    ) -> INetworkCycleDataRepository = { NetworkCycleDataRepository(context, it) },
    private val dataPlanRepositoryFactory: (
        networkCycleDataRepository: INetworkCycleDataRepository,
    ) -> DataPlanRepository = { DataPlanRepositoryImpl(it) }
) : TelephonyBasePreferenceController(context, KEY) {

    init {
        mSubId = subId
    }

    private val subInfo by lazy {
        if (DataUsageUtils.hasMobileData(mContext)) {
            proxySubscriptionManager.getAccessibleSubscriptionInfo(mSubId)
        } else null
    }
    private val networkCycleDataRepository by lazy {
        networkCycleDataRepositoryFactory(getMobileTemplate(mContext, mSubId))
    }
    private val policy by lazy { networkCycleDataRepository.getPolicy() }
    private lateinit var preference: DataUsageSummaryPreference

    override fun getAvailabilityStatus(subId: Int) =
        if (subInfo != null && policy != null) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
        policy?.let {
            preference.setLimitInfo(it.getLimitInfo())
            val dataBarSize = max(it.limitBytes, it.warningBytes)
            if (dataBarSize > NetworkPolicy.WARNING_DISABLED) {
                setDataBarSize(dataBarSize)
            }
        }
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                update()
            }
        }
    }

    private suspend fun update() {
        val policy = policy ?: return
        val dataPlanInfo = withContext(Dispatchers.Default) {
            dataPlanRepositoryFactory(networkCycleDataRepository).getDataPlanInfo(
                policy = policy,
                plans = proxySubscriptionManager.get().getSubscriptionPlans(mSubId),
            )
        }
        Log.d(TAG, "dataPlanInfo: $dataPlanInfo")
        preference.setUsageNumbers(dataPlanInfo.dataPlanUse, dataPlanInfo.dataPlanSize)
        if (dataPlanInfo.dataBarSize > 0) {
            preference.setChartEnabled(true)
            setDataBarSize(dataPlanInfo.dataBarSize)
            preference.setProgress(dataPlanInfo.dataPlanUse / dataPlanInfo.dataBarSize.toFloat())
        } else {
            preference.setChartEnabled(false)
        }

        preference.setUsageInfo(
            dataPlanInfo.cycleEnd,
            dataPlanInfo.snapshotTime,
            subInfo?.carrierName,
            dataPlanInfo.dataPlanCount,
        )
    }

    private fun setDataBarSize(dataBarSize: Long) {
        preference.setLabels(
            DataUsageUtils.formatDataUsage(mContext, /* byteValue = */ 0),
            DataUsageUtils.formatDataUsage(mContext, dataBarSize)
        )
    }

    private fun NetworkPolicy.getLimitInfo(): CharSequence? = when {
        warningBytes > 0 && limitBytes > 0 -> {
            TextUtils.expandTemplate(
                mContext.getText(R.string.cell_data_warning_and_limit),
                DataUsageUtils.formatDataUsage(mContext, warningBytes),
                DataUsageUtils.formatDataUsage(mContext, limitBytes),
            )
        }

        warningBytes > 0 -> {
            TextUtils.expandTemplate(
                mContext.getText(R.string.cell_data_warning),
                DataUsageUtils.formatDataUsage(mContext, warningBytes),
            )
        }

        limitBytes > 0 -> {
            TextUtils.expandTemplate(
                mContext.getText(R.string.cell_data_limit),
                DataUsageUtils.formatDataUsage(mContext, limitBytes),
            )
        }

        else -> null
    }

    companion object {
        private const val TAG = "DataUsageSummaryPC"
        private const val KEY = "status_header"
    }
}
