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
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.SubscriptionUtil
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class MobileNetworkImeiPreferenceControllerTest {
    private lateinit var mockSession: MockitoSession

    private val mockViewModels =  mock<Lazy<SubscriptionInfoListViewModel>>()
    private val mockFragment = mock<Fragment>{
        val viewmodel = mockViewModels
    }

    private var mockImei = String()
    private val mockTelephonyManager = mock<TelephonyManager> {
        on { uiccCardsInfo } doReturn listOf()
        on { createForSubscriptionId(any()) } doReturn mock
        on { currentPhoneType } doReturn TelephonyManager.PHONE_TYPE_GSM
        on { imei } doReturn mockImei
        on { meid } doReturn mockImei
        on { primaryImei } doReturn mockImei
        on { activeModemCount } doReturn 2
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val controller = MobileNetworkImeiPreferenceController(context, TEST_KEY)
    private val preference = Preference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(SubscriptionUtil::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()

        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun refreshData_getImei_preferenceSummaryIsExpected() = runBlocking {
        whenever(SubscriptionUtil.isSimHardwareVisible(context)).thenReturn(true)
        whenever(SubscriptionUtil.getActiveSubscriptions(any())).thenReturn(
            listOf(
                SUB_INFO_1,
                SUB_INFO_2
            )
        )
        var mockSubId = 2
        controller.init(mockFragment, mockSubId)
        mockImei = "test imei"
        mockTelephonyManager.stub {
            on { imei } doReturn mockImei
        }

        controller.refreshData(SUB_INFO_2)

        assertThat(preference.summary).isEqualTo(mockImei)
    }

    @Test
    fun refreshData_getImeiTitle_showImei() = runBlocking {
        whenever(SubscriptionUtil.isSimHardwareVisible(context)).thenReturn(true)
        whenever(SubscriptionUtil.getActiveSubscriptions(any())).thenReturn(
            listOf(
                SUB_INFO_1,
                SUB_INFO_2
            )
        )
        var mockSubId = 2
        controller.init(mockFragment, mockSubId)
        mockImei = "test imei"
        mockTelephonyManager.stub {
            on { imei } doReturn mockImei
            on { primaryImei } doReturn ""
        }

        controller.refreshData(SUB_INFO_2)

        assertThat(preference.title).isEqualTo(context.getString(R.string.status_imei))
    }

    @Test
    fun refreshData_getPrimaryImeiTitle_showPrimaryImei() = runBlocking {
        whenever(SubscriptionUtil.isSimHardwareVisible(context)).thenReturn(true)
        whenever(SubscriptionUtil.getActiveSubscriptions(any())).thenReturn(
            listOf(
                SUB_INFO_1,
                SUB_INFO_2
            )
        )
        var mockSubId = 2
        controller.init(mockFragment, mockSubId)
        mockImei = "test imei"
        mockTelephonyManager.stub {
            on { imei } doReturn mockImei
            on { primaryImei } doReturn mockImei
        }

        controller.refreshData(SUB_INFO_2)

        assertThat(preference.title).isEqualTo(context.getString(R.string.imei_primary))
    }

    @Test
    fun getAvailabilityStatus_notSimHardwareVisible() {
        whenever(SubscriptionUtil.isSimHardwareVisible(context)).thenReturn(false)

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(1)
            setDisplayName(DISPLAY_NAME_1)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(2)
            setDisplayName(DISPLAY_NAME_2)
        }.build()

    }
}
