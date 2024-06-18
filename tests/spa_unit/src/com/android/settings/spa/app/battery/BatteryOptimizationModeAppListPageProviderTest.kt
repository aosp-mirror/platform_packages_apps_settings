/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.spa.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.DisplaySettings
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail
import com.android.settings.spa.app.battery.BatteryOptimizationModeAppList
import com.android.settings.spa.app.battery.BatteryOptimizationModeAppListModel
import com.android.settings.spa.app.battery.BatteryOptimizationModeAppListPageProvider
import com.android.settingslib.spa.testutils.FakeNavControllerWrapper
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spaprivileged.framework.compose.getPlaceholder
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BatteryOptimizationModeAppListPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeNavControllerWrapper = FakeNavControllerWrapper()

    private val packageManager = mock<PackageManager> {
        on { getPackagesForUid(USER_ID) } doReturn arrayOf(PACKAGE_NAME)
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn packageManager
    }

    @Test
    fun batteryOptimizationModeAppListPageProvider_name() {
        assertThat(BatteryOptimizationModeAppListPageProvider.name)
            .isEqualTo("BatteryOptimizationModeAppList")
    }

    @Test
    fun injectEntry_title() {
        setInjectEntry()

        composeTestRule.onNodeWithText(context.getString(R.string.app_battery_usage_title))
            .assertIsDisplayed()
    }

    @Test
    fun injectEntry_onClick_navigate() {
        setInjectEntry()

        composeTestRule.onNodeWithText(context.getString(R.string.app_battery_usage_title))
            .performClick()

        assertThat(fakeNavControllerWrapper.navigateCalledWith)
            .isEqualTo("BatteryOptimizationModeAppList")
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            BatteryOptimizationModeAppList {}
        }

        composeTestRule.onNodeWithText(context.getString(R.string.app_battery_usage_title))
            .assertIsDisplayed()
    }

    @Test
    fun showInstantApps_isFalse() {
        val input = getAppListInput()

        assertThat(input.config.showInstantApps).isFalse()
    }

    @Test
    fun item_labelDisplayed() {
        setItemContent()

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun item_summaryDisplayed() {
        setItemContent()

        composeTestRule.onNodeWithText(SUMMARY).assertIsDisplayed()
    }

    @Test
    fun item_onClick_navigate() {
        setItemContent()
        doNothing().whenever(context).startActivity(any())

        composeTestRule.onNodeWithText(LABEL).performClick()

        val intent = argumentCaptor<Intent> {
            verify(context).startActivity(capture())
        }.firstValue

        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))!!
            .isEqualTo(AdvancedPowerUsageDetail::class.java.name)
        val arguments = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)!!
        assertThat(arguments.getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME))
            .isEqualTo(PACKAGE_NAME)
    }

    @Test
    fun BatteryOptimizationModeAppListModel_transform() = runTest {
        val listModel = BatteryOptimizationModeAppListModel(context)

        val recordListFlow = listModel.transform(flowOf(USER_ID), flowOf(listOf(APP)))

        val recordList = recordListFlow.firstWithTimeoutOrNull()!!
        assertThat(recordList).hasSize(1)
        assertThat(recordList[0].app).isSameInstanceAs(APP)
    }

    @Test
    fun listModelGetSummary_regular() {
        val listModel = BatteryOptimizationModeAppListModel(context)

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary = listModel.getSummary(option = 0, record = AppRecordWithSize(app = APP))
        }

        assertThat(summary()).isEmpty()
    }

    @Test
    fun listModelGetSummary_disabled() {
        val listModel = BatteryOptimizationModeAppListModel(context)
        val disabledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
            enabled = false
        }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary =
                listModel.getSummary(option = 0, record = AppRecordWithSize(app = disabledApp))
        }

        assertThat(summary())
            .isEqualTo(context.getString(com.android.settingslib.R.string.disabled))
    }

    @Test
    fun listModelGetSummary_notInstalled() {
        val listModel = BatteryOptimizationModeAppListModel(context)
        val notInstalledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary =
                listModel.getSummary(option = 0, record = AppRecordWithSize(app = notInstalledApp))
        }

        assertThat(summary()).isEqualTo(context.getString(R.string.not_installed))
    }

    @Test
    fun batteryOptimizationModeAppListModel_archivedApp() {
        val app = mock<ApplicationInfo> {
            on { loadUnbadgedIcon(any()) } doReturn UNBADGED_ICON
            on { loadLabel(any()) } doReturn LABEL
        }
        app.isArchived = true
        packageManager.stub {
            on {
                getApplicationInfoAsUser(PACKAGE_NAME, 0, USER_ID)
            } doReturn app
        }
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                with(BatteryOptimizationModeAppListModel(context)) {
                    AppListItemModel(
                        record = AppRecordWithSize(app = app),
                        label = LABEL,
                        summary = { SUMMARY },
                    ).AppItem()
                }
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun batteryOptimizationModeAppListModel_NoStorageSummary() {
        val listModel = BatteryOptimizationModeAppListModel(context)
        val archivedApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = true
        }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary =
                listModel.getSummary(option = 0, record = AppRecordWithSize(app = archivedApp))
        }

        assertThat(summary()).isEmpty()
    }

    private fun setInjectEntry() {
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                BatteryOptimizationModeAppListPageProvider.buildInjectEntry().build().UiLayout()
            }
        }
    }

    private fun getAppListInput(): AppListInput<AppRecordWithSize> {
        lateinit var input: AppListInput<AppRecordWithSize>
        composeTestRule.setContent {
            BatteryOptimizationModeAppList {
                SideEffect {
                    input = this
                }
            }
        }
        return input
    }

    private fun setItemContent() {
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                with(BatteryOptimizationModeAppListModel(context)) {
                    AppListItemModel(
                        record = AppRecordWithSize(app = APP),
                        label = LABEL,
                        summary = { SUMMARY },
                    ).AppItem()
                }
            }
        }
    }

    private companion object {
        const val USER_ID = 0
        const val PACKAGE_NAME = "package.name"
        const val LABEL = "Label"
        const val SUMMARY = "Summary"
        val UNBADGED_ICON = mock<Drawable>()
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
        }
    }
}
