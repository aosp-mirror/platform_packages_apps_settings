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

package com.android.settings.datausage

import android.app.settings.SettingsEnums
import android.net.NetworkPolicy
import android.net.NetworkTemplate
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.EventLog
import android.util.Log
import android.view.View
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.datausage.lib.BillingCycleRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlin.jvm.optionals.getOrNull

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through [NetworkPolicy].
 */
@OpenForTesting
open class DataUsageList : DashboardFragment() {
    @VisibleForTesting
    var template: NetworkTemplate? = null
        private set

    @VisibleForTesting
    var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        private set

    private lateinit var billingCycleRepository: BillingCycleRepository

    private var usageAmount: Preference? = null
    private var dataUsageListAppsController: DataUsageListAppsController? = null
    private var chartDataUsagePreferenceController: ChartDataUsagePreferenceController? = null
    private var dataUsageListHeaderController: DataUsageListHeaderController? = null

    private val viewModel: DataUsageListViewModel by viewModels()

    override fun getMetricsCategory() = SettingsEnums.DATA_USAGE_LIST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingCycleRepository = BillingCycleRepository(requireContext())
        if (requireContext().userManager.isGuestUser) {
            Log.e(TAG, "This setting isn't available for guest user")
            EventLog.writeEvent(0x534e4554, "262741858", -1 /* UID */, "Guest user")
            finish()
            return
        }
        if (!billingCycleRepository.isBandwidthControlEnabled()) {
            Log.w(TAG, "No bandwidth control; leaving")
            finish()
            return
        }
        usageAmount = findPreference(KEY_USAGE_AMOUNT)
        processArgument()
        val template = template
        if (template == null) {
            Log.e(TAG, "No template; leaving")
            finish()
            return
        }
        dataUsageListAppsController = use(DataUsageListAppsController::class.java).apply {
            init(template)
        }
        chartDataUsagePreferenceController = use(ChartDataUsagePreferenceController::class.java)
            .apply { init(template) }

        updateWarning()
    }

    private fun updateWarning() {
        val template = template ?: return
        val warningPreference = findPreference<Preference>(KEY_WARNING)!!
        if (template.matchRule != NetworkTemplate.MATCH_WIFI) {
            warningPreference.setSummary(R.string.operator_warning)
        } else if (SubscriptionUtil.isSimHardwareVisible(context)) {
            warningPreference.setSummary(R.string.non_carrier_data_usage_warning)
        }
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        billingCycleRepository.isModifiableFlow(subId)
            .collectLatestWithLifecycle(viewLifecycleOwner, action = ::updatePolicy)

        val template = template ?: return
        viewModel.templateFlow.value = template
        dataUsageListHeaderController = DataUsageListHeaderController(
            setPinnedHeaderView(R.layout.apps_filter_spinner),
            template,
            metricsCategory,
            viewLifecycleOwner,
            viewModel.cyclesFlow,
            ::updateSelectedCycle,
        )
        viewModel.cyclesFlow.collectLatestWithLifecycle(viewLifecycleOwner) { cycles ->
            dataUsageListAppsController?.updateCycles(cycles)
        }
        viewModel.chartDataFlow.collectLatestWithLifecycle(viewLifecycleOwner) { chartData ->
            chartDataUsagePreferenceController?.update(chartData)
        }
        finishIfSubscriptionDisabled()
    }

    private fun finishIfSubscriptionDisabled() {
        if (SubscriptionManager.isUsableSubscriptionId(subId)) {
            SubscriptionRepository(requireContext()).isSubscriptionEnabledFlow(subId)
                .collectLatestWithLifecycle(viewLifecycleOwner) { isSubscriptionEnabled ->
                    if (!isSubscriptionEnabled) finish()
                }
        }
    }

    override fun getPreferenceScreenResId() = R.xml.data_usage_list

    override fun getLogTag() = TAG

    private fun processArgument() {
        arguments?.let {
            subId = it.getInt(EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            template = it.getParcelable(EXTRA_NETWORK_TEMPLATE, NetworkTemplate::class.java)
        }
        if (template == null && subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            subId = intent.getIntExtra(
                Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            )
            template = intent.getParcelableExtra(
                Settings.EXTRA_NETWORK_TEMPLATE,
                NetworkTemplate::class.java,
            ) ?: DataUsageUtils.getMobileNetworkTemplateFromSubId(context, intent).getOrNull()
        }
    }

    /** Update chart sweeps and cycle list to reflect [NetworkPolicy] for current [template]. */
    private fun updatePolicy(isModifiable: Boolean) {
        dataUsageListHeaderController?.setConfigButtonVisible(isModifiable)
        chartDataUsagePreferenceController?.setBillingCycleModifiable(isModifiable)
    }

    /**
     * Updates the chart and detail data when initial loaded or selected cycle changed.
     */
    private fun updateSelectedCycle(usageData: NetworkUsageData) {
        Log.d(TAG, "showing cycle $usageData")

        usageAmount?.title = usageData.getDataUsedString(requireContext()).displayText
        viewModel.selectedCycleFlow.value = usageData

        updateApps(usageData)
    }

    /** Updates applications data usage. */
    private fun updateApps(usageData: NetworkUsageData) {
        dataUsageListAppsController?.update(
            subId = subId,
            startTime = usageData.startTime,
            endTime = usageData.endTime,
        )
    }

    companion object {
        const val EXTRA_SUB_ID = "sub_id"
        const val EXTRA_NETWORK_TEMPLATE = "network_template"

        private const val TAG = "DataUsageList"
        private const val KEY_USAGE_AMOUNT = "usage_amount"

        @VisibleForTesting
        const val KEY_WARNING = "warning"
    }
}
