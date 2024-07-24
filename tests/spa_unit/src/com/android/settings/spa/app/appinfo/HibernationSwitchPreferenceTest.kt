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

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.MODE_DEFAULT
import android.app.AppOpsManager.MODE_IGNORED
import android.app.AppOpsManager.OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED
import android.apphibernation.AppHibernationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.Flags as PmFlags
import android.os.Build
import android.os.SystemProperties
import android.permission.PermissionControllerManager
import android.permission.PermissionControllerManager.HIBERNATION_ELIGIBILITY_ELIGIBLE
import android.permission.PermissionControllerManager.HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM
import android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED
import com.android.settings.Utils.PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS
import com.android.settings.flags.Flags
import com.android.settings.testutils.TestDeviceConfig
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.spaprivileged.framework.common.appHibernationManager
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.framework.common.permissionControllerManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.util.function.IntConsumer
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class HibernationSwitchPreferenceTest {

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var permissionControllerManager: PermissionControllerManager

    @Mock
    private lateinit var appOpsManager: AppOpsManager

    @Mock
    private lateinit var appHibernationManager: AppHibernationManager

    private val hibernationEnabledConfig =
        TestDeviceConfig(NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED)

    private val hibernationTargetsPreSConfig =
        TestDeviceConfig(NAMESPACE_APP_HIBERNATION, PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS)

    private val isHibernationSwitchEnabledStateFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        hibernationEnabledConfig.override(true)
        hibernationTargetsPreSConfig.override(false)
        context.mockAsUser()
        whenever(context.permissionControllerManager).thenReturn(permissionControllerManager)
        whenever(context.appOpsManager).thenReturn(appOpsManager)
        whenever(context.appHibernationManager).thenReturn(appHibernationManager)
        mockHibernationEligibility(HIBERNATION_ELIGIBILITY_ELIGIBLE)
    }

    @After
    fun cleanUp() {
        hibernationEnabledConfig.reset()
        hibernationTargetsPreSConfig.reset()
    }

    private fun mockHibernationEligibility(eligibility: Int) {
        doAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[2] as IntConsumer).accept(eligibility)
        }.`when`(permissionControllerManager).getHibernationEligibility(
            eq(PACKAGE_NAME), any(), any()
        )
    }

    private fun mockOpsMode(mode: Int) {
        whenever(
            appOpsManager.checkOpNoThrow(OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, UID, PACKAGE_NAME)
        ).thenReturn(mode)
    }

    @Test
    fun `Hibernation disabled - not display`() {
        hibernationEnabledConfig.override(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun `Not eligible - displayed but disabled`() {
        mockHibernationEligibility(HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM)

        setContent()

        val text = if (isArchivingEnabled()) {
            context.getString(R.string.unused_apps_switch_v2)
        } else {
            context.getString(R.string.unused_apps_switch)
        }
        composeTestRule.onNodeWithText(text)
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .assertIsOff()
    }

    private fun isArchivingEnabled() =
            PmFlags.archiving() || SystemProperties.getBoolean("pm.archiving.enabled", false)
                    || Flags.appArchiving()
    @Test
    fun `An app targets Q with ops mode default when hibernation targets pre S - not exempted`() {
        mockOpsMode(MODE_DEFAULT)
        hibernationTargetsPreSConfig.override(true)

        setContent(TARGET_Q_APP)

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOn()
    }

    @Test
    fun `An app targets Q with ops mode default when hibernation targets R - exempted`() {
        mockOpsMode(MODE_DEFAULT)
        hibernationTargetsPreSConfig.override(false)

        setContent(TARGET_Q_APP)

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOff()
    }

    @Test
    fun `An app targets R with ops mode default - not exempted`() {
        mockOpsMode(MODE_DEFAULT)

        setContent(TARGET_R_APP)

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOn()
    }

    @Test
    fun `An app with ops mode allowed - not exempted`() {
        mockOpsMode(MODE_ALLOWED)

        setContent()

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOn()
    }

    @Test
    fun `An app with ops mode ignored - exempted`() {
        mockOpsMode(MODE_IGNORED)

        setContent()

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOff()
    }

    @Test
    fun `An app is exempted - on click`() {
        mockOpsMode(MODE_IGNORED)

        setContent()
        composeTestRule.onRoot().performClick()

        verify(appOpsManager).setUidMode(OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, UID, MODE_ALLOWED)
        verify(appHibernationManager, never()).setHibernatingForUser(anyString(), anyBoolean())
        verify(appHibernationManager, never()).setHibernatingGlobally(anyString(), anyBoolean())
    }

    @Test
    fun `An app is not exempted - on click`() {
        mockOpsMode(MODE_ALLOWED)

        setContent()
        composeTestRule.onRoot().performClick()

        verify(appOpsManager).setUidMode(OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, UID, MODE_IGNORED)
        verify(appHibernationManager).setHibernatingForUser(PACKAGE_NAME, false)
        verify(appHibernationManager).setHibernatingGlobally(PACKAGE_NAME, false)
    }

    private fun setContent(app: ApplicationInfo = TARGET_R_APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                HibernationSwitchPreference(app, isHibernationSwitchEnabledStateFlow)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "package name"
        const val UID = 123

        val TARGET_R_APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            targetSdkVersion = Build.VERSION_CODES.R
        }
        val TARGET_Q_APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            targetSdkVersion = Build.VERSION_CODES.Q
        }
    }
}
