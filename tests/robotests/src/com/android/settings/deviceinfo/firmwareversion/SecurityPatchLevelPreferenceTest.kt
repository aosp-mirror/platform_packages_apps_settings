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
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class SecurityPatchLevelPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val securityPatchLevelPreference = SecurityPatchLevelPreference()

    @Test
    fun isAvailable_noPatch_unavailable() {
        setSecurityPatch("")
        assertThat(securityPatchLevelPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasPatch_available() {
        setSecurityPatch("foobar")
        assertThat(securityPatchLevelPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun getSummary_patchIsDate() {
        setSecurityPatch("2024-09-24")
        assertThat(securityPatchLevelPreference.getSummary(context)).isEqualTo("September 24, 2024")
    }

    @Test
    fun getSummary_patchIsNotDate() {
        setSecurityPatch("foobar")
        assertThat(securityPatchLevelPreference.getSummary(context)).isEqualTo("foobar")
    }

    private fun setSecurityPatch(patch: String) {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SECURITY_PATCH", patch)
    }
}
// LINT.ThenChange(SecurityPatchLevelPreferenceControllerTest.java)
