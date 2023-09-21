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
import android.net.NetworkTemplate
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.lib.BillingCycleRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class BillingCyclePreferenceTest {

    private val mockBillingCycleRepository = mock<BillingCycleRepository> {
        on { isModifiable(SUB_ID) } doReturn false
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preference = BillingCyclePreference(context, null, mockBillingCycleRepository)

    @Test
    fun isEnabled_initialState() {
        val enabled = preference.isEnabled

        assertThat(enabled).isTrue()
    }

    @Test
    fun isEnabled_afterSetTemplate_updated() {
        preference.setTemplate(mock<NetworkTemplate>(), SUB_ID)

        val enabled = preference.isEnabled

        assertThat(enabled).isFalse()
    }

    private companion object {
        const val SUB_ID = 1
    }
}
