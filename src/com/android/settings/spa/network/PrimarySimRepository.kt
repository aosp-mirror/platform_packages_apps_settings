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

package com.android.settings.spa.network

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.R
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.CarrierConfigRepository
import com.android.settingslib.spa.widget.preference.ListPreferenceOption

class PrimarySimRepository(private val context: Context) {

    data class PrimarySimInfo(
        val callsList: List<ListPreferenceOption>,
        val smsList: List<ListPreferenceOption>,
        val dataList: List<ListPreferenceOption>,
    )

    fun getPrimarySimInfo(selectableSubscriptionInfoList: List<SubscriptionInfo>): PrimarySimInfo? {
        if (selectableSubscriptionInfoList.size < 2) {
            Log.d(TAG, "Hide primary sim")
            return null
        }

        val callsList = mutableListOf<ListPreferenceOption>()
        val smsList = mutableListOf<ListPreferenceOption>()
        val dataList = mutableListOf<ListPreferenceOption>()
        for (info in selectableSubscriptionInfoList) {
            val item = ListPreferenceOption(
                id = info.subscriptionId,
                text = "${info.displayName}",
                summary = SubscriptionUtil.getBidiFormattedPhoneNumber(context, info) ?: "",
            )
            callsList += item
            smsList += item
            dataList += item
        }

        val askEveryTime = ListPreferenceOption(
            id = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            text = context.getString(R.string.sim_calls_ask_first_prefs_title),
        )
        callsList += askEveryTime
        if (context.resources
                .getBoolean(com.android.internal.R.bool.config_sms_ask_every_time_support)) {
            smsList += askEveryTime
        }

        return PrimarySimInfo(callsList, smsList, dataList)
    }

    private companion object {
        private const val TAG = "PrimarySimRepository"
    }
}
