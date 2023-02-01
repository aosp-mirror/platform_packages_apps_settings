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
import com.android.settingslib.net.NetworkCycleDataForUid
import com.android.settingslib.net.NetworkCycleDataForUidLoader
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppDataUsagePreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var builder: NetworkCycleDataForUidLoader.Builder<NetworkCycleDataForUidLoader>

    @Mock
    private lateinit var loader: NetworkCycleDataForUidLoader

    @Before
    fun setUp() {
        mockSession = mockitoSession()
            .initMocks(this)
            .mockStatic(Utils::class.java)
            .mockStatic(NetworkCycleDataForUidLoader::class.java)
            .mockStatic(NetworkTemplate::class.java)
            .mockStatic(AppInfoDashboardFragment::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(Utils.isBandwidthControlEnabled()).thenReturn(true)
        whenever(NetworkCycleDataForUidLoader.builder(context)).thenReturn(builder)
        whenever(builder.build()).thenReturn(loader)
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

        composeTestRule.onNodeWithText(context.getString(R.string.data_usage_app_summary_title))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun whenAppInstalled_enabled() {
        setContent(APP)

        composeTestRule.onNodeWithText(context.getString(R.string.data_usage_app_summary_title))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun setCorrectValuesForBuilder() {
        setContent()

        verify(builder).setRetrieveDetail(false)
        verify(builder).addUid(UID)
    }

    @Test
    fun whenNoDataUsage() {
        whenever(loader.loadInBackground()).thenReturn(emptyList())

        setContent()

        composeTestRule.waitUntilExists(hasText(context.getString(R.string.no_data_usage)))
    }

    @Test
    fun whenHasDataUsage() {
        val cycleData = mock(NetworkCycleDataForUid::class.java)
        whenever(cycleData.totalUsage).thenReturn(123)
        whenever(cycleData.startTime).thenReturn(1666666666666)
        whenever(loader.loadInBackground()).thenReturn(listOf(cycleData))

        setContent()

        composeTestRule.waitUntilExists(hasText("123 B used since Oct 25, 2022"))
    }

    @Test
    fun whenClick_startActivity() {
        whenever(loader.loadInBackground()).thenReturn(emptyList())

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
                AppDataUsagePreference(app)
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "packageName"
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            flags = ApplicationInfo.FLAG_INSTALLED
        }
    }
}
