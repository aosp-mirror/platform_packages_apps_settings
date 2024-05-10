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

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.framework.common.storageStatsManager
import java.util.UUID
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppStoragePreferenceTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var storageStatsManager: StorageStatsManager

    @Before
    fun setUp() {
        whenever(context.storageStatsManager).thenReturn(storageStatsManager)
        whenever(
            storageStatsManager.queryStatsForPackage(eq(STORAGE_UUID), eq(PACKAGE_NAME), any())
        ).thenReturn(STATS)
    }

    @Test
    fun notInstalledApp_notDisplayed() {
        val notInstalledApp = ApplicationInfo()

        setContent(notInstalledApp)

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun internalApp_displayed() {
        val internalApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
            storageUuid = STORAGE_UUID
        }

        setContent(internalApp)

        composeTestRule.onNodeWithText(context.getString(R.string.storage_settings_for_app))
            .assertIsDisplayed()
        composeTestRule.waitUntilExists(hasText("120 B used in internal storage"))
    }

    @Test
    fun externalApp_displayed() {
        val externalApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_EXTERNAL_STORAGE
            storageUuid = STORAGE_UUID
        }

        setContent(externalApp)

        composeTestRule.onNodeWithText(context.getString(R.string.storage_settings_for_app))
            .assertIsDisplayed()
        composeTestRule.waitUntilExists(hasText("120 B used in external storage"))
    }

    private fun setContent(app: ApplicationInfo) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppStoragePreference(app)
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "package name"
        private val STORAGE_UUID = UUID.randomUUID()

        private val STATS = StorageStats().apply {
            codeBytes = 100
            dataBytes = 20
            cacheBytes = 3
        }
    }
}
