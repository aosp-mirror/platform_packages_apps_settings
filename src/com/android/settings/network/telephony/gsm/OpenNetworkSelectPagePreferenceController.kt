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

package com.android.settings.network.telephony.gsm

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.Settings.NetworkSelectActivity
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.network.telephony.TelephonyBasePreferenceController
import com.android.settings.network.telephony.allowedNetworkTypesFlow
import com.android.settings.network.telephony.serviceStateFlow
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Preference controller for "Open network select"
 */
class OpenNetworkSelectPagePreferenceController @JvmOverloads constructor(
    context: Context,
    key: String,
    private val allowedNetworkTypesFlowFactory: (subId: Int) -> Flow<Long> =
        context::allowedNetworkTypesFlow,
    private val serviceStateFlowFactory: (subId: Int) -> Flow<ServiceState> =
        context::serviceStateFlow,
) : TelephonyBasePreferenceController(context, key),
    AutoSelectPreferenceController.OnNetworkSelectModeListener {

    private var preference: Preference? = null

    /**
     * Initialization based on given subscription id.
     */
    fun init(subId: Int): OpenNetworkSelectPagePreferenceController {
        mSubId = subId
        return this
    }

    override fun getAvailabilityStatus(subId: Int) =
        if (MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        preference?.intent = Intent().apply {
            setClass(mContext, NetworkSelectActivity::class.java)
            putExtra(Settings.EXTRA_SUB_ID, mSubId)
        }
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        allowedNetworkTypesFlowFactory(mSubId).collectLatestWithLifecycle(viewLifecycleOwner) {
            preference?.isVisible = withContext(Dispatchers.Default) {
                MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, mSubId)
            }
        }

        serviceStateFlowFactory(mSubId)
            .collectLatestWithLifecycle(viewLifecycleOwner) { serviceState ->
                preference?.summary = if (serviceState.state == ServiceState.STATE_IN_SERVICE) {
                    withContext(Dispatchers.Default) {
                        MobileNetworkUtils.getCurrentCarrierNameForDisplay(mContext, mSubId)
                    }
                } else {
                    mContext.getString(R.string.network_disconnected)
                }
            }
    }

    override fun onNetworkSelectModeUpdated(mode: Int) {
        preference?.isEnabled = mode != TelephonyManager.NETWORK_SELECTION_MODE_AUTO
    }
}
