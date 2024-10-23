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
package com.android.settings.network

import android.content.ContextWrapper
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class NetworkProviderScreenTest : CatalystScreenTestCase() {
    override val preferenceScreenCreator = NetworkProviderScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_INTERNET_SETTINGS

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(NetworkProviderScreen.KEY)
    }

    @Test
    fun isAvailable_showInternetSettings_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_notShowInternetSettings_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    override fun migration() {}
}
