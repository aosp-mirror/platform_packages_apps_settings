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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
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
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllAppListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val fakeNavControllerWrapper = FakeNavControllerWrapper()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun allAppListModel_transform() = runTest {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }

        val recordListFlow = listModel.transform(flowOf(USER_ID), flowOf(listOf(APP)))

        val recordList = recordListFlow.firstWithTimeoutOrNull()!!
        assertThat(recordList).hasSize(1)
        assertThat(recordList[0].app).isSameInstanceAs(APP)
    }

    @Test
    fun allAppListModel_getSummary() {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }

        lateinit var summaryState: State<String>
        composeTestRule.setContent {
            summaryState = listModel.getSummary(option = 0, record = AppRecordWithSize(app = APP))
        }

        assertThat(summaryState.value).isEqualTo(SUMMARY)
    }

    @Test
    fun allAppListModel_getSummaryWhenDisabled() {
        val listModel = AllAppListModel(context) { stateOf(SUMMARY) }
        val disabledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            enabled = false
        }

        lateinit var summaryState: State<String>
        composeTestRule.setContent {
            summaryState =
                listModel.getSummary(option = 0, record = AppRecordWithSize(app = disabledApp))
        }

        assertThat(summaryState.value).isEqualTo("$SUMMARY${System.lineSeparator()}Disabled")
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
                        summary = stateOf(SUMMARY),
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
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}
