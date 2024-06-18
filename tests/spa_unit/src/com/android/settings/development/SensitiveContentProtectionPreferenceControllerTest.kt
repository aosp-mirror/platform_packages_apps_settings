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
package com.android.settings.development

import android.content.Context
import android.permission.flags.Flags.FLAG_SENSITIVE_NOTIFICATION_APP_PROTECTION
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import android.provider.Settings.Global.DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS
import android.view.flags.Flags.FLAG_SENSITIVE_CONTENT_APP_PROTECTION
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.notification.Flags.FLAG_SCREENSHARE_NOTIFICATION_HIDING
import com.android.settings.development.SensitiveContentProtectionPreferenceController.Companion.SETTING_VALUE_OFF
import com.android.settings.development.SensitiveContentProtectionPreferenceController.Companion.SETTING_VALUE_ON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class SensitiveContentProtectionPreferenceControllerTest {
    @get:Rule
    val mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule
    val mocks = MockitoJUnit.rule()

    @Mock
    private lateinit var preference: SwitchPreference

    @Mock
    private lateinit var screen: PreferenceScreen

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var controller: SensitiveContentProtectionPreferenceController

    @Before
    fun setUp() {
        controller = SensitiveContentProtectionPreferenceController(context)
        whenever(screen.findPreference<Preference>(controller.getPreferenceKey()))
            .thenReturn(preference)
        controller.displayPreference(screen)
    }

    @Test
    fun onPreferenceChange_settingEnabled_shouldDisableSensitiveContentProtection() {
        controller.onPreferenceChange(preference, true /* new value */)
        val mode = Settings.Global.getInt(
            context.contentResolver,
            DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            -1 /* default */
        )

        assertEquals(mode, SETTING_VALUE_ON)
    }

    @Test
    fun onPreferenceChange_settingDisabled_shouldEnableSensitiveContentProtection() {
        controller.onPreferenceChange(preference, false /* new value */)
        val mode = Settings.Global.getInt(
            context.contentResolver,
            DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            -1 /* default */
        )

        assertEquals(mode, SETTING_VALUE_OFF)
    }

    @Test
    fun updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(
            context.contentResolver,
            DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            SETTING_VALUE_ON
        )
        controller.updateState(preference)

        verify(preference).isChecked = true
    }

    @Test
    fun updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(
            context.contentResolver,
            DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            SETTING_VALUE_OFF
        )
        controller.updateState(preference)

        verify(preference).isChecked = false
    }

    @Test
    fun onDeveloperOptionsSwitchDisabled_preferenceShouldBeDisabled() {
        controller.onDeveloperOptionsSwitchDisabled()
        val mode = Settings.Global.getInt(
            context.contentResolver,
            DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            -1 /* default */
        )

        assertEquals(mode, SETTING_VALUE_OFF)
        verify(preference).isChecked = false
        verify(preference).isEnabled = false
    }

    @Test
    @RequiresFlagsDisabled(
        FLAG_SENSITIVE_NOTIFICATION_APP_PROTECTION,
        FLAG_SCREENSHARE_NOTIFICATION_HIDING,
        FLAG_SENSITIVE_CONTENT_APP_PROTECTION)
    fun isAvailable_flagsDisabled_returnFalse() {
        assertFalse(controller.isAvailable)
    }

    @Test
    @RequiresFlagsEnabled(FLAG_SENSITIVE_NOTIFICATION_APP_PROTECTION)
    fun isAvailable_sensitiveNotificationAppProtectionEnabled_returnTrue() {
        assertTrue(controller.isAvailable)
    }

    @Test
    @RequiresFlagsEnabled(FLAG_SCREENSHARE_NOTIFICATION_HIDING)
    fun isAvailable_screenshareNotificationHidingEnabled_returnTrue() {
        assertTrue(controller.isAvailable)
    }

    @Test
    @RequiresFlagsEnabled(FLAG_SENSITIVE_CONTENT_APP_PROTECTION)
    fun isAvailable_screenshareSensitiveContentHidingEnabled_returnTrue() {
        assertTrue(controller.isAvailable)
    }
}
