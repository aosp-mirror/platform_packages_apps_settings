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

package com.android.settings.network

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.network.NetworkCellularGroupProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow

@SearchIndexable(forTarget = SearchIndexable.ALL and SearchIndexable.ARC.inv())
class MobileNetworkListFragment : DashboardFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectAirplaneModeAndFinishIfOn()
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        if (Flags.isDualSimOnboardingEnabled()) {
            context?.startSpaActivity(NetworkCellularGroupProvider.name);
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Disable the animation of the preference list
        listView.itemAnimator = null

        findPreference<Preference>(KEY_ADD_SIM)!!.isVisible =
            MobileNetworkUtils.showEuiccSettings(context)
    }

    override fun getPreferenceScreenResId() = R.xml.network_provider_sims_list

    override fun getLogTag() = LOG_TAG

    override fun getMetricsCategory() = SettingsEnums.MOBILE_NETWORK_LIST

    companion object {
        private const val LOG_TAG = "NetworkListFragment"
        private const val KEY_ADD_SIM = "add_sim"

        @JvmStatic
        fun SettingsPreferenceFragment.collectAirplaneModeAndFinishIfOn() {
            requireContext().settingsGlobalBooleanFlow(Settings.Global.AIRPLANE_MODE_ON)
                .collectLatestWithLifecycle(viewLifecycleOwner) { isAirplaneModeOn ->
                    if (isAirplaneModeOn) {
                        finish()
                    }
                }
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = SearchIndexProvider()

        @VisibleForTesting
        class SearchIndexProvider : BaseSearchIndexProvider(R.xml.network_provider_sims_list) {
            public override fun isPageSearchEnabled(context: Context): Boolean =
                SubscriptionUtil.isSimHardwareVisible(context) &&
                    context.userManager.isAdminUser
        }
    }
}
