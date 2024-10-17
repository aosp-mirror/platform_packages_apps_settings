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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkModesTest {

    @Test
    fun addNrToLteNetworkMode_lteOnly() {
        val nrNetworkMode =
            NetworkModes.addNrToLteNetworkMode(TelephonyManager.NETWORK_MODE_LTE_ONLY)

        assertThat(nrNetworkMode).isEqualTo(TelephonyManager.NETWORK_MODE_NR_LTE)
    }

    @Test
    fun addNrToLteNetworkMode_lteCdmaEvdo() {
        val nrNetworkMode =
            NetworkModes.addNrToLteNetworkMode(TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO)

        assertThat(nrNetworkMode).isEqualTo(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO)
    }

    @Test
    fun reduceNrToLteNetworkMode_nrLte() {
        val lteNetworkMode =
            NetworkModes.reduceNrToLteNetworkMode(TelephonyManager.NETWORK_MODE_NR_LTE)

        assertThat(lteNetworkMode).isEqualTo(TelephonyManager.NETWORK_MODE_LTE_ONLY)
    }

    @Test
    fun reduceNrToLteNetworkMode_nrLteCdmaEvdo() {
        val lteNetworkMode =
            NetworkModes.reduceNrToLteNetworkMode(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO)

        assertThat(lteNetworkMode).isEqualTo(TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO)
    }
}
