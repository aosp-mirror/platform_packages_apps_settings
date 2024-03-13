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

package com.android.settings.wifi.details2

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.cert.X509Certificate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class CertificateDetailsPreferenceControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockCertX509 = mock<X509Certificate> {}

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }

    private val controller = CertificateDetailsPreferenceController(context, TEST_KEY)

    @Before
    fun setUp() {
        controller.certificateAliases = MOCK_CA
        controller.certX509 = mockCertX509
    }

    @Test
    fun title_isDisplayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(context.getString(com.android.internal.R.string.ssl_certificate))
            .assertIsDisplayed()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val MOCK_CA = "mock_ca"
    }
}