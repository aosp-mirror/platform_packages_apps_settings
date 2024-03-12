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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail
import com.android.settings.fuelgauge.batteryusage.BatteryChartPreferenceController
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry
import com.android.settingslib.spaprivileged.model.app.userId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppBatteryPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Spy
    private val resources = context.resources

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(BatteryChartPreferenceController::class.java)
            .mockStatic(AdvancedPowerUsageDetail::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.resources).thenReturn(resources)
        whenever(resources.getBoolean(R.bool.config_show_app_info_settings_battery))
            .thenReturn(true)
    }

    private fun mockBatteryDiffEntry(batteryDiffEntry: BatteryDiffEntry?) {
        whenever(BatteryChartPreferenceController.getAppBatteryUsageData(
            context, PACKAGE_NAME, APP.userId
        )).thenReturn(batteryDiffEntry)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenConfigIsFalse_notDisplayed() {
        whenever(resources.getBoolean(R.bool.config_show_app_info_settings_battery))
            .thenReturn(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenAppNotInstalled_noSummary() {
        val notInstalledApp = ApplicationInfo()

        setContent(notInstalledApp)

        composeTestRule.onNode(hasTextExactly(context.getString(R.string.battery_details_title)))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun batteryDiffEntryIsNull() {
        mockBatteryDiffEntry(null)

        setContent()

        composeTestRule.onNode(
            hasTextExactly(
                context.getString(R.string.battery_details_title),
                context.getString(R.string.no_battery_summary),
            ),
        ).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun noConsumePower() {
        val batteryDiffEntry = mock(BatteryDiffEntry::class.java).apply {
            mConsumePower = 0.0
        }
        mockBatteryDiffEntry(batteryDiffEntry)

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.no_battery_summary))
            .assertIsDisplayed()
    }

    @Test
    fun hasConsumePower() {
        val batteryDiffEntry = mock(BatteryDiffEntry::class.java).apply {
            mConsumePower = 12.3
        }
        whenever(batteryDiffEntry.percentage).thenReturn(45.6)
        mockBatteryDiffEntry(batteryDiffEntry)

        setContent()

        composeTestRule.onNodeWithText("46% use since last full charge").assertIsDisplayed()
    }

    @Test
    fun whenClick_openDetailsPage() {
        val batteryDiffEntry = mock(BatteryDiffEntry::class.java)
        whenever(batteryDiffEntry.percentage).thenReturn(10.0)
        mockBatteryDiffEntry(batteryDiffEntry)

        setContent()
        composeTestRule.onRoot().performClick()

        ExtendedMockito.verify {
            AdvancedPowerUsageDetail.startBatteryDetailPage(
                context,
                AppInfoSettingsProvider.METRICS_CATEGORY,
                batteryDiffEntry,
                "10%",
                null,
                false,
                null,
                null
            )
        }
    }

    private fun setContent(app: ApplicationInfo = APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppBatteryPreference(app)
            }
        }
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
