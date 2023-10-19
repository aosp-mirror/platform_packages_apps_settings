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
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE
import com.android.settings.datausage.DataUsageUtils
import com.android.settings.datausage.lib.DataUsageLib
import com.android.settingslib.net.DataUsageController
import com.android.settingslib.net.DataUsageController.DataUsageInfo
import com.android.settingslib.spa.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DataUsagePreferenceControllerTest {

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var controller: DataUsagePreferenceController

    private val preference = Preference(context)

    @Mock
    private lateinit var networkTemplate: NetworkTemplate

    @Mock
    private lateinit var dataUsageController: DataUsageController

    @Mock
    private lateinit var preferenceScreen: PreferenceScreen

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
        ExtendedMockito.doReturn(networkTemplate)
            .`when` { DataUsageLib.getMobileTemplate(context, SUB_ID) }
        preference.key = TEST_KEY
        whenever(preferenceScreen.findPreference<Preference>(TEST_KEY)).thenReturn(preference)

        controller =
            DataUsagePreferenceController(context, TEST_KEY).apply {
                init(SUB_ID)
                displayPreference(preferenceScreen)
                dataUsageControllerFactory = { dataUsageController }
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
    fun handlePreferenceTreeClick_startActivity() = runTest {
        val usageInfo = DataUsageInfo().apply {
            usageLevel = DataUnit.MEBIBYTES.toBytes(1)
        }
        whenever(dataUsageController.getDataUsageInfo(networkTemplate)).thenReturn(usageInfo)
        doNothing().`when`(context).startActivity(any())
        controller.whenViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))
        waitUntil { preference.summary != null }

        controller.handlePreferenceTreeClick(preference)

        val captor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(captor.capture())
        val intent = captor.value
        assertThat(intent.action).isEqualTo(Settings.ACTION_MOBILE_DATA_USAGE)
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID, 0)).isEqualTo(SUB_ID)
    }

    @Test
    fun updateState_invalidSubId_disabled() = runTest {
        controller.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        controller.whenViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))

        waitUntil { !preference.isEnabled }
    }

    @Test
    fun updateState_noUsageData_shouldDisablePreference() = runTest {
        val usageInfo = DataUsageInfo()
        whenever(dataUsageController.getDataUsageInfo(networkTemplate)).thenReturn(usageInfo)

        controller.whenViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))

        waitUntil { !preference.isEnabled }
    }

    @Test
    fun updateState_shouldUseIecUnit() = runTest {
        val usageInfo = DataUsageInfo().apply {
            usageLevel = DataUnit.MEBIBYTES.toBytes(1)
        }
        whenever(dataUsageController.getDataUsageInfo(networkTemplate)).thenReturn(usageInfo)

        controller.whenViewCreated(TestLifecycleOwner(initialState = Lifecycle.State.STARTED))

        waitUntil { preference.summary?.contains("1.00 MB") == true }
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
    }
}
