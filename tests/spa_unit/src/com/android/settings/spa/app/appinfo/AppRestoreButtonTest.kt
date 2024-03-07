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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppRestoreButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val packageInfoPresenter = mock<PackageInfoPresenter>()

    private val userPackageManager = mock<PackageManager>()

    private val packageInstaller = mock<PackageInstaller>()

    private lateinit var appRestoreButton: AppRestoreButton

    @Before
    fun setUp() {
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(packageInfoPresenter.userPackageManager).thenReturn(userPackageManager)
        whenever(userPackageManager.packageInstaller).thenReturn(packageInstaller)
        whenever(packageInfoPresenter.packageName).thenReturn(PACKAGE_NAME)
        appRestoreButton = AppRestoreButton(packageInfoPresenter)
    }

    @Test
    fun appRestoreButton_whenIsNotArchived_isDisabled() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = false
        }

        val actionButton = setContent(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun appRestoreButton_whenIsArchived_isEnabled() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = true
        }

        val actionButton = setContent(app)

        assertThat(actionButton.enabled).isTrue()
    }

    @Test
    fun appRestoreButton_displaysRightTextAndIcon() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = false
        }

        val actionButton = setContent(app)

        assertThat(actionButton.text).isEqualTo(context.getString(R.string.restore))
        assertThat(actionButton.imageVector).isEqualTo(Icons.Outlined.CloudDownload)
    }

    @Test
    @UiThreadTest
    fun appRestoreButton_clicked() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = true
        }

        val actionButton = setContent(app)
        actionButton.onClick()

        verify(packageInstaller).requestUnarchive(
            eq(PACKAGE_NAME),
            any()
        )
    }

    private fun setContent(app: ApplicationInfo): ActionButton {
        lateinit var actionButton: ActionButton
        composeTestRule.setContent {
            actionButton = appRestoreButton.getActionButton(app)
        }
        return actionButton
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
    }
}
