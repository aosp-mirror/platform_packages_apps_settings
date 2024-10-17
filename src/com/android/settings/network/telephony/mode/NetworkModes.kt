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

package com.android.settings.network.telephony.mode

import android.telephony.TelephonyManager
import com.google.common.collect.ImmutableBiMap

/** Network mode related utilities. */
object NetworkModes {
    const val NETWORK_MODE_UNKNOWN = -1

    private val LteToNrNetworkModeMap =
        ImmutableBiMap.builder<Int, Int>()
            .put(TelephonyManager.NETWORK_MODE_LTE_ONLY, TelephonyManager.NETWORK_MODE_NR_LTE)
            .put(
                TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO,
                TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_GSM_WCDMA,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_WCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_WCDMA,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_TDSCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM,
                TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA,
            )
            .put(
                TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA,
                TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA,
            )
            .build()

    /**
     * Transforms LTE network mode to 5G network mode.
     *
     * @param networkMode an LTE network mode without 5G.
     * @return the corresponding network mode with 5G.
     */
    @JvmStatic
    fun addNrToLteNetworkMode(networkMode: Int): Int =
        LteToNrNetworkModeMap.getOrElse(networkMode) { networkMode }

    /**
     * Transforms NR5G network mode to LTE network mode.
     *
     * @param networkMode an 5G network mode.
     * @return the corresponding network mode without 5G.
     */
    @JvmStatic
    fun reduceNrToLteNetworkMode(networkMode: Int): Int =
        LteToNrNetworkModeMap.inverse().getOrElse(networkMode) { networkMode }
}
