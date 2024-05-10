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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MmsMessagePreferenceControllerTest {
    private val mockTelephonyManager: TelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(any()) } doReturn mock
    }

    private var context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val controller = MmsMessagePreferenceController(context, KEY).apply {
        init(SUB_ID)
    }

    @Test
    fun getAvailabilityStatus_invalidSubscription_returnUnavailable() {
        controller.init(INVALID_SUBSCRIPTION_ID)

        val availabilityStatus = controller.getAvailabilityStatus(INVALID_SUBSCRIPTION_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_mobileDataOn_returnUnavailable() {
        mockTelephonyManager.stub {
            on { isDataEnabled } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_meteredOff_returnUnavailable() {
        mockTelephonyManager.stub {
            on { isApnMetered(ApnSetting.TYPE_MMS) } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_mobileDataOffWithValidSubId_returnAvailable() {
        mockTelephonyManager.stub {
            on { isDataEnabled } doReturn false
            on { isApnMetered(ApnSetting.TYPE_MMS) } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun isChecked_whenMmsNotAlwaysAllowed_returnFalse() {
        mockTelephonyManager.stub {
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED)
            } doReturn false
        }

        val isChecked = controller.isChecked()

        assertThat(isChecked).isFalse()
    }

    @Test
    fun isChecked_whenMmsAlwaysAllowed_returnTrue() {
        mockTelephonyManager.stub {
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

        verify(mockTelephonyManager).setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED, true
        )
    }

    @Test
    fun setChecked_setFalse_setDataIntoSubscriptionManager() {
        controller.setChecked(false)

        verify(mockTelephonyManager).setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED, false
        )
    }

    private companion object {
        const val KEY = "mms_message"
        const val SUB_ID = 2
    }
}
