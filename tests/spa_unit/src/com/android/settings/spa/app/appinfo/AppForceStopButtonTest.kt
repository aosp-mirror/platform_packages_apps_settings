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
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.model.app.userId
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppForceStopButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockPackageManager = mock<PackageManager>()

    private val mockDevicePolicyManager = mock<DevicePolicyManager>()

    private val mockUserManager = mock<UserManager> {
        on { getUserRestrictionSources(any(), any()) } doReturn emptyList()
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn mockPackageManager
        on { devicePolicyManager } doReturn mockDevicePolicyManager
        on { getSystemService(Context.DEVICE_POLICY_SERVICE) } doReturn mockDevicePolicyManager
        on { getSystemService(Context.USER_SERVICE) } doReturn mockUserManager
    }

    private val packageInfoPresenter = mock<PackageInfoPresenter> {
        on { context } doReturn context
    }

    private val appForceStopButton = AppForceStopButton(packageInfoPresenter)

    @Test
    fun getActionButton_isActiveAdmin_buttonDisabled() {
        val app = createApp()
        mockDevicePolicyManager.stub {
            on { packageHasActiveAdmins(PACKAGE_NAME, app.userId) } doReturn true
        }

        setForceStopButton(app)

        composeTestRule.onNodeWithText(context.getString(R.string.force_stop)).assertIsNotEnabled()
    }

    @Test
    fun getActionButton_isUninstallInQueue_buttonDisabled() {
        val app = createApp()
        mockDevicePolicyManager.stub {
            on { isUninstallInQueue(PACKAGE_NAME) } doReturn true
        }

        setForceStopButton(app)

        composeTestRule.onNodeWithText(context.getString(R.string.force_stop)).assertIsNotEnabled()
    }

    @Test
    fun getActionButton_isStopped_buttonDisabled() {
        val app = createApp {
            flags = ApplicationInfo.FLAG_STOPPED
        }

        setForceStopButton(app)

        composeTestRule.onNodeWithText(context.getString(R.string.force_stop)).assertIsNotEnabled()
    }

    @Test
    fun getActionButton_regularApp_buttonEnabled() {
        val app = createApp()

        setForceStopButton(app)

        composeTestRule.onNodeWithText(context.getString(R.string.force_stop)).assertIsEnabled()
    }

    @Test
    fun getAdminRestriction_packageNotProtected() {
        mockPackageManager.stub {
            on { isPackageStateProtected(PACKAGE_NAME, UserHandle.getUserId(UID)) } doReturn false
        }

        val admin = appForceStopButton.getAdminRestriction(createApp())

        assertThat(admin).isNull()
    }

    @Test
    fun getAdminRestriction_packageProtectedAndHaveOwner() {
        mockPackageManager.stub {
            on { isPackageStateProtected(PACKAGE_NAME, UserHandle.getUserId(UID)) } doReturn true
        }
        mockDevicePolicyManager.stub {
            on { deviceOwnerComponentOnAnyUser } doReturn DEVICE_OWNER
        }

        val admin = appForceStopButton.getAdminRestriction(createApp())!!

        assertThat(admin.component).isEqualTo(DEVICE_OWNER)
    }

    @Test
    fun getAdminRestriction_packageProtectedAndNotHaveOwner() {
        mockPackageManager.stub {
            on { isPackageStateProtected(PACKAGE_NAME, UserHandle.getUserId(UID)) } doReturn true
        }
        mockDevicePolicyManager.stub {
            on { deviceOwnerComponentOnAnyUser } doReturn null
        }

        val admin = appForceStopButton.getAdminRestriction(createApp())!!

        assertThat(admin.component).isNull()
    }

    private fun setForceStopButton(app: ApplicationInfo) {
        composeTestRule.setContent {
            val actionButton = appForceStopButton.getActionButton(app)
            Button(onClick = {}, enabled = actionButton.enabled) {
                Text(actionButton.text)
            }
        }
        composeTestRule.delay()
    }

    private fun createApp(builder: ApplicationInfo.() -> Unit = {}) =
        ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            enabled = true
        }.apply(builder)

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val UID = 10000
        val DEVICE_OWNER = ComponentName("device", "Owner")
    }
}
