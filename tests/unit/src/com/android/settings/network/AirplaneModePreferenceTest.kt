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

import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_LEANBACK
import android.content.res.Resources
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.DrawableRes
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
open class AirplaneModePreferenceTest {

    private val mockResources = mock<Resources>()
    private val mockPackageManager = mock<PackageManager>()
    private var mockTelephonyManager = mock<TelephonyManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = mockResources

            override fun getPackageManager(): PackageManager = mockPackageManager

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    else -> super.getSystemService(name)
                }
        }

    private var airplaneModePreference =
        object : AirplaneModePreference() {
            // TODO: Remove override
            override val icon: Int
                @DrawableRes get() = 0
        }

    @Test
    fun isAvailable_hasConfigAndNoFeatureLeanback_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_LEANBACK) } doReturn false }

        assertThat(airplaneModePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noConfig_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_LEANBACK) } doReturn false }

        assertThat(airplaneModePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasFeatureLeanback_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_LEANBACK) } doReturn true }

        assertThat(airplaneModePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun getValue_defaultOn_returnOn() {
        SettingsGlobalStore.get(context).setInt(Settings.Global.AIRPLANE_MODE_ON, 1)

        val getValue =
            airplaneModePreference
                .storage(context)
                .getValue(AirplaneModePreference.KEY, Boolean::class.javaObjectType)

        assertThat(getValue).isTrue()
    }

    @Test
    fun getValue_defaultOff_returnOff() {
        SettingsGlobalStore.get(context).setInt(Settings.Global.AIRPLANE_MODE_ON, 0)

        val getValue =
            airplaneModePreference
                .storage(context)
                .getValue(AirplaneModePreference.KEY, Boolean::class.javaObjectType)

        assertThat(getValue).isFalse()
    }

    @Test
    fun performClick_defaultOn_checkedIsFalse() {
        SettingsGlobalStore.get(context).setInt(Settings.Global.AIRPLANE_MODE_ON, 1)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun performClick_defaultOff_checkedIsTrue() {
        SettingsGlobalStore.get(context).setInt(Settings.Global.AIRPLANE_MODE_ON, 0)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isTrue()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        airplaneModePreference.createAndBindWidget(context)
}
