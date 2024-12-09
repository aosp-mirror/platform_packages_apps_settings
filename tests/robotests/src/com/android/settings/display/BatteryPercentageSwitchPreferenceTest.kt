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
package com.android.settings.display

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.provider.Settings
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.shadow.ShadowUtils
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowUtils::class])
// LINT.IfChange
class BatteryPercentageSwitchPreferenceTest {
    private val mockResources = mock<Resources>()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    private val batteryPercentageSwitchPreference = BatteryPercentageSwitchPreference()

    @After
    fun tearDown() {
        ShadowUtils.reset()
    }

    @Test
    fun isAvailable_noBatteryPresent_shouldReturnFalse() {
        ShadowUtils.setIsBatteryPresent(false)

        assertThat(batteryPercentageSwitchPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_batterySettingsAvailable_shouldReturnTrue() {
        ShadowUtils.setIsBatteryPresent(true)
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(batteryPercentageSwitchPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_batterySettingsUnavailable_shouldReturnFalse() {
        ShadowUtils.setIsBatteryPresent(true)
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(batteryPercentageSwitchPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun batteryPercentageEnabled_shouldSwitchPreferenceChecked() {
        showBatteryPercentage(true)

        val switchPreference = getSwitchPreferenceCompat()

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun batteryPercentageDisabled_shouldSwitchPreferenceUnChecked() {
        showBatteryPercentage(false)

        val switchPreference = getSwitchPreferenceCompat()

        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun click_defaultBatteryPercentageDisabled_shouldChangeToEnabled() {
        showBatteryPercentage(false)

        val switchPreference = getSwitchPreferenceCompat().apply { performClick() }

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun click_defaultBatteryPercentageEnabled_shouldChangeToDisabled() {
        showBatteryPercentage(true)

        val switchPreference = getSwitchPreferenceCompat().apply { performClick() }

        assertThat(switchPreference.isChecked).isFalse()
    }

    private fun getSwitchPreferenceCompat(): SwitchPreferenceCompat =
        batteryPercentageSwitchPreference.createAndBindWidget(context)

    private fun showBatteryPercentage(on: Boolean) =
        batteryPercentageSwitchPreference.storage(context).setBoolean(
                Settings.System.SHOW_BATTERY_PERCENT,
                on,
            )
}
// LINT.ThenChange(BatteryPercentagePreferenceControllerTest.java)
