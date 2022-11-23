/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.pm.PackageManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.applications.AppLocaleUtil
import com.android.settings.applications.appinfo.AppLocaleDetails
import com.android.settings.localepicker.AppLocalePickerActivity
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.eq
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppLocalePreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        mockSession = mockitoSession()
            .initMocks(this)
            .mockStatic(AppLocaleUtil::class.java)
            .mockStatic(AppLocaleDetails::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(AppLocaleUtil.canDisplayLocaleUi(any(), ArgumentMatchers.eq(APP), any()))
                .thenReturn(true)
        whenever(AppLocaleDetails.getSummary(any(), ArgumentMatchers.eq(APP))).thenReturn(SUMMARY)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenCanNotDisplayLocalUi_notDisplayed() {
        whenever(AppLocaleUtil.canDisplayLocaleUi(any(), ArgumentMatchers.eq(APP), any()))
                .thenReturn(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenCanDisplayLocalUi_displayed() {
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.app_locale_preference_title))
            .assertIsDisplayed()
        composeTestRule.waitUntilExists(hasText(SUMMARY))
    }

    @Test
    fun whenCanDisplayLocalUi_click_startActivity() {
        doNothing().`when`(context).startActivityAsUser(any(), any())

        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivityAsUser(intentCaptor.capture(), eq(APP.userHandle))
        val intent = intentCaptor.value
        assertThat(intent.component?.className)
            .isEqualTo(AppLocalePickerActivity::class.qualifiedName)
        assertThat(intent.getIntExtra(AppInfoBase.ARG_PACKAGE_UID, -1)).isEqualTo(UID)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppLocalePreference(APP)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "packageName"
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
        const val SUMMARY = "summary"
    }
}
