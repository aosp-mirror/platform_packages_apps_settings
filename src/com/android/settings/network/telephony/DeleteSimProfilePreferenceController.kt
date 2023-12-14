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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.network.SubscriptionUtil
import com.android.settings.wifi.dpp.WifiDppUtils
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle

/** This controls a preference allowing the user to delete the profile for an eSIM.  */
class DeleteSimProfilePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {
    private var subscriptionId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var subscriptionInfo: SubscriptionInfo? = null
    private lateinit var preference: Preference

    fun init(subscriptionId: Int) {
        this.subscriptionId = subscriptionId
        subscriptionInfo = SubscriptionUtil.getAvailableSubscriptions(mContext)
            .find { it.subscriptionId == subscriptionId && it.isEmbedded }
    }

    override fun getAvailabilityStatus() = when (subscriptionInfo) {
        null -> CONDITIONALLY_UNAVAILABLE
        else -> AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        mContext.callStateFlow(subscriptionId).collectLatestWithLifecycle(viewLifecycleOwner) {
            preference.isEnabled = (it == TelephonyManager.CALL_STATE_IDLE)
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != preferenceKey) return false

        WifiDppUtils.showLockScreen(mContext) { deleteSim() }

        return true
    }

    private fun deleteSim() {
        SubscriptionUtil.startDeleteEuiccSubscriptionDialogActivity(mContext, subscriptionId)
        // result handled in MobileNetworkSettings
    }
}
