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

package com.android.settings.network

import android.content.Context
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.RestrictedPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class MobileNetworkSummaryControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preference = RestrictedPreference(context).apply { key = KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val mockMobileNetworkSummaryRepository = mock<MobileNetworkSummaryRepository>()
    private val airplaneModeOnFlow = MutableStateFlow(false)

    private val controller =
        MobileNetworkSummaryController(
            context = context,
            preferenceKey = KEY,
            repository = mockMobileNetworkSummaryRepository,
            airplaneModeOnFlow = airplaneModeOnFlow,
        )

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun onViewCreated_noSubscriptions(): Unit = runBlocking {
        mockMobileNetworkSummaryRepository.stub {
            on { subscriptionsStateFlow() } doReturn
                flowOf(MobileNetworkSummaryRepository.NoSubscriptions)
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isNull()
        assertThat(preference.isEnabled).isFalse()
        assertThat(preference.onPreferenceClickListener).isNull()
    }

    @Test
    fun onViewCreated_addNetwork(): Unit = runBlocking {
        mockMobileNetworkSummaryRepository.stub {
            on { subscriptionsStateFlow() } doReturn
                flowOf(MobileNetworkSummaryRepository.AddNetwork)
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary)
            .isEqualTo(context.getString(R.string.mobile_network_summary_add_a_network))
        assertThat(preference.isEnabled).isTrue()
        assertThat(preference.onPreferenceClickListener).isNotNull()
    }

    @Test
    fun onViewCreated_hasSubscriptions(): Unit = runBlocking {
        mockMobileNetworkSummaryRepository.stub {
            on { subscriptionsStateFlow() } doReturn
                flowOf(
                    MobileNetworkSummaryRepository.HasSubscriptions(
                        displayNames = listOf(DISPLAY_NAME_1, DISPLAY_NAME_2)
                    )
                )
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isEqualTo("$DISPLAY_NAME_1, $DISPLAY_NAME_2")
        assertThat(preference.isEnabled).isTrue()
        assertThat(preference.fragment).isNotNull()
    }

    @Test
    fun onViewCreated_addNetworkAndAirplaneModeOn(): Unit = runBlocking {
        mockMobileNetworkSummaryRepository.stub {
            on { subscriptionsStateFlow() } doReturn
                flowOf(MobileNetworkSummaryRepository.AddNetwork)
        }
        airplaneModeOnFlow.value = true

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun onViewCreated_hasSubscriptionsAndAirplaneModeOn(): Unit = runBlocking {
        mockMobileNetworkSummaryRepository.stub {
            on { subscriptionsStateFlow() } doReturn
                flowOf(
                    MobileNetworkSummaryRepository.HasSubscriptions(
                        displayNames = listOf(DISPLAY_NAME_1, DISPLAY_NAME_2)
                    )
                )
        }
        airplaneModeOnFlow.value = true

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isEnabled).isFalse()
    }


    private companion object {
        const val KEY = "test_key"
        const val DISPLAY_NAME_1 = "Display Name 1"
        const val DISPLAY_NAME_2 = "Display Name 2"
    }
}
