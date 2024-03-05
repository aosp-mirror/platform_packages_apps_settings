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

package com.android.settings.wifi

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.wifi.WifiDialog2.WifiDialog2Listener
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@Ignore
@RunWith(AndroidJUnit4::class)
class WifiDialog2Test {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockWifiEntry: WifiEntry

    private val listener = object : WifiDialog2Listener {}

    @Test
    fun constructor_usesDefaultTheme() {
        activityScenarioRule.scenario.onActivity { activity ->
            val wifiDialog2 = WifiDialog2(
                context = activity,
                listener = listener,
                wifiEntry = mockWifiEntry,
                mode = WifiConfigUiBase2.MODE_CONNECT,
                style = 0,
                hideSubmitButton = false
            )

            val modal = WifiDialog2(
                context = activity,
                listener = listener,
                wifiEntry = mockWifiEntry,
                mode = WifiConfigUiBase2.MODE_CONNECT,
            )

            assertThat(modal.context.themeResId).isEqualTo(wifiDialog2.context.themeResId)
        }
    }

    @Test
    fun constructor_whenSetTheme_shouldBeCustomizedTheme() {
        activityScenarioRule.scenario.onActivity { activity ->
            val wifiDialog2 = WifiDialog2(
                context = activity,
                listener = listener,
                wifiEntry = mockWifiEntry,
                mode = WifiConfigUiBase2.MODE_CONNECT,
                style = R.style.SuwAlertDialogThemeCompat_Light,
                hideSubmitButton = false,
            )

            val modal = WifiDialog2(
                context = activity,
                listener = listener,
                wifiEntry = mockWifiEntry,
                mode = WifiConfigUiBase2.MODE_CONNECT,
                style = R.style.SuwAlertDialogThemeCompat_Light,
            )

            assertThat(modal.context.themeResId).isEqualTo(wifiDialog2.context.themeResId)
        }
    }
}
