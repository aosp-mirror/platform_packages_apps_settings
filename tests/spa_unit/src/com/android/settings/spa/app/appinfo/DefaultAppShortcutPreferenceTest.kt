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

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.UserManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.eq
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class DefaultAppShortcutPreferenceTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var roleManager: RoleManager

    @Before
    fun setUp() {
        whenever(context.userManager).thenReturn(userManager)
        whenever(userManager.isManagedProfile(anyInt())).thenReturn(false)
        whenever(context.getSystemService(RoleManager::class.java)).thenReturn(roleManager)
        mockIsRoleVisible(true)
        mockIsApplicationVisibleForRole(true)
    }

    private fun mockIsRoleVisible(visible: Boolean) {
        doAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[2] as Consumer<Boolean>).accept(visible)
        }.`when`(roleManager).isRoleVisible(eq(ROLE), any(), any())
    }

    private fun mockIsApplicationVisibleForRole(visible: Boolean) {
        doAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[3] as Consumer<Boolean>).accept(visible)
        }.`when`(roleManager).isApplicationVisibleForRole(eq(ROLE), eq(PACKAGE_NAME), any(), any())
    }

    @Test
    fun isManagedProfile_notDisplay() {
        whenever(userManager.isManagedProfile(anyInt())).thenReturn(true)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun roleNotVisible_notDisplay() {
        mockIsRoleVisible(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun applicationVisibleForRole_notDisplay() {
        mockIsApplicationVisibleForRole(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun isRoleHolder_summaryIsYes() {
        whenever(roleManager.getRoleHoldersAsUser(eq(ROLE), any())).thenReturn(listOf(PACKAGE_NAME))

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.yes))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun notRoleHolder_summaryIsNo() {
        whenever(roleManager.getRoleHoldersAsUser(eq(ROLE), any())).thenReturn(emptyList())

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.no))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun onClick_startManageDefaultAppIntent() {
        whenever(roleManager.getRoleHoldersAsUser(eq(ROLE), any())).thenReturn(emptyList())
        doNothing().`when`(context).startActivityAsUser(any(), any())

        setContent()
        composeTestRule.onRoot().performClick()

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivityAsUser(intentCaptor.capture(), any())
        val intent = intentCaptor.value
        assertThat(intent.action).isEqualTo(Intent.ACTION_MANAGE_DEFAULT_APP)
        assertThat(intent.getStringExtra(Intent.EXTRA_ROLE_NAME)).isEqualTo(ROLE)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                DefaultAppShortcutPreference(SHORTCUT, App)
            }
        }
    }

    private companion object {
        const val ROLE = RoleManager.ROLE_HOME
        val SHORTCUT = DefaultAppShortcut(roleName = ROLE, titleResId = R.string.home_app)
        const val PACKAGE_NAME = "package name"

        val App = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}
