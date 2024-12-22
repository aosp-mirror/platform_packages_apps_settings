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

package com.android.settings.spa.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.SpaSearchLanding.BundleValue
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingFragment
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpaSearchLandingKeyExtTest {

    @Test
    fun encodeToString_thenDecode_shouldDecodeCorrectly() {
        val encoded = KEY.encodeToString()

        val decoded = decodeToSpaSearchLandingKey(encoded)

        assertThat(decoded).isEqualTo(KEY)
    }

    @Test
    fun decodeToSpaSearchLandingKey_badString_shouldReturnNull() {
        val decoded = decodeToSpaSearchLandingKey("bad")

        assertThat(decoded).isNull()
    }

    private companion object {
        val KEY: SpaSearchLandingKey =
            SpaSearchLandingKey.newBuilder()
                .setFragment(
                    SpaSearchLandingFragment.newBuilder()
                        .setFragmentName("Destination")
                        .setPreferenceKey("preference_key")
                        .putArguments(
                            "argument_key", BundleValue.newBuilder().setIntValue(123).build()))
                .build()
    }
}
