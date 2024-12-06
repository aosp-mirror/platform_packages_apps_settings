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

package com.android.settings.network

import android.content.Context
import android.net.TetheringManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.TetherUtil
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.mock
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class TetherPreferenceControllerTest {
    private lateinit var mockSession: MockitoSession

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockTetheredRepository =
        mock<TetheredRepository> { on { tetheredTypesFlow() }.thenReturn(flowOf(emptySet())) }

    private val controller = TetherPreferenceController(context, TEST_KEY, mockTetheredRepository)

    private val preference = PreferenceCategory(context).apply { key = TEST_KEY }

    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(TetherUtil::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()

        ExtendedMockito.doReturn(true).`when` { TetherUtil.isTetherAvailable(context) }

        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun getAvailabilityStatus_alwaysReturnAvailable() {
        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun onViewCreated_whenTetherAvailable() = runBlocking {
        ExtendedMockito.doReturn(true).`when` { TetherUtil.isTetherAvailable(context) }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isTrue()
    }

    @Test
    fun onViewCreated_whenTetherNotAvailable() = runBlocking {
        ExtendedMockito.doReturn(false).`when` { TetherUtil.isTetherAvailable(context) }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun getSummaryResId_bothWifiAndBluetoothOn() {
        val summaryResId = controller.getSummaryResId(
            setOf(TetheringManager.TETHERING_WIFI, TetheringManager.TETHERING_BLUETOOTH)
        )

        assertThat(summaryResId).isEqualTo(R.string.tether_settings_summary_hotspot_on_tether_on)
    }

    @Test
    fun getSummaryResId_onlyWifiHotspotOn() {
        val summaryResId = controller.getSummaryResId(setOf(TetheringManager.TETHERING_WIFI))

        assertThat(summaryResId).isEqualTo(R.string.tether_settings_summary_hotspot_on_tether_off)
    }

    @Test
    fun getSummaryResId_onlyBluetoothTetheringOn() {
        val summaryResId = controller.getSummaryResId(setOf(TetheringManager.TETHERING_BLUETOOTH))

        assertThat(summaryResId).isEqualTo(R.string.tether_settings_summary_hotspot_off_tether_on)
    }

    @Test
    fun getSummaryResId_allOff() {
        val summaryResId = controller.getSummaryResId(emptySet())

        assertThat(summaryResId).isEqualTo(R.string.tether_preference_summary_off)
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}
