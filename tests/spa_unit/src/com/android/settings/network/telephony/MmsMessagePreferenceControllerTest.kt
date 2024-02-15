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
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import android.telephony.TelephonyManager
import android.telephony.data.ApnSetting
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MmsMessagePreferenceControllerTest {
    private val mockTelephonyManager1: TelephonyManager = mock<TelephonyManager> {
        on { isApnMetered(ApnSetting.TYPE_MMS) } doReturn true
    }

    private val mockTelephonyManager2: TelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_1_ID) } doReturn mockTelephonyManager1
        on { isApnMetered(ApnSetting.TYPE_MMS) } doReturn true
    }

    private val mockTelephonyManager: TelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_1_ID) } doReturn mockTelephonyManager1
        on { createForSubscriptionId(SUB_2_ID) } doReturn mockTelephonyManager2
        on { createForSubscriptionId(INVALID_SUBSCRIPTION_ID) } doReturn mock
    }

    private var context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private var defaultDataSubId = SUB_1_ID

    private val controller = MmsMessagePreferenceController(
        context = context,
        key = KEY,
        getDefaultDataSubId = { defaultDataSubId },
    ).apply { init(SUB_2_ID) }

    @Test
    fun getAvailabilityStatus_invalidSubscription_unavailable() {
        controller.init(INVALID_SUBSCRIPTION_ID)

        val availabilityStatus = controller.getAvailabilityStatus(INVALID_SUBSCRIPTION_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_mobileDataOn_unavailable() {
        mockTelephonyManager2.stub {
            on { isDataEnabled } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_2_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_meteredOff_unavailable() {
        mockTelephonyManager2.stub {
            on { isApnMetered(ApnSetting.TYPE_MMS) } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_2_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_isDefaultDataAndDataOnAndAutoDataSwitchOn_unavailable() {
        defaultDataSubId = SUB_2_ID
        mockTelephonyManager2.stub {
            on { isDataEnabled } doReturn true
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
            } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_2_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_isDefaultDataAndDataOffAndAutoDataSwitchOn_available() {
        defaultDataSubId = SUB_2_ID
        mockTelephonyManager2.stub {
            on { isDataEnabled } doReturn false
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
            } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_2_ID)

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_defaultDataOnAndAutoDataSwitchOn_unavailable() {
        mockTelephonyManager1.stub {
            on { isDataEnabled } doReturn true
        }
        mockTelephonyManager2.stub {
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
            } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_2_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_defaultDataOffAndAutoDataSwitchOn_available() {
        mockTelephonyManager1.stub {
            on { isDataEnabled } doReturn false
        }
        mockTelephonyManager2.stub {
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
            } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_2_ID)

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun isChecked_whenMmsNotAlwaysAllowed_returnFalse() {
        mockTelephonyManager2.stub {
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED)
            } doReturn false
        }

        val isChecked = controller.isChecked()

        assertThat(isChecked).isFalse()
    }

    @Test
    fun isChecked_whenMmsAlwaysAllowed_returnTrue() {
        mockTelephonyManager2.stub {
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED)
            } doReturn true
        }

        val isChecked = controller.isChecked()

        assertThat(isChecked).isTrue()
    }

    @Test
    fun setChecked_setTrue_setDataIntoSubscriptionManager() {
        controller.setChecked(true)

        verify(mockTelephonyManager2).setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED, true
        )
    }

    @Test
    fun setChecked_setFalse_setDataIntoSubscriptionManager() {
        controller.setChecked(false)

        verify(mockTelephonyManager2).setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED, false
        )
    }

    private companion object {
        const val KEY = "mms_message"
        const val SUB_1_ID = 1
        const val SUB_2_ID = 2
    }
}
