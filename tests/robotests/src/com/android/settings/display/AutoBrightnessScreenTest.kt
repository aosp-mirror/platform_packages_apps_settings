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
import android.provider.Settings
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.view.LayoutInflater
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import com.android.settingslib.widget.theme.R
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
@Ignore("robolectric runtime")
class AutoBrightnessScreenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preferenceScreenCreator = AutoBrightnessScreen()

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
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_automatic_brightness_available,
            true,
        )

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configFalseSet_shouldReturnFalse() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_automatic_brightness_available,
            false,
        )

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    private fun getPrimarySwitchPreference(): PrimarySwitchPreference =
        preferenceScreenCreator.run {
            val preference = createWidget(context)
            bind(preference, this)
            val holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(context).inflate(getResId(), /* root= */ null)
                    )
                    .apply { findViewById(androidx.preference.R.id.switchWidget) }
            preference.apply { onBindViewHolder(holder) }
        }

    private fun setScreenBrightnessMode(value: Int) =
        Settings.System.putInt(context.contentResolver, AutoBrightnessScreen.KEY, value)

    private fun getResId() =
        when {
            isExpressiveTheme(context) -> R.layout.settingslib_expressive_preference_switch
            else -> androidx.preference.R.layout.preference_widget_switch_compat
        }
}
// LINT.ThenChange(AutoBrightnessPreferenceControllerTest.java)
