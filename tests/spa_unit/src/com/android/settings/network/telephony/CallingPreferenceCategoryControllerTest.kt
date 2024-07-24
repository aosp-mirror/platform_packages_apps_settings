/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.preference.ComposePreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallingPreferenceCategoryControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preference = ComposePreference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val controller = CallingPreferenceCategoryController(context, TEST_KEY)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun updateChildVisible_singleChildVisible_categoryVisible() {
        controller.updateChildVisible(CHILD_A_KEY, true)

        assertThat(preference.isVisible).isTrue()
    }

    @Test
    fun updateChildVisible_singleChildNotVisible_categoryNotVisible() {
        controller.updateChildVisible(CHILD_A_KEY, false)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun updateChildVisible_oneChildVisible_categoryVisible() {
        controller.updateChildVisible(CHILD_A_KEY, true)
        controller.updateChildVisible(CHILD_B_KEY, false)

        assertThat(preference.isVisible).isTrue()
    }

    @Test
    fun updateChildVisible_nonChildNotVisible_categoryNotVisible() {
        controller.updateChildVisible(CHILD_A_KEY, false)
        controller.updateChildVisible(CHILD_B_KEY, false)

        assertThat(preference.isVisible).isFalse()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val CHILD_A_KEY = "a"
        const val CHILD_B_KEY = "b"
    }
}
