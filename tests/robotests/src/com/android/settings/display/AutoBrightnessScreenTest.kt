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

import android.content.ContextWrapper
import android.content.res.Resources
import android.provider.Settings
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.view.LayoutInflater
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import com.android.settingslib.widget.theme.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class AutoBrightnessScreenTest {

    private val preferenceScreenCreator = AutoBrightnessScreen()

    private var mockResources: Resources? = null

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = mockResources ?: super.getResources()
        }

    @Test
    fun switchClick_defaultScreenBrightnessModeTurnOffAuto_returnTrue() {
        setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_MANUAL)
        val preference = getPrimarySwitchPreference()

        assertThat(preference.switch.isChecked).isFalse()

        preference.switch.performClick()

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun switchClick_defaultScreenBrightnessModeTurnOnAuto_returnFalse() {
        setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
        val preference = getPrimarySwitchPreference()

        assertThat(preference.switch.isChecked).isTrue()

        preference.switch.performClick()

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun setChecked_updatesCorrectly() {
        val preference = getPrimarySwitchPreference()

        preference.isChecked = true

        assertThat(preference.switch.isChecked).isTrue()

        preference.isChecked = false

        assertThat(preference.switch.isChecked).isFalse()
    }

    @Test
    fun isChecked_defaultScreenBrightnessModeTurnOffAuto_returnFalse() {
        setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_MANUAL)

        val preference = getPrimarySwitchPreference()

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun isChecked_defaultScreenBrightnessModeTurnOffAuto_returnTrue() {
        setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

        val preference = getPrimarySwitchPreference()

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun isAvailable_configTrueSet_shouldReturnTrue() {
        mockResources = mock { on { getBoolean(any()) } doReturn true }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configFalseSet_shouldReturnFalse() {
        mockResources = mock { on { getBoolean(any()) } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    private fun getPrimarySwitchPreference() =
        preferenceScreenCreator.createAndBindWidget<PrimarySwitchPreference>(context).also {
            val holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(context).inflate(getResId(), /* root= */ null)
                    )
                    .apply { findViewById(androidx.preference.R.id.switchWidget) }
            it.onBindViewHolder(holder)
        }

    private fun setScreenBrightnessMode(value: Int) =
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            value,
        )

    private fun getResId() =
        when {
            isExpressiveTheme(context) -> R.layout.settingslib_expressive_preference_switch
            else -> androidx.preference.R.layout.preference_widget_switch_compat
        }
}
// LINT.ThenChange(AutoBrightnessPreferenceControllerTest.java)
