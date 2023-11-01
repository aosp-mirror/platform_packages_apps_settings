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

package com.android.settings.utils

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsGlobalBooleanDelegateTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getValue_setTrue_returnTrue() {
        Settings.Global.putInt(context.contentResolver, TEST_NAME, 1)

        val value by context.settingsGlobalBoolean(TEST_NAME)

        assertThat(value).isTrue()
    }

    @Test
    fun getValue_setFalse_returnFalse() {
        Settings.Global.putInt(context.contentResolver, TEST_NAME, 0)

        val value by context.settingsGlobalBoolean(TEST_NAME)

        assertThat(value).isFalse()
    }

    @Test
    fun setValue_setTrue_returnTrue() {
        var value by context.settingsGlobalBoolean(TEST_NAME)

        value = true

        assertThat(Settings.Global.getInt(context.contentResolver, TEST_NAME, 0)).isEqualTo(1)
    }

    @Test
    fun setValue_setFalse_returnFalse() {
        var value by context.settingsGlobalBoolean(TEST_NAME)

        value = false

        assertThat(Settings.Global.getInt(context.contentResolver, TEST_NAME, 1)).isEqualTo(0)
    }

    @Test
    fun observeSettingsGlobalBoolean_valueNotChanged() {
        var value by context.settingsGlobalBoolean(TEST_NAME)
        value = false
        var newValue: Boolean? = null

        context.observeSettingsGlobalBoolean(TEST_NAME, TestLifecycleOwner().lifecycle) {
            newValue = it
        }

        assertThat(newValue).isFalse()
    }

    @Test
    fun observeSettingsGlobalBoolean_valueChanged() {
        var value by context.settingsGlobalBoolean(TEST_NAME)
        value = false
        var newValue: Boolean? = null

        context.observeSettingsGlobalBoolean(TEST_NAME, TestLifecycleOwner().lifecycle) {
            newValue = it
        }
        value = true

        assertThat(newValue).isFalse()
    }

    private companion object {
        const val TEST_NAME = "test_boolean_delegate"
    }
}
