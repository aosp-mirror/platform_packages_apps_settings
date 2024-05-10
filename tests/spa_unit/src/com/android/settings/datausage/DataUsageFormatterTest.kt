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

package com.android.settings.datausage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.DataUsageFormatter.getBytesDisplayUnit
import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataUsageFormatterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getUnitDisplayName_megaByte() {
        val displayName = context.resources.getBytesDisplayUnit(ONE_MEGA_BYTE_IN_BYTES)

        assertThat(displayName).isEqualTo("MB")
    }

    @Test
    fun getUnitDisplayName_gigaByte() {
        val displayName = context.resources.getBytesDisplayUnit(ONE_GIGA_BYTE_IN_BYTES)

        assertThat(displayName).isEqualTo("GB")
    }

    private companion object {
        const val ONE_MEGA_BYTE_IN_BYTES = 1024L * 1024
        const val ONE_GIGA_BYTE_IN_BYTES = 1024L * 1024 * 1024
    }
}