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
import android.net.wifi.WifiManager
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class AdaptiveConnectivityTogglePreferenceTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()

    private val appContext: Context = spy(ApplicationProvider.getApplicationContext()){}

    private val mockWifiManager: WifiManager = mock()

    private val adaptiveConnectivityTogglePreference = AdaptiveConnectivityTogglePreference()

    @Before
    fun setUp() {
        whenever(appContext.getSystemService(WifiManager::class.java)).thenReturn(mockWifiManager)
    }

    @Test
    fun setChecked_withTrue_shouldUpdateSetting() {
        Settings.Secure.putInt(
            appContext.contentResolver,
            Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 0
        )

        val mainSwitchPreference = getMainSwitchPreferenceCompat().apply { performClick() }

        assertThat(mainSwitchPreference.isChecked).isTrue()
        verify(mockWifiManager, atLeastOnce()).setWifiScoringEnabled(true)
    }

    @Test
    fun setChecked_withFalse_shouldUpdateSetting() {
        Settings.Secure.putInt(
            appContext.contentResolver,
            Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1
        )

        val mainSwitchPreference = getMainSwitchPreferenceCompat().apply { performClick() }

        assertThat(mainSwitchPreference.isChecked).isFalse()
        verify(mockWifiManager).setWifiScoringEnabled(false)
    }

    private fun getMainSwitchPreferenceCompat(): MainSwitchPreference =
        adaptiveConnectivityTogglePreference.createAndBindWidget(appContext)
}
// LINT.ThenChange(AdaptiveConnectivityTogglePreferenceControllerTest.java)
