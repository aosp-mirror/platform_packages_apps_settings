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

package com.android.settings.spa.app

import android.content.Context
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
import com.android.settings.R
import com.android.settingslib.spa.framework.compose.stateOf
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub


@RunWith(AndroidJUnit4::class)
class AllAppListTest {
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
    fun allAppListPageProvider_name() {
        assertThat(AllAppListPageProvider.name).isEqualTo("AllAppList")
    }

    @Test
    fun injectEntry_title() {
        setInjectEntry()

        composeTestRule.onNodeWithText(context.getString(R.string.all_apps)).assertIsDisplayed()
    }

    @Test
    fun injectEntry_onClick_navigate() {
        setInjectEntry()

        composeTestRule.onNodeWithText(context.getString(R.string.all_apps)).performClick()

        assertThat(fakeNavControllerWrapper.navigateCalledWith).isEqualTo("AllAppList")
    }

    private fun setInjectEntry() {
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                AllAppListPageProvider.buildInjectEntry().build().UiLayout()
            }
        }
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            AllAppListPage {}
        }

        composeTestRule.onNodeWithText(context.getString(R.string.all_apps)).assertIsDisplayed()
    }

    @Test
    fun showInstantApps_isTrue() {
        val input = getAppListInput()

        assertThat(input.config.showInstantApps).isTrue()
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

        composeTestRule.onNodeWithText(LABEL).performClick()

        assertThat(fakeNavControllerWrapper.navigateCalledWith)
            .isEqualTo("AppInfoSettings/package.name/0")
    }

    @Test
    fun allAppListModel_transform() = runTest {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }

        val recordListFlow = listModel.transform(flowOf(USER_ID), flowOf(listOf(APP)))

        val recordList = recordListFlow.firstWithTimeoutOrNull()!!
        assertThat(recordList).hasSize(1)
        assertThat(recordList[0].app).isSameInstanceAs(APP)
    }

    @Test
    fun listModelGetSummary_regular() {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary = listModel.getSummary(option = 0, record = AppRecordWithSize(app = APP))
        }

        assertThat(summary()).isEqualTo(SUMMARY)
    }

    @Test
    fun listModelGetSummary_emptyStorage() {
        val listModel = AllAppListModel(context) { stateOf("") }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary = listModel.getSummary(option = 0, record = AppRecordWithSize(app = APP))
        }

        assertThat(summary()).isEqualTo(context.getPlaceholder())
    }

    @Test
    fun listModelGetSummary_disabled() {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }
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

        assertThat(summary()).isEqualTo("$SUMMARY${System.lineSeparator()}Disabled")
    }

    @Test
    fun listModelGetSummary_emptyStorageAndDisabled() {
        val listModel = AllAppListModel(context) { stateOf("") }
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
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }
        val notInstalledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary =
                listModel.getSummary(option = 0, record = AppRecordWithSize(app = notInstalledApp))
        }

        assertThat(summary())
            .isEqualTo("$SUMMARY${System.lineSeparator()}Not installed for this user")
    }

    @Test
    fun allAppListModel_archivedApp() {
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
                with(AllAppListModel(context)) {
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
    fun allAppListModel_getSummaryWhenArchived() {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }
        val archivedApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            isArchived = true
        }

        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary =
                listModel.getSummary(option = 0, record = AppRecordWithSize(app = archivedApp))
        }

        assertThat(summary()).isEqualTo(SUMMARY)
    }

    private fun getAppListInput(): AppListInput<AppRecordWithSize> {
        lateinit var input: AppListInput<AppRecordWithSize>
        composeTestRule.setContent {
            AllAppListPage {
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
                with(AllAppListModel(context)) {
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
