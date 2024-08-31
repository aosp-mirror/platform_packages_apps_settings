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

package com.android.settings.network.telephony.euicc

import android.content.Context
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class EuiccRepositoryTest {

    private val mockEuiccManager = mock<EuiccManager> { on { isEnabled } doReturn true }

    private val mockTelephonyManager =
        mock<TelephonyManager> {
            on { activeModemCount } doReturn 1
            on { getNetworkCountryIso(any()) } doReturn COUNTRY_CODE
        }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(EuiccManager::class.java) } doReturn mockEuiccManager
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        }

    private val resources =
        spy(context.resources) { on { getBoolean(R.bool.config_show_sim_info) } doReturn true }

    private var euiccProvisioned = false

    private val repository =
        EuiccRepository(
            context,
            isEuiccProvisioned = { euiccProvisioned },
            isDevelopmentSettingsEnabled = { false },
        )

    @Before
    fun setUp() {
        context.stub { on { resources } doReturn resources }
    }

    @Test
    fun showEuiccSettings_noSim_returnFalse() {
        resources.stub { on { getBoolean(R.bool.config_show_sim_info) } doReturn false }

        val showEuiccSettings = repository.showEuiccSettings()

        assertThat(showEuiccSettings).isFalse()
    }

    @Test
    fun showEuiccSettings_euiccDisabled_returnFalse() {
        mockEuiccManager.stub { on { isEnabled } doReturn false }

        val showEuiccSettings = repository.showEuiccSettings()

        assertThat(showEuiccSettings).isFalse()
    }

    @Test
    fun showEuiccSettings_euiccProvisioned_returnTrue() {
        euiccProvisioned = true

        val showEuiccSettings = repository.showEuiccSettings()

        assertThat(showEuiccSettings).isTrue()
    }

    @Test
    fun showEuiccSettings_countryNotSupported_returnFalse() {
        mockEuiccManager.stub { on { isSupportedCountry(COUNTRY_CODE) } doReturn false }

        val showEuiccSettings = repository.showEuiccSettings()

        assertThat(showEuiccSettings).isFalse()
    }

    @Test
    fun showEuiccSettings_countrySupported_returnTrue() {
        mockEuiccManager.stub { on { isSupportedCountry(COUNTRY_CODE) } doReturn true }

        val showEuiccSettings = repository.showEuiccSettings()

        assertThat(showEuiccSettings).isTrue()
    }

    private companion object {
        const val COUNTRY_CODE = "us"
    }
}
