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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.datausage.lib.BillingCycleRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class BillingCyclePreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockBillingCycleRepository = mock<BillingCycleRepository>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preference = BillingCyclePreference(context, null, mockBillingCycleRepository)

    @Test
    fun setTemplate_titleDisplayed() {
        setTemplate()

        composeTestRule.onNodeWithText(context.getString(R.string.billing_cycle))
            .assertIsDisplayed()
    }

    @Test
    fun setTemplate_modifiable_enabled() {
        mockBillingCycleRepository.stub {
            on { isModifiable(SUB_ID) } doReturn true
        }

        setTemplate()

        composeTestRule.onNodeWithText(context.getString(R.string.billing_cycle)).assertIsEnabled()
    }

    @Test
    fun setTemplate_notModifiable_notEnabled() {
        mockBillingCycleRepository.stub {
            on { isModifiable(SUB_ID) } doReturn false
        }

        setTemplate()

        composeTestRule.onNodeWithText(context.getString(R.string.billing_cycle))
            .assertIsNotEnabled()
    }

    private fun setTemplate() {
        preference.setTemplate(mock<NetworkTemplate>(), SUB_ID)
        composeTestRule.setContent {
            preference.Content()
        }
    }

    private companion object {
        const val SUB_ID = 1
    }
}
