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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.model.app.userHandle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class TopBarAppLaunchButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageInfoPresenter: PackageInfoPresenter

    @Mock
    private lateinit var userPackageManager: PackageManager

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(packageInfoPresenter.userPackageManager).thenReturn(userPackageManager)
        val intent = Intent()
        whenever(userPackageManager.getLaunchIntentForPackage(PACKAGE_NAME)).thenReturn(intent)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun topBarAppLaunchButton_isDisplayed() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        setContent(app)

        composeTestRule.waitUntilExists(
            hasContentDescription(context.getString(R.string.launch_instant_app))
        )
    }

    @Test
    fun topBarAppLaunchButton_opensApp() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        setContent(app)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.launch_instant_app))
            .performClick()

        verify(context).startActivityAsUser(any(), eq(app.userHandle))
    }

    private fun setContent(app: ApplicationInfo) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                TopBarAppLaunchButton(packageInfoPresenter, app)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
    }
}
