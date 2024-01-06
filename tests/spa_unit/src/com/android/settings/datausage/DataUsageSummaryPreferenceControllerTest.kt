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

package com.android.settings.datausage

import android.content.Context
import android.net.NetworkPolicy
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionPlan
import android.telephony.TelephonyManager
import android.util.Range
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settings.network.ProxySubscriptionManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class DataUsageSummaryPreferenceControllerTest {

    private var policy: NetworkPolicy? = mock<NetworkPolicy>()

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { isDataCapable } doReturn true
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { getSubscriptionPlans(any()) } doReturn emptyList()
    }

    private val mockProxySubscriptionManager = mock<ProxySubscriptionManager> {
        on { get() } doReturn mockSubscriptionManager
    }

    private val fakeNetworkCycleDataRepository = object : INetworkCycleDataRepository {
        override fun getCycles(): List<Range<Long>> = emptyList()
        override fun getPolicy() = policy
        override fun queryUsage(range: Range<Long>) = NetworkUsageData.AllZero
    }

    private var dataPlanInfo = EMPTY_DATA_PLAN_INFO

    private val fakeDataPlanRepository = object : DataPlanRepository {
        override fun getDataPlanInfo(policy: NetworkPolicy, plans: List<SubscriptionPlan>) =
            dataPlanInfo
    }

    private val controller = DataUsageSummaryPreferenceController(
        context = context,
        subId = SUB_ID,
        proxySubscriptionManager = mockProxySubscriptionManager,
        networkCycleDataRepositoryFactory = { fakeNetworkCycleDataRepository },
        dataPlanRepositoryFactory = { fakeDataPlanRepository },
    )

    private val preference = mock<DataUsageSummaryPreference> {
        on { key } doReturn controller.preferenceKey
    }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
    }

    @Test
    fun getAvailabilityStatus_noMobileData_conditionallyUnavailable() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_hasSubInfoAndPolicy_available() {
        mockProxySubscriptionManager.stub {
            on { getAccessibleSubscriptionInfo(SUB_ID) } doReturn SubscriptionInfo.Builder().build()
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_noSubInfo_conditionallyUnavailable() {
        mockProxySubscriptionManager.stub {
            on { getAccessibleSubscriptionInfo(SUB_ID) } doReturn null
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_noPolicy_conditionallyUnavailable() {
        policy = null

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun displayPreference_policyHasNoLimitInfo() {
        policy = mock<NetworkPolicy>().apply {
            warningBytes = NetworkPolicy.WARNING_DISABLED
            limitBytes = NetworkPolicy.LIMIT_DISABLED
        }

        controller.displayPreference(preferenceScreen)

        verify(preference).setLimitInfo(null)
        verify(preference, never()).setLabels(any(), any())
    }

    @Test
    fun displayPreference_policyWarningOnly() {
        policy = mock<NetworkPolicy>().apply {
            warningBytes = 1L
            limitBytes = NetworkPolicy.LIMIT_DISABLED
        }

        controller.displayPreference(preferenceScreen)

        val limitInfo = argumentCaptor {
            verify(preference).setLimitInfo(capture())
        }.firstValue.toString()
        assertThat(limitInfo).isEqualTo("1 B data warning")
        verify(preference).setLabels("0 B", "1 B")
    }

    @Test
    fun displayPreference_policyLimitOnly() {
        policy = mock<NetworkPolicy>().apply {
            warningBytes = NetworkPolicy.WARNING_DISABLED
            limitBytes = 1L
        }

        controller.displayPreference(preferenceScreen)

        val limitInfo = argumentCaptor {
            verify(preference).setLimitInfo(capture())
        }.firstValue.toString()
        assertThat(limitInfo).isEqualTo("1 B data limit")
        verify(preference).setLabels("0 B", "1 B")
    }

    @Test
    fun displayPreference_policyHasWarningAndLimit() {
        policy = mock<NetworkPolicy>().apply {
            warningBytes = BillingCycleSettings.GIB_IN_BYTES / 2
            limitBytes = BillingCycleSettings.GIB_IN_BYTES
        }

        controller.displayPreference(preferenceScreen)

        val limitInfo = argumentCaptor {
            verify(preference).setLimitInfo(capture())
        }.firstValue.toString()
        assertThat(limitInfo).isEqualTo("512 MB data warning / 1.00 GB data limit")
        verify(preference).setLabels("0 B", "1.00 GB")
    }

    @Test
    fun onViewCreated_emptyDataPlanInfo() = runBlocking {
        dataPlanInfo = EMPTY_DATA_PLAN_INFO
        controller.displayPreference(preferenceScreen)
        clearInvocations(preference)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        verify(preference).setUsageNumbers(
            EMPTY_DATA_PLAN_INFO.dataPlanUse,
            EMPTY_DATA_PLAN_INFO.dataPlanSize,
        )
        verify(preference).setChartEnabled(false)
        verify(preference).setUsageInfo(
            EMPTY_DATA_PLAN_INFO.cycleEnd,
            EMPTY_DATA_PLAN_INFO.snapshotTime,
            null,
            EMPTY_DATA_PLAN_INFO.dataPlanCount,
        )
    }

    @Test
    fun onViewCreated_positiveDataPlanInfo() = runBlocking {
        dataPlanInfo = POSITIVE_DATA_PLAN_INFO
        controller.displayPreference(preferenceScreen)
        clearInvocations(preference)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        verify(preference).setUsageNumbers(
            POSITIVE_DATA_PLAN_INFO.dataPlanUse,
            POSITIVE_DATA_PLAN_INFO.dataPlanSize,
        )
        verify(preference).setChartEnabled(true)
        verify(preference).setLabels("0 B", "9 B")
        val progress = argumentCaptor {
            verify(preference).setProgress(capture())
        }.firstValue
        assertThat(progress).isEqualTo(0.8888889f)
        verify(preference).setUsageInfo(
            POSITIVE_DATA_PLAN_INFO.cycleEnd,
            POSITIVE_DATA_PLAN_INFO.snapshotTime,
            null,
            POSITIVE_DATA_PLAN_INFO.dataPlanCount,
        )
    }

    private companion object {
        const val SUB_ID = 1234
        val EMPTY_DATA_PLAN_INFO = DataPlanInfo(
            dataPlanCount = 0,
            dataPlanSize = SubscriptionPlan.BYTES_UNKNOWN,
            dataBarSize = SubscriptionPlan.BYTES_UNKNOWN,
            dataPlanUse = 0,
            cycleEnd = null,
            snapshotTime = SubscriptionPlan.TIME_UNKNOWN,
        )
        val POSITIVE_DATA_PLAN_INFO = DataPlanInfo(
            dataPlanCount = 0,
            dataPlanSize = 10L,
            dataBarSize = 9L,
            dataPlanUse = 8L,
            cycleEnd = 7L,
            snapshotTime = 6L,
        )
    }
}
