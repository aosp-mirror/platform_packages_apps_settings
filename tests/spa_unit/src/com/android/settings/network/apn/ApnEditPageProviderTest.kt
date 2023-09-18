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

package com.android.settings.network.apn

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApnEditPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    val apnData = ApnData(name = "apn_name")

    @Test
    fun apnEditPageProvider_name() {
        Truth.assertThat(ApnEditPageProvider.name).isEqualTo("Apn")
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            ApnPage(apnData)
        }
        composeTestRule.onNodeWithText(context.getString(R.string.apn_edit)).assertIsDisplayed()
    }

    @Test
    fun name_displayed() {
        composeTestRule.setContent {
            ApnPage(apnData)
        }
        composeTestRule.onNodeWithText("apn_name", true).assertIsDisplayed()
    }
}