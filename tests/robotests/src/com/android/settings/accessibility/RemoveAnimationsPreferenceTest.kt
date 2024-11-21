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
import com.android.settings.accessibility.RemoveAnimationsPreference.Companion.ANIMATION_ON_VALUE
import com.android.settings.accessibility.RemoveAnimationsPreference.Companion.TOGGLE_ANIMATION_KEYS
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoveAnimationsPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val removeAnimationsPreference =
        RemoveAnimationsPreference()

    private fun getSwitchPreferenceCompat(): SwitchPreferenceCompat =
        removeAnimationsPreference.createAndBindWidget(appContext)

    @Test
    fun animationOff_switchPreferenceIsChecked() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, false)

        assertThat(getSwitchPreferenceCompat().isChecked).isTrue()
    }

    @Test
    fun animationOn_switchPreferenceIsNotChecked() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, true)

        assertThat(getSwitchPreferenceCompat().isChecked).isFalse()
    }

    @Test
    fun oneAnimationValueOn_switchPreferenceIsNotChecked() {
        // Animation is disabled, except for one value.
        RemoveAnimationsPreference.setAnimationEnabled(appContext, false)
        SettingsGlobalStore.get(appContext).setFloat(TOGGLE_ANIMATION_KEYS[0], ANIMATION_ON_VALUE)

        assertThat(getSwitchPreferenceCompat().isChecked).isFalse()
    }

    @Test
    fun storageSetTrue_turnsOffAnimation() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, true)

        storageSetValue(true)

        assertThat(RemoveAnimationsPreference.isAnimationEnabled(appContext)).isFalse()
    }

    @Test
    fun storageSetFalse_turnsOnAnimation() {
        RemoveAnimationsPreference.setAnimationEnabled(appContext, false)

        storageSetValue(false)

        assertThat(RemoveAnimationsPreference.isAnimationEnabled(appContext)).isTrue()
    }

    private fun storageSetValue(enabled: Boolean) = removeAnimationsPreference.storage(appContext)
        .setValue(RemoveAnimationsPreference.KEY, Boolean::class.javaObjectType, enabled)
}