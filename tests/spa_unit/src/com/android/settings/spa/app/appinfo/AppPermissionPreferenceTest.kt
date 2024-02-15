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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppPermissionPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivityAsUser(any(), any())
    }

    @Test
    fun title_display() {
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.permissions_label))
            .assertIsDisplayed()
    }

    @Test
    fun whenClick_startActivity() {
        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intent = argumentCaptor {
            verify(context).startActivityAsUser(capture(), eq(APP.userHandle))
        }.firstValue
        assertThat(intent.action).isEqualTo(Intent.ACTION_MANAGE_APP_PERMISSIONS)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(intent.getBooleanExtra(EXTRA_HIDE_INFO_BUTTON, false)).isEqualTo(true)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppPermissionPreference(
                    app = APP,
                    summaryFlow = flowOf(
                        AppPermissionSummaryState(summary = SUMMARY, enabled = true)
                    ),
                )
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val SUMMARY = "Summary"
        private const val EXTRA_HIDE_INFO_BUTTON = "hideInfoButton"

        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}
