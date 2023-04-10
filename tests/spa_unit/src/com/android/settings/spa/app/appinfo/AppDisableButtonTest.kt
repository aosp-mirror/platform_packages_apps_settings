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
import android.os.UserManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.Utils
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import com.android.settingslib.Utils as SettingsLibUtils
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppDisableButtonTest {
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

    private val fakeFeatureFactory = FakeFeatureFactory()
    private val appFeatureProvider = fakeFeatureFactory.mockApplicationFeatureProvider

    private lateinit var appDisableButton: AppDisableButton

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(SettingsLibUtils::class.java)
            .mockStatic(Utils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.userManager).thenReturn(userManager)
        whenever(context.devicePolicyManager).thenReturn(devicePolicyManager)
        whenever(appFeatureProvider.keepEnabledPackages).thenReturn(emptySet())
        whenever(
            SettingsLibUtils.isEssentialPackage(context.resources, packageManager, PACKAGE_NAME)
        ).thenReturn(false)
        whenever(Utils.isProfileOrDeviceOwner(userManager, devicePolicyManager, PACKAGE_NAME))
            .thenReturn(false)
        appDisableButton = AppDisableButton(packageInfoPresenter)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun getActionButton_signedWithPlatformKey_cannotDisable() {
        val app = enabledSystemApp {
            privateFlags = privateFlags or ApplicationInfo.PRIVATE_FLAG_SIGNED_WITH_PLATFORM_KEY
        }

        val actionButton = setDisableButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_isResourceOverlay_cannotDisable() {
        val app = enabledSystemApp {
            privateFlags = privateFlags or ApplicationInfo.PRIVATE_FLAG_IS_RESOURCE_OVERLAY
        }

        val actionButton = setDisableButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_isKeepEnabledPackages_cannotDisable() {
        whenever(appFeatureProvider.keepEnabledPackages).thenReturn(setOf(PACKAGE_NAME))
        val app = enabledSystemApp()

        val actionButton = setDisableButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_isEssentialPackage_cannotDisable() {
        whenever(
            SettingsLibUtils.isEssentialPackage(context.resources, packageManager, PACKAGE_NAME)
        ).thenReturn(true)
        val app = enabledSystemApp()

        val actionButton = setDisableButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_isProfileOrDeviceOwner_cannotDisable() {
        whenever(Utils.isProfileOrDeviceOwner(userManager, devicePolicyManager, PACKAGE_NAME))
            .thenReturn(true)
        val app = enabledSystemApp()

        val actionButton = setDisableButton(app)

        assertThat(actionButton.enabled).isFalse()
    }

    @Test
    fun getActionButton_regularEnabledSystemApp_canDisable() {
        val app = enabledSystemApp()

        val actionButton = setDisableButton(app)

        assertThat(actionButton.enabled).isTrue()
    }

    private fun setDisableButton(app: ApplicationInfo): ActionButton {
        lateinit var actionButton: ActionButton
        composeTestRule.setContent {
            actionButton = appDisableButton.getActionButton(app)!!
        }
        return actionButton
    }

    private fun enabledSystemApp(builder: ApplicationInfo.() -> Unit = {}) =
        ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            enabled = true
            flags = ApplicationInfo.FLAG_SYSTEM
        }.apply(builder)

    private companion object {
        const val PACKAGE_NAME = "package.name"
    }
}
