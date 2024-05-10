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
import android.content.Intent
import android.net.NetworkTemplate
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.DataUnit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE
import com.android.settings.datausage.DataUsageUtils
import com.android.settings.datausage.lib.DataUsageLib
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settingslib.spa.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class DataUsagePreferenceControllerTest {

    private lateinit var mockSession: MockitoSession

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }

    private val preference = Preference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
    private val networkTemplate = mock<NetworkTemplate>()
    private val repository = mock<NetworkCycleDataRepository> {
        on { queryUsage(any()) } doReturn NetworkUsageData(START_TIME, END_TIME, 0L)
    }

    private val controller = spy(DataUsagePreferenceController(context, TEST_KEY)) {
        doReturn(repository).whenever(mock).createNetworkCycleDataRepository()
    }

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(SubscriptionManager::class.java)
            .spyStatic(DataUsageUtils::class.java)
            .spyStatic(DataUsageLib::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()

        whenever(SubscriptionManager.isValidSubscriptionId(SUB_ID)).thenReturn(true)
        ExtendedMockito.doReturn(true).`when` { DataUsageUtils.hasMobileData(context) }
        ExtendedMockito.doReturn(networkTemplate).`when` {
            DataUsageLib.getMobileTemplate(context, SUB_ID)
        }

        preferenceScreen.addPreference(preference)
        controller.apply {
            init(SUB_ID)
            displayPreference(preferenceScreen)
        }
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun getAvailabilityStatus_validSubId_returnAvailable() {
        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_invalidSubId_returnUnsearchable() {
        controller.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE_UNSEARCHABLE)
    }

    @Test
    fun handlePreferenceTreeClick_startActivity() = runBlocking {
        val usageData = NetworkUsageData(START_TIME, END_TIME, 1L)
        repository.stub {
            on { loadFirstCycle() } doReturn usageData
        }
        controller.onViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))
        waitUntil { preference.summary != null }

        controller.handlePreferenceTreeClick(preference)

        val intent = argumentCaptor<Intent> {
            verify(context).startActivity(capture())
        }.firstValue
        assertThat(intent.action).isEqualTo(Settings.ACTION_MOBILE_DATA_USAGE)
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID, 0)).isEqualTo(SUB_ID)
    }

    @Test
    fun updateState_invalidSubId_disabled() = runBlocking {
        controller.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        controller.onViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))

        waitUntil { !preference.isEnabled }
    }

    @Test
    fun updateState_noUsageData_shouldDisablePreference() = runBlocking {
        val usageData = NetworkUsageData(START_TIME, END_TIME, 0L)
        repository.stub {
            on { loadFirstCycle() } doReturn usageData
        }

        controller.onViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))

        waitUntil { !preference.isEnabled }
    }

    @Test
    fun updateState_shouldUseIecUnit() = runBlocking {
        val usageData = NetworkUsageData(START_TIME, END_TIME, DataUnit.MEBIBYTES.toBytes(1))
        repository.stub {
            on { loadFirstCycle() } doReturn usageData
        }

        controller.onViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))

        waitUntil { preference.summary?.contains("1.00 MB") == true }
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
        const val START_TIME = 10L
        const val END_TIME = 30L
    }
}
