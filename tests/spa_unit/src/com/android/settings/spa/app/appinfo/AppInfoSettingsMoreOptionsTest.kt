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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.os.UserManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.userId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppInfoSettingsMoreOptionsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageInfoPresenter: PackageInfoPresenter

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var devicePolicyManager: DevicePolicyManager

    @Spy
    private var resources = context.resources

    @Mock
    private lateinit var packageManagers: IPackageManagers

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(Utils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.userManager).thenReturn(userManager)
        whenever(context.devicePolicyManager).thenReturn(devicePolicyManager)
        whenever(Utils.isProfileOrDeviceOwner(userManager, devicePolicyManager, PACKAGE_NAME))
            .thenReturn(false)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenProfileOrDeviceOwner_notDisplayed() {
        whenever(Utils.isProfileOrDeviceOwner(userManager, devicePolicyManager, PACKAGE_NAME))
            .thenReturn(true)

        setContent(ApplicationInfo())

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun uninstallUpdates_updatedSystemAppAndUserAdmin_displayed() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            flags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        }
        whenever(userManager.isUserAdmin(app.userId)).thenReturn(true)
        whenever(resources.getBoolean(R.bool.config_disable_uninstall_update)).thenReturn(false)

        setContent(app)
        composeTestRule.onRoot().performClick()

        composeTestRule.waitUntilExists(hasText(context.getString(R.string.app_factory_reset)))
    }

    @Test
    fun uninstallForAllUsers_regularAppAndPrimaryUser_displayed() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
        whenever(userManager.aliveUsers).thenReturn(listOf(OTHER_USER))
        whenever(packageManagers.isPackageInstalledAsUser(PACKAGE_NAME, OTHER_USER_ID))
            .thenReturn(true)

        setContent(app)
        composeTestRule.onRoot().performClick()

        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.uninstall_all_users_text))
        )
    }

    private fun setContent(app: ApplicationInfo) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppInfoSettingsMoreOptions(packageInfoPresenter, app, packageManagers)
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val UID = 123
        const val OTHER_USER_ID = 10
        val OTHER_USER = UserInfo(OTHER_USER_ID, "Other user", 0)
    }
}
