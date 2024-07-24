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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail
import com.android.settings.fuelgauge.batteryusage.BatteryChartPreferenceController
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppBatteryPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val resources = spy(context.resources) {
        on { getBoolean(R.bool.config_show_app_info_settings_battery) } doReturn true
    }

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .mockStatic(BatteryChartPreferenceController::class.java)
            .mockStatic(AdvancedPowerUsageDetail::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.resources).thenReturn(resources)
    }

    private fun mockBatteryDiffEntry(batteryDiffEntry: BatteryDiffEntry?) {
        whenever(
            BatteryChartPreferenceController.getAppBatteryUsageData(
                context, PACKAGE_NAME, APP.userId
            )
        ).thenReturn(batteryDiffEntry)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenConfigIsFalse_notDisplayed() {
        resources.stub {
            on { getBoolean(R.bool.config_show_app_info_settings_battery) } doReturn false
        }

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

        composeTestRule.waitUntilExactlyOneExists(
            hasTextExactly(
                context.getString(R.string.battery_details_title),
                context.getString(R.string.no_battery_summary),
            ) and isEnabled(),
        )
    }

    @Test
    fun noConsumePower() {
        val batteryDiffEntry = mock<BatteryDiffEntry>().apply { mConsumePower = 0.0 }
        mockBatteryDiffEntry(batteryDiffEntry)

        setContent()

        composeTestRule.waitUntilExactlyOneExists(
            hasText(context.getString(R.string.no_battery_summary))
        )
    }

    @Test
    fun hasConsumePower() {
        val batteryDiffEntry = mock<BatteryDiffEntry> {
            on { percentage } doReturn 45.6
        }.apply { mConsumePower = 12.3 }
        mockBatteryDiffEntry(batteryDiffEntry)

        setContent()

        composeTestRule.waitUntilExactlyOneExists(hasText("46% use since last full charge"))
    }

    @Test
    fun whenClick_openDetailsPage() {
        val batteryDiffEntry = mock<BatteryDiffEntry> {
            on { percentage } doReturn 10.0
        }.apply { mConsumePower = 12.3 }
        mockBatteryDiffEntry(batteryDiffEntry)

        setContent()
        composeTestRule.waitUntilExactlyOneExists(hasText("10% use since last full charge"))
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
        const val PACKAGE_NAME = "package.name"
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            flags = ApplicationInfo.FLAG_INSTALLED
        }
    }
}
