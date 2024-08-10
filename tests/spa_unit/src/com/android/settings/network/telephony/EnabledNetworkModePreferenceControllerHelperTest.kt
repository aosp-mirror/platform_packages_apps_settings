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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.CarrierConfigManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class EnabledNetworkModePreferenceControllerHelperTest {

    private var context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
        CarrierConfigRepository.setBooleanForTest(
            SUB_ID, CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL, true)
    }

    @Test
    fun getNetworkModePreferenceType_hideCarrierNetworkSettings_returnNone() {
        CarrierConfigRepository.setBooleanForTest(
            SUB_ID, CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL, true)

        val networkModePreferenceType = getNetworkModePreferenceType(context, SUB_ID)

        assertThat(networkModePreferenceType).isEqualTo(NetworkModePreferenceType.None)
    }

    @Test
    fun getNetworkModePreferenceType_hidePreferredNetworkType_returnNone() {
        CarrierConfigRepository.setBooleanForTest(
            SUB_ID, CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL, true)

        val networkModePreferenceType = getNetworkModePreferenceType(context, SUB_ID)

        assertThat(networkModePreferenceType).isEqualTo(NetworkModePreferenceType.None)
    }

    @Test
    fun getNetworkModePreferenceType_carrierConfigNotReady_returnNone() {
        CarrierConfigRepository.setBooleanForTest(
            SUB_ID, CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL, false)

        val networkModePreferenceType = getNetworkModePreferenceType(context, SUB_ID)

        assertThat(networkModePreferenceType).isEqualTo(NetworkModePreferenceType.None)
    }

    @Test
    fun getNetworkModePreferenceType_isWorldPhone_returnPreferredNetworkMode() {
        CarrierConfigRepository.setBooleanForTest(
            SUB_ID, CarrierConfigManager.KEY_WORLD_PHONE_BOOL, true)

        val networkModePreferenceType = getNetworkModePreferenceType(context, SUB_ID)

        assertThat(networkModePreferenceType)
            .isEqualTo(NetworkModePreferenceType.PreferredNetworkMode)
    }

    @Test
    fun getNetworkModePreferenceType_notWorldPhone_returnEnabledNetworkMode() {
        CarrierConfigRepository.setBooleanForTest(
            SUB_ID, CarrierConfigManager.KEY_WORLD_PHONE_BOOL, false)

        val networkModePreferenceType = getNetworkModePreferenceType(context, SUB_ID)

        assertThat(networkModePreferenceType)
            .isEqualTo(NetworkModePreferenceType.EnabledNetworkMode)
    }

    private companion object {
        const val SUB_ID = 10
    }
}
