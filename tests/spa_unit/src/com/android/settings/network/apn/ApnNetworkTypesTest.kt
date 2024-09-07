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

package com.android.settings.network.apn

import android.telephony.TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
import android.telephony.TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP
import android.telephony.TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.widget.editor.SettingsDropdownCheckOption
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApnNetworkTypesTest {

    @Test
    fun getNetworkTypeOptions() {
        val networkTypeOptions =
            ApnNetworkTypes.getNetworkTypeOptions(
                NETWORK_TYPE_BITMASK_EDGE xor NETWORK_TYPE_BITMASK_CDMA
            )

        assertThat(networkTypeOptions.single { it.text == "EDGE" }.selected.value).isTrue()
        assertThat(networkTypeOptions.single { it.text == "CDMA" }.selected.value).isTrue()
        assertThat(networkTypeOptions.single { it.text == "GPRS" }.selected.value).isFalse()
    }

    @Test
    fun optionsToNetworkType() {
        val options = listOf(
            SettingsDropdownCheckOption(text = "", selected = mutableStateOf(false)),
            SettingsDropdownCheckOption(text = "", selected = mutableStateOf(true)),
            SettingsDropdownCheckOption(text = "", selected = mutableStateOf(false)),
            SettingsDropdownCheckOption(text = "", selected = mutableStateOf(true)),
        )

        val networkType = ApnNetworkTypes.optionsToNetworkType(options)

        assertThat(networkType).isEqualTo(NETWORK_TYPE_BITMASK_HSPAP xor NETWORK_TYPE_BITMASK_HSUPA)
    }
}
