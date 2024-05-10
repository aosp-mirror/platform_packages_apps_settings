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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.model.app.userId
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppForceStopButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageInfoPresenter: PackageInfoPresenter

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var devicePolicyManager: DevicePolicyManager

    private lateinit var appForceStopButton: AppForceStopButton

    @Before
    fun setUp() {
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.devicePolicyManager).thenReturn(devicePolicyManager)
        appForceStopButton = AppForceStopButton(packageInfoPresenter)
    }

    @Test
    fun getActionButton() {
    }

    @Test
    fun getActionButton_isActiveAdmin_buttonDisabled() {
        val app = createApp()
        whenever(devicePolicyManager.packageHasActiveAdmins(PACKAGE_NAME, app.userId))
            .thenReturn(true)

        val actionButton = setForceStopButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_isUninstallInQueue_buttonDisabled() {
        val app = createApp()
        whenever(devicePolicyManager.isUninstallInQueue(PACKAGE_NAME)).thenReturn(true)

        val actionButton = setForceStopButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_isStopped_buttonDisabled() {
        val app = createApp {
            flags = ApplicationInfo.FLAG_STOPPED
        }

        val actionButton = setForceStopButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_regularApp_buttonEnabled() {
        val app = createApp()

        val actionButton = setForceStopButton(app)

        assertThat(actionButton.enabled).isTrue()
    }

    private fun setForceStopButton(app: ApplicationInfo): ActionButton {
        lateinit var actionButton: ActionButton
        composeTestRule.setContent {
            actionButton = appForceStopButton.getActionButton(app)
        }
        return actionButton
    }

    private fun createApp(builder: ApplicationInfo.() -> Unit = {}) =
        ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            enabled = true
        }.apply(builder)

    private companion object {
        const val PACKAGE_NAME = "package.name"
    }
}