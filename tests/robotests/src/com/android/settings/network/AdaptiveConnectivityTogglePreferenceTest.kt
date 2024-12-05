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
import android.content.ContextWrapper
import android.net.wifi.WifiManager
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class AdaptiveConnectivityTogglePreferenceTest {
    private val mockWifiManager = mock<WifiManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    else -> super.getSystemService(name)
                }
        }

    private val adaptiveConnectivityTogglePreference = AdaptiveConnectivityTogglePreference()

    @Test
    fun switchClick_defaultDisabled_returnFalse() {
        setAdaptiveConnectivityEnabled(false)

        assertThat(getMainSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun switchClick_defaultEnabled_returnTrue() {
        setAdaptiveConnectivityEnabled(true)

        assertThat(getMainSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun setChecked_defaultEnabled_updatesCorrectly() {
        val preference = getMainSwitchPreference()
        assertThat(preference.isChecked).isTrue()

        preference.performClick()

        assertThat(preference.isChecked).isFalse()

        preference.performClick()

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun storeSetTrue_wifiManagerSetWifiScoringEnabled() {
        setAdaptiveConnectivityEnabled(true)

        assertThat(getAdaptiveConnectivityEnabled()).isTrue()
        verify(mockWifiManager).setWifiScoringEnabled(true)
    }

    @Test
    fun storeSetFalse_wifiManagerSetWifiScoringDisabled() {
        setAdaptiveConnectivityEnabled(false)

        assertThat(getAdaptiveConnectivityEnabled()).isFalse()
        verify(mockWifiManager).setWifiScoringEnabled(false)
    }

    private fun getMainSwitchPreference(): MainSwitchPreference =
        adaptiveConnectivityTogglePreference.createAndBindWidget(context)

    private fun setAdaptiveConnectivityEnabled(enabled: Boolean) =
        adaptiveConnectivityTogglePreference
            .storage(context)
            .setValue(ADAPTIVE_CONNECTIVITY_ENABLED, Boolean::class.javaObjectType, enabled)

    private fun getAdaptiveConnectivityEnabled() =
        adaptiveConnectivityTogglePreference
            .storage(context)
            .getValue(ADAPTIVE_CONNECTIVITY_ENABLED, Boolean::class.javaObjectType)
}
// LINT.ThenChange(AdaptiveConnectivityTogglePreferenceControllerTest.java)
