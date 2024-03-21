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

package com.android.settings.network.apn

import android.os.PersistableBundle
import android.provider.Telephony
import android.telephony.CarrierConfigManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ApnStatusTest {
    private val apnData = ApnData(subId = 1)

    private val configManager = mock<CarrierConfigManager> {
        val p = PersistableBundle()
        p.putBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL, true)
        on {
            getConfigForSubId(
                apnData.subId,
                CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY,
                CarrierConfigManager.KEY_READ_ONLY_APN_FIELDS_STRING_ARRAY,
                CarrierConfigManager.KEY_APN_SETTINGS_DEFAULT_APN_TYPES_STRING_ARRAY,
                CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_PROTOCOL_STRING,
                CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_ROAMING_PROTOCOL_STRING,
                CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL
            )
        } doReturn p
    }

    @Test
    fun getCarrierCustomizedConfig_test() {
        assert(getCarrierCustomizedConfig(apnData, configManager).isAddApnAllowed)
    }

    @Test
    fun isFieldEnabled_default() {
        val apnData = ApnData()

        val enabled = apnData.isFieldEnabled(Telephony.Carriers.NAME)

        assertThat(enabled).isTrue()
    }

    @Test
    fun isFieldEnabled_readOnlyApn() {
        val apnData = ApnData(customizedConfig = CustomizedConfig(readOnlyApn = true))

        val enabled = apnData.isFieldEnabled(Telephony.Carriers.NAME)

        assertThat(enabled).isFalse()
    }

    @Test
    fun isFieldEnabled_readOnlyApnFields() {
        val apnData = ApnData(
            customizedConfig = CustomizedConfig(
                readOnlyApnFields = listOf(Telephony.Carriers.NAME, Telephony.Carriers.PROXY),
            ),
        )

        assertThat(apnData.isFieldEnabled(Telephony.Carriers.NAME)).isFalse()
        assertThat(apnData.isFieldEnabled(Telephony.Carriers.PROXY)).isFalse()
        assertThat(apnData.isFieldEnabled(Telephony.Carriers.APN)).isTrue()
    }
}
