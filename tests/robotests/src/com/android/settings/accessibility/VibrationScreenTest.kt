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
package com.android.settings.accessibility

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.R
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class VibrationScreenTest : CatalystScreenTestCase() {
    private lateinit var vibrator: Vibrator

    private val resourcesSpy: Resources =
        spy((ApplicationProvider.getApplicationContext() as Context).resources)

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(Vibrator::class.java) -> vibrator
                    else -> super.getSystemService(name)
                }
            override fun getResources(): Resources = resourcesSpy
        }

    override val preferenceScreenCreator = VibrationScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(VibrationScreen.KEY)
    }

    @Test
    fun isAvailable_noVibrator_unavailable() {
        vibrator = mock { on { hasVibrator() } doReturn false }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndMultipleIntensityLevels_unavailable() {
        vibrator = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 3
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndSingleIntensityLevel_available() {
        vibrator = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }
}
// LINT.ThenChange(VibrationPreferenceControllerTest.java)
