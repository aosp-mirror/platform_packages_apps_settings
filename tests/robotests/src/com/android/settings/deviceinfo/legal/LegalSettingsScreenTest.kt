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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.text.TextUtils
import androidx.test.core.app.ApplicationProvider
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegalSettingsScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val legalSettingsScreen = LegalSettingsScreen()

    @Test
    fun screenKey_exist() {
        assertThat(TextUtils.equals(legalSettingsScreen.key, LegalSettingsScreen.KEY)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_LEGAL_INFORMATION)
    fun isFlagEnabled_returnTrue() {
        assertThat(legalSettingsScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_LEGAL_INFORMATION)
    fun isFlagDisabled_returnTrue() {
        assertThat(legalSettingsScreen.isFlagEnabled(context)).isFalse()
    }
}
