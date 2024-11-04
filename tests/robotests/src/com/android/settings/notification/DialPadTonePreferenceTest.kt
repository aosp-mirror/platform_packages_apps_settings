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
package com.android.settings.notification

import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings.System.DTMF_TONE_WHEN_DIALING
import android.telephony.TelephonyManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class DialPadTonePreferenceTest {
    private var telephonyManager: TelephonyManager? = null

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.TELEPHONY_SERVICE -> telephonyManager
                    else -> super.getSystemService(name)
                }
        }

    private val dialPadTonePreference = DialPadTonePreference()

    @Test
    fun isAvailable_voiceCapable_shouldReturnTrue() {
        telephonyManager = mock { on { isVoiceCapable } doReturn true }

        assertThat(dialPadTonePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noVoicCapable_shouldReturnFalse() {
        telephonyManager = mock { on { isVoiceCapable } doReturn false }

        assertThat(dialPadTonePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun performClick_shouldPreferenceChangeToChecked() {
        enableDialPadTone(false)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun performClick_shouldPreferenceChangeToUnchecked() {
        enableDialPadTone(true)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun dialToneEnabled_shouldCheckedPreference() {
        enableDialPadTone(true)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun dialToneDisabled_shouldUncheckedPreference() {
        enableDialPadTone(false)

        assertThat(getSwitchPreference().isChecked).isFalse()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        dialPadTonePreference.createAndBindWidget(context)

    private fun enableDialPadTone(enabled: Boolean) =
        SettingsSystemStore.get(context).setBoolean(DTMF_TONE_WHEN_DIALING, enabled)
}
// LINT.ThenChange(DialPadTonePreferenceControllerTest.java)
