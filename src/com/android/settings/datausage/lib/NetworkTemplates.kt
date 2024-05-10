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

package com.android.settings.datausage.lib

import android.content.Context
import android.net.NetworkTemplate
import android.telephony.SubscriptionManager
import androidx.annotation.StringRes
import com.android.settings.R
import com.android.settings.datausage.DataUsageUtils

interface INetworkTemplates {
    /**
     * Returns the default network template based on the availability of mobile data, Wifi. Returns
     * ethernet template if both mobile data and Wifi are not available.
     */
    fun getDefaultTemplate(context: Context): NetworkTemplate
}

object NetworkTemplates : INetworkTemplates {
    @JvmStatic
    @StringRes
    fun NetworkTemplate.getTitleResId(): Int =
        when (matchRule) {
            NetworkTemplate.MATCH_MOBILE,
            NetworkTemplate.MATCH_CARRIER -> R.string.cellular_data_usage

            NetworkTemplate.MATCH_WIFI -> R.string.wifi_data_usage
            NetworkTemplate.MATCH_ETHERNET -> R.string.ethernet_data_usage
            else -> R.string.data_usage_app_summary_title
        }

    /**
     * Returns the default network template based on the availability of mobile data, Wifi. Returns
     * ethernet template if both mobile data and Wifi are not available.
     */
    override fun getDefaultTemplate(context: Context): NetworkTemplate =
        DataUsageUtils.getDefaultTemplate(
            context,
            SubscriptionManager.getDefaultDataSubscriptionId(),
        )
}
