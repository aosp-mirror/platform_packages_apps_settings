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
package com.android.settings.deviceinfo.legal

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class WallpaperAttributionsPreferenceTest {
    private val mockResources = mock<Resources>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = mockResources
        }

    private val wallpaperAttributionsPreference = WallpaperAttributionsPreference()

    @Test
    fun isAvailable_configTrue_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(wallpaperAttributionsPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configFalse_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(wallpaperAttributionsPreference.isAvailable(context)).isFalse()
    }
}
// LINT.ThenChange(WallpaperAttributionsPreferenceControllerTest.java)
