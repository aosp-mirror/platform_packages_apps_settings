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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.NetworkTemplate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.datausage.AppDataUsage
import com.android.settings.datausage.lib.IAppDataUsageSummaryRepository
import com.android.settings.datausage.lib.INetworkTemplates
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppDataUsagePreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    private val context: Context = ApplicationProvider.getApplicationContext()

    private var networkUsageData: NetworkUsageData? = null

    private inner class TestRepository : IAppDataUsageSummaryRepository {
        override suspend fun querySummary(uid: Int): NetworkUsageData? = when (uid) {
            UID -> networkUsageData
            else -> null
        }
    }

    @Before
    fun setUp() {
        mockSession = mockitoSession()
            .initMocks(this)
            .mockStatic(Utils::class.java)
            .mockStatic(AppInfoDashboardFragment::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(Utils.isBandwidthControlEnabled()).thenReturn(true)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenBandwidthControlDisabled_notDisplayed() {
        whenever(Utils.isBandwidthControlEnabled()).thenReturn(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenAppNotInstalled_disabled() {
        val notInstalledApp = ApplicationInfo()

        setContent(notInstalledApp)

        composeTestRule.onNodeWithText(context.getString(R.string.cellular_data_usage))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun whenAppInstalled_enabled() {
        setContent(APP)

        composeTestRule.onNodeWithText(context.getString(R.string.cellular_data_usage))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun whenNoDataUsage() {
        networkUsageData = null

        setContent()

        composeTestRule.waitUntilExists(hasText(context.getString(R.string.no_data_usage)))
    }

    @Test
    fun whenHasDataUsage() {
        networkUsageData = NetworkUsageData(
            startTime = 1666666666666L,
            endTime = 1666666666666L,
            usage = 123L,
        )

        setContent()

        composeTestRule.waitUntilExists(hasText("123 B used since Oct 25, 2022"))
    }

    @Test
    fun whenClick_startActivity() {
        setContent()
        composeTestRule.onRoot().performClick()

        ExtendedMockito.verify {
            AppInfoDashboardFragment.startAppInfoFragment(
                AppDataUsage::class.java,
                APP,
                context,
                AppInfoSettingsProvider.METRICS_CATEGORY,
            )
        }
    }

    private fun setContent(app: ApplicationInfo = APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppDataUsagePreference(app, TestNetworkTemplates) { _, _ ->
                    TestRepository()
                }
            }
        }
        composeTestRule.delay()
    }

    private object TestNetworkTemplates : INetworkTemplates {
        override fun getDefaultTemplate(context: Context): NetworkTemplate =
            NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE).build()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            flags = ApplicationInfo.FLAG_INSTALLED
        }
    }
}
