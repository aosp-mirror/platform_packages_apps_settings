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

package com.android.settings.wifi

import android.content.ContextWrapper
import android.net.wifi.WifiManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiSwitchPreferenceTest {

    private val mockWifiManager = mock<WifiManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    else -> super.getSystemService(name)
                }
        }

    private val wifiSwitchPreference = WifiSwitchPreference()

    @Test
    fun getValue_defaultOn_returnOn() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn true }

        val getValue = wifiSwitchPreference.storage(context).getBoolean(WifiSwitchPreference.KEY)

        assertThat(getValue).isTrue()
    }

    @Test
    fun getValue_defaultOff_returnOff() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn false }

        val getValue = wifiSwitchPreference.storage(context).getBoolean(WifiSwitchPreference.KEY)

        assertThat(getValue).isFalse()
    }

    @Test
    fun performClick_defaultOn_checkedIsFalse() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn true }

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun performClick_defaultOff_checkedIsTrue() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn false }

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isTrue()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        wifiSwitchPreference.createAndBindWidget(context)
}
