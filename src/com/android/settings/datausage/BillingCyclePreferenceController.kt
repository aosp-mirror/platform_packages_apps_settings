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
import android.telephony.SubscriptionManager
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.datausage.lib.DataUsageLib.getMobileTemplate
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult

class BillingCyclePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {
    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    fun init(subId: Int) {
        this.subId = subId
    }

    override fun getAvailabilityStatus() =
        if (DataUsageUtils.hasMobileData(mContext)) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val preference = screen.findPreference<BillingCyclePreference>(preferenceKey)
        val template = getMobileTemplate(mContext, subId)
        preference?.setTemplate(template, subId)
    }

    companion object {
        class BillingCycleSearchItem(private val context: Context) :
            MobileNetworkSettingsSearchItem {
            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!DataUsageUtils.hasMobileData(context)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "billing_preference",
                    title = context.getString(R.string.billing_cycle),
                )
            }
        }
    }
}
