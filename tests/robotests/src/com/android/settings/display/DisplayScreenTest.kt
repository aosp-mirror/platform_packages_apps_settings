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
package com.android.settings.display

import android.content.ContextWrapper
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.widget.LockPatternUtils
import com.android.settings.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.SystemProperty
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class DisplayScreenTest : CatalystScreenTestCase() {

    override val preferenceScreenCreator = DisplayScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_DISPLAY_SETTINGS_SCREEN

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(DisplayScreen.KEY)
    }

    @Test
    fun isAvailable_configTrue_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configFalse_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    override fun migration() {
        // avoid UnsupportedOperationException when getDisplay from context
        SystemProperty("robolectric.createActivityContexts", "true").use {
            val lockPatternUtils =
                mock<LockPatternUtils> { on { isSecure(anyInt()) } doReturn true }
            FakeFeatureFactory.setupForTest().securityFeatureProvider.stub {
                on { getLockPatternUtils(any()) } doReturn lockPatternUtils
            }

            super.migration()
        }
    }
}
