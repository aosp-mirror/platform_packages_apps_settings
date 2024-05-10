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

package com.android.settings.spa.app.backgroundinstall

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.IBackgroundInstallControlService
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ParceledListSlice
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.FakeNavControllerWrapper
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BackgroundInstalledAppsPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockBackgroundInstallControlService: IBackgroundInstallControlService

    private var packageInfoFlagsCaptor = argumentCaptor<PackageManager.PackageInfoFlags>()

    private val fakeNavControllerWrapper = FakeNavControllerWrapper()

    @Before
    fun setup() {
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    }
    @Test
    fun allAppListPageProvider_name() {
        assertThat(BackgroundInstalledAppsPageProvider.name)
            .isEqualTo(EXPECTED_PROVIDER_NAME)
    }

    @Test
    fun injectEntry_title() {
        whenever(mockBackgroundInstallControlService.getBackgroundInstalledPackages(any(), any()))
            .thenReturn(ParceledListSlice(listOf()))

        setInjectEntry(false)

        composeTestRule.onNodeWithText(
            context.getString(R.string.background_install_title)).assertIsDisplayed()
    }

    @Test
    fun injectEntry_title_disabled() {
        setInjectEntry(true)

        composeTestRule.onNodeWithText(
            context.getString(R.string.background_install_title)).assertDoesNotExist()
    }

    @Test
    fun injectEntry_summary() {
        whenever(mockBackgroundInstallControlService.getBackgroundInstalledPackages(any(), any()))
            .thenReturn(ParceledListSlice(listOf()))

        setInjectEntry(false)

        composeTestRule.onNodeWithText("0 apps").assertIsDisplayed()
    }

    @Test
    fun injectEntry_summary_disabled() {
        setInjectEntry(true)

        composeTestRule.onNodeWithText("0 apps").assertDoesNotExist()
    }

    @Test
    fun injectEntry_onClick_navigate() {
        whenever(mockBackgroundInstallControlService.getBackgroundInstalledPackages(any(), any()))
            .thenReturn(ParceledListSlice(listOf()))
        setInjectEntry(false)

        composeTestRule.onNodeWithText(
            context.getString(R.string.background_install_title)).performClick()

        assertThat(fakeNavControllerWrapper.navigateCalledWith)
            .isEqualTo(EXPECTED_PROVIDER_NAME)
    }

    private fun setInjectEntry(disableFeature: Boolean = false) {
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                BackgroundInstalledAppsPageProvider
                    .setBackgroundInstallControlService(mockBackgroundInstallControlService)
                    .setDisableFeature(disableFeature)
                    .buildInjectEntry().build().UiLayout()
            }
        }
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            BackgroundInstalledAppList()
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.background_install_title)).assertIsDisplayed()
    }

    @Test
    fun item_labelDisplayed() {
        setItemContent()

        composeTestRule.onNodeWithText(TEST_LABEL).assertIsDisplayed()
    }

    @Test
    fun item_onClick_navigate() {
        setItemContent()

        composeTestRule.onNodeWithText(TEST_LABEL).performClick()

        assertThat(fakeNavControllerWrapper.navigateCalledWith)
            .isEqualTo("AppInfoSettings/package.name/0")
    }

    @Test
    fun backgroundInstalledAppsWithGroupingListModel_getGroupTitleOne() = runTest {
        val listModel = BackgroundInstalledAppsWithGroupingListModel(context)

        val actualGroupTitle = listModel
            .getGroupTitle(0,
                BackgroundInstalledAppListWithGroupingAppRecord(
                    APP,
                    System.currentTimeMillis()
                ))

        assertThat(actualGroupTitle).isEqualTo("Apps installed in the last 6 months")
    }

    @Test
    fun backgroundInstalledAppsWithGroupingListModel_getGroupTitleTwo() = runTest {
        val listModel = BackgroundInstalledAppsWithGroupingListModel(context)

        val actualGroupTitle = listModel
            .getGroupTitle(0,
                BackgroundInstalledAppListWithGroupingAppRecord(
                APP,
                    0L
        ))

        assertThat(actualGroupTitle).isEqualTo("Apps installed more than 6 months ago")
    }

    @Test
    fun backgroundInstalledAppsWithGroupingListModel_transform() = runTest {
        val listModel = BackgroundInstalledAppsWithGroupingListModel(mockContext)
        whenever(mockPackageManager.getPackageInfoAsUser(
            eq(TEST_PACKAGE_NAME),
            packageInfoFlagsCaptor.capture(),
            eq(TEST_USER_ID))
        )
            .thenReturn(PACKAGE_INFO)
        val recordListFlow = listModel.transform(flowOf(TEST_USER_ID), flowOf(listOf(APP)))

        val recordList = recordListFlow.first()

        assertThat(recordList).hasSize(1)
        assertThat(recordList[0].app).isSameInstanceAs(APP)
        assertThat(packageInfoFlagsCaptor.firstValue.value).isEqualTo(EXPECTED_PACKAGE_INFO_FLAG)
    }

    @Test
    fun backgroundInstalledAppsWithGroupingListModel_filter() = runTest {
        val listModel = BackgroundInstalledAppsWithGroupingListModel(mockContext)
        listModel.setBackgroundInstallControlService(mockBackgroundInstallControlService)
        whenever(mockBackgroundInstallControlService.getBackgroundInstalledPackages(
            PackageManager.MATCH_ALL.toLong(),
            TEST_USER_ID
        )).thenReturn(ParceledListSlice(listOf(PACKAGE_INFO)))

        val recordListFlow = listModel.filter(
            flowOf(TEST_USER_ID),
            0,
            flowOf(listOf(APP_RECORD_WITH_PACKAGE_MATCH, APP_RECORD_WITHOUT_PACKAGE_MATCH))
        )

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
        assertThat(recordList[0]).isSameInstanceAs(APP_RECORD_WITH_PACKAGE_MATCH)
    }

    private fun setItemContent() {
        composeTestRule.setContent {
            BackgroundInstalledAppList {
                fakeNavControllerWrapper.Wrapper {
                    with(BackgroundInstalledAppsWithGroupingListModel(context)) {
                        AppListItemModel(
                            record = BackgroundInstalledAppListWithGroupingAppRecord(
                                app = APP,
                                dateOfInstall = TEST_FIRST_INSTALL_TIME),
                            label = TEST_LABEL,
                            summary = { TEST_SUMMARY },
                        ).AppItem()
                    }
                }
            }
        }
    }

    private companion object {
        private const val TEST_USER_ID = 0
        private const val TEST_PACKAGE_NAME = "package.name"
        private const val TEST_NO_MATCH_PACKAGE_NAME = "no.match"
        private const val TEST_LABEL = "Label"
        private const val TEST_SUMMARY = "Summary"
        private const val TEST_FIRST_INSTALL_TIME = 0L
        private const val EXPECTED_PROVIDER_NAME = "BackgroundInstalledAppsPage"
        private const val EXPECTED_PACKAGE_INFO_FLAG = 0L

        val APP = ApplicationInfo().apply {
            packageName = TEST_PACKAGE_NAME
        }
        val APP_NO_RECORD = ApplicationInfo().apply {
            packageName = TEST_NO_MATCH_PACKAGE_NAME
        }
        val APP_RECORD_WITH_PACKAGE_MATCH = BackgroundInstalledAppListWithGroupingAppRecord(
            APP,
            TEST_FIRST_INSTALL_TIME
        )
        val APP_RECORD_WITHOUT_PACKAGE_MATCH = BackgroundInstalledAppListWithGroupingAppRecord(
            APP_NO_RECORD,
            TEST_FIRST_INSTALL_TIME
        )
        val PACKAGE_INFO = PackageInfo().apply {
            packageName = TEST_PACKAGE_NAME
            applicationInfo = APP
            firstInstallTime = TEST_FIRST_INSTALL_TIME
        }
    }
}