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
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.accessibility.RemoveAnimationsPreference.Companion.ANIMATION_ON_VALUE
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.preference.PreferenceScreenBindingHelper
import com.android.settingslib.preference.PreferenceScreenFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoveAnimationsPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private fun getRemoveAnimationsSwitchPreference(): SwitchPreferenceCompat =
        PreferenceScreenFactory(appContext).let {
            val preferenceScreen = it.inflate(R.xml.accessibility_color_and_motion)!!
            it.preferenceManager.setPreferences(preferenceScreen)
            PreferenceScreenBindingHelper.bind(preferenceScreen)
            preferenceScreen.findPreference(RemoveAnimationsPreference.KEY)!!
        }

    @Test
    fun animationOff_switchPreferenceIsChecked() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, false)

        assertThat(getRemoveAnimationsSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun animationOn_switchPreferenceIsNotChecked() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, true)

        assertThat(getRemoveAnimationsSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun oneAnimationValueOn_switchPreferenceIsNotChecked() {
        // Animation is disabled, except for one value.
        RemoveAnimationsPreference.setAnimationEnabled(appContext, false)
        SettingsGlobalStore.get(appContext)
            .setFloat(RemoveAnimationsPreference.getAnimationKeys()[0], ANIMATION_ON_VALUE)

        assertThat(getRemoveAnimationsSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun toggleOnSwitch_turnsOffAnimation() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, true)

        val switchPreference = getRemoveAnimationsSwitchPreference()
        assertThat(switchPreference.isChecked).isFalse()
        switchPreference.performClick()
        assertThat(switchPreference.isChecked).isTrue()

        assertThat(RemoveAnimationsPreference.isAnimationEnabled(appContext)).isFalse()
    }

    @Test
    fun toggleOffSwitch_turnsOnAnimation() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, false)

        val switchPreference = getRemoveAnimationsSwitchPreference()
        assertThat(switchPreference.isChecked).isTrue()
        switchPreference.performClick()
        assertThat(switchPreference.isChecked).isFalse()

        assertThat(RemoveAnimationsPreference.isAnimationEnabled(appContext)).isTrue()
    }
}
