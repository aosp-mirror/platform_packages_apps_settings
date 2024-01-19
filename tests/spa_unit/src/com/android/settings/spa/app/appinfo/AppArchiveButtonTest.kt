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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.widget.button.ActionButton
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
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
class AppArchiveButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val packageInfoPresenter = mock<PackageInfoPresenter>()

    private val userPackageManager = mock<PackageManager>()

    private val packageInstaller = mock<PackageInstaller>()

    private val isHibernationSwitchEnabledStateFlow = MutableStateFlow(true)

    private lateinit var appArchiveButton: AppArchiveButton

    @Before
    fun setUp() {
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(packageInfoPresenter.userPackageManager).thenReturn(userPackageManager)
        whenever(userPackageManager.packageInstaller).thenReturn(packageInstaller)
        whenever(userPackageManager.getApplicationLabel(any())).thenReturn(APP_LABEL)
        whenever(packageInfoPresenter.packageName).thenReturn(PACKAGE_NAME)
        appArchiveButton =
            AppArchiveButton(packageInfoPresenter, isHibernationSwitchEnabledStateFlow)
    }

    @Test
    fun appArchiveButton_whenIsArchived_isDisabled() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = true
        }
        whenever(userPackageManager.isAppArchivable(app.packageName)).thenReturn(true)

        val actionButton = setContent(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun appArchiveButton_whenIsNotAppArchivable_isDisabled() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = false
        }
        whenever(userPackageManager.isAppArchivable(app.packageName)).thenReturn(false)

        val actionButton = setContent(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun appArchiveButton_whenIsHibernationSwitchDisabled_isDisabled() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = false
            flags = ApplicationInfo.FLAG_INSTALLED
        }
        whenever(userPackageManager.isAppArchivable(app.packageName)).thenReturn(true)
        isHibernationSwitchEnabledStateFlow.value = false
        val enabledActionButton = setContent(app)

        assertThat(enabledActionButton.enabled).isFalse()
    }

    @Test
    fun appArchiveButton_displaysRightTextAndIcon() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = false
        }
        whenever(userPackageManager.isAppArchivable(app.packageName)).thenReturn(true)

        val actionButton = setContent(app)

        assertThat(actionButton.text).isEqualTo(context.getString(R.string.archive))
        assertThat(actionButton.imageVector).isEqualTo(Icons.Outlined.CloudUpload)
    }

    @Test
    fun appArchiveButton_clicked() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = false
        }
        whenever(userPackageManager.isAppArchivable(app.packageName)).thenReturn(true)

        val actionButton = setContent(app)
        actionButton.onClick()

        verify(packageInstaller).requestArchive(
            eq(PACKAGE_NAME),
            any()
        )
    }

    private fun setContent(app: ApplicationInfo): ActionButton {
        lateinit var actionButton: ActionButton
        composeTestRule.setContent {
            actionButton = appArchiveButton.getActionButton(app)
        }
        composeTestRule.delay()
        return actionButton
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val APP_LABEL = "App label"
    }
}
