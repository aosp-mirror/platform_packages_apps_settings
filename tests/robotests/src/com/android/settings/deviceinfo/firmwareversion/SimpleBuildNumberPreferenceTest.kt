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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.Build
import android.text.BidiFormatter
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class SimpleBuildNumberPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val simpleBuildNumberPreference = SimpleBuildNumberPreference()

    @Test
    fun isIndexable() {
        assertThat(simpleBuildNumberPreference.isIndexable(context)).isFalse()
    }

    @Test
    fun getSummary_ltr() {
        context.resources.configuration.setLayoutDirection(Locale.ENGLISH)
        assertThat(simpleBuildNumberPreference.getSummary(context))
            .isEqualTo(BidiFormatter.getInstance(false).unicodeWrap(Build.DISPLAY))
    }

    @Test
    fun getSummary_rtl() {
        context.resources.configuration.setLayoutDirection(Locale("ar"))
        assertThat(simpleBuildNumberPreference.getSummary(context))
            .isEqualTo(BidiFormatter.getInstance(true).unicodeWrap(Build.DISPLAY))
    }
}
// LINT.ThenChange(SimpleBuildNumberPreferenceControllerTest.java)
