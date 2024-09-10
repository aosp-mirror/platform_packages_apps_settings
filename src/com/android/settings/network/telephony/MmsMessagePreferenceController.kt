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
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.data.ApnSetting
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.Settings.MobileNetworkActivity.EXTRA_MMS_MESSAGE
import com.android.settings.core.TogglePreferenceController
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.flow.combine

/** Preference controller for "MMS messages" */
class MmsMessagePreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val getDefaultDataSubId: () -> Int = {
        SubscriptionManager.getDefaultDataSubscriptionId()
    },
) : TogglePreferenceController(context, key) {

    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var telephonyManager: TelephonyManager =
        context.getSystemService(TelephonyManager::class.java)!!

    private var preferenceScreen: PreferenceScreen? = null

    fun init(subId: Int) {
        this.subId = subId
        telephonyManager = telephonyManager.createForSubscriptionId(subId)
    }

    override fun getAvailabilityStatus() =
        if (getAvailabilityStatus(telephonyManager, subId, getDefaultDataSubId)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preferenceScreen = screen
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        combine(
                MobileDataRepository(mContext).mobileDataEnabledChangedFlow(subId),
                mContext.subscriptionsChangedFlow(), // Capture isMobileDataPolicyEnabled() changes
            ) { _, _ ->
            }
            .collectLatestWithLifecycle(viewLifecycleOwner) {
                preferenceScreen?.let { super.displayPreference(it) }
            }
    }

    override fun getSliceHighlightMenuRes() = NO_RES

    override fun isChecked(): Boolean =
        telephonyManager.isMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED)

    override fun setChecked(isChecked: Boolean): Boolean {
        telephonyManager.setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED,
            isChecked,
        )
        return true
    }

    companion object {
        private fun getAvailabilityStatus(
            telephonyManager: TelephonyManager,
            subId: Int,
            getDefaultDataSubId: () -> Int,
        ): Boolean {
            return SubscriptionManager.isValidSubscriptionId(subId) &&
                !telephonyManager.isDataEnabled &&
                telephonyManager.isApnMetered(ApnSetting.TYPE_MMS) &&
                !isFallbackDataEnabled(telephonyManager, subId, getDefaultDataSubId())
        }

        private fun isFallbackDataEnabled(
            telephonyManager: TelephonyManager,
            subId: Int,
            defaultDataSubId: Int,
        ): Boolean {
            return defaultDataSubId != subId &&
                telephonyManager.createForSubscriptionId(defaultDataSubId).isDataEnabled &&
                telephonyManager.isMobileDataPolicyEnabled(
                    TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
        }

        class MmsMessageSearchItem(
            private val context: Context,
            private val getDefaultDataSubId: () -> Int = {
                SubscriptionManager.getDefaultDataSubscriptionId()
            },
        ) : MobileNetworkSettingsSearchItem {
            private var telephonyManager: TelephonyManager =
                context.getSystemService(TelephonyManager::class.java)!!

            @VisibleForTesting
            fun isAvailable(subId: Int): Boolean =
                getAvailabilityStatus(
                    telephonyManager.createForSubscriptionId(subId), subId, getDefaultDataSubId)

            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!isAvailable(subId)) return null
                return MobileNetworkSettingsSearchResult(
                    key = EXTRA_MMS_MESSAGE,
                    title = context.getString(R.string.mms_message_title),
                )
            }
        }
    }
}
