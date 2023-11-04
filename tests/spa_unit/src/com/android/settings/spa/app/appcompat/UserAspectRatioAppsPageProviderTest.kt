/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.spa.app.appcompat

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN
import android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.FakeNavControllerWrapper
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * To run this test: atest SettingsSpaUnitTests:UserAspectRatioAppsPageProviderTest
 */
@RunWith(AndroidJUnit4::class)
class UserAspectRatioAppsPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fakeNavControllerWrapper = FakeNavControllerWrapper()

    @Test
    fun aspectRatioAppsPageProvider_name() {
        assertThat(UserAspectRatioAppsPageProvider.name).isEqualTo(EXPECTED_PROVIDER_NAME)
    }

    @Test
    fun injectEntry_title() {
        setInjectEntry()
        composeTestRule.onNodeWithText(context.getString(R.string.aspect_ratio_experimental_title))
            .assertIsDisplayed()
    }

    @Test
    fun injectEntry_summary() {
        setInjectEntry()
        composeTestRule
            .onNodeWithText(context.getString(R.string.aspect_ratio_summary_text, Build.MODEL))
            .assertIsDisplayed()
    }

    @Test
    fun injectEntry_onClick_navigate() {
        setInjectEntry()
        composeTestRule.onNodeWithText(context.getString(R.string.aspect_ratio_experimental_title))
            .performClick()
        assertThat(fakeNavControllerWrapper.navigateCalledWith).isEqualTo("UserAspectRatioAppsPage")
    }

    private fun setInjectEntry() {
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                UserAspectRatioAppsPageProvider.buildInjectEntry().build().UiLayout()
            }
        }
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            UserAspectRatioAppList {}
        }

        composeTestRule.onNodeWithText(context.getString(R.string.aspect_ratio_experimental_title))
            .assertIsDisplayed()
    }

    @Test
    fun item_labelDisplayed() {
        setItemContent()

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun aspectRatioAppListModel_transform() = runTest {
        val listModel = UserAspectRatioAppListModel(context)
        val recordListFlow = listModel.transform(flowOf(USER_ID), flowOf(listOf(APP)))
        val recordList = recordListFlow.firstWithTimeoutOrNull()!!

        assertThat(recordList).hasSize(1)
        assertThat(recordList[0].app).isSameInstanceAs(APP)
    }

    @Test
    fun aspectRatioAppListModel_filter() = runTest {
        val listModel = UserAspectRatioAppListModel(context)

        val recordListFlow = listModel.filter(flowOf(USER_ID), 0,
            flowOf(listOf(APP_RECORD_NOT_DISPLAYED, APP_RECORD_SUGGESTED)))

        val recordList = checkNotNull(recordListFlow.firstWithTimeoutOrNull())
        assertThat(recordList).containsExactly(APP_RECORD_SUGGESTED)
    }

    private fun setItemContent() {
        composeTestRule.setContent {
            fakeNavControllerWrapper.Wrapper {
                with(UserAspectRatioAppListModel(context)) {
                    AppListItemModel(
                        record = APP_RECORD_SUGGESTED,
                        label = LABEL,
                        summary = { SUMMARY }
                    ).AppItem()
                }
            }
        }
    }

    @Test
    fun aspectRatioAppListModel_getSummaryDefault() {
        val summary = getSummary(USER_MIN_ASPECT_RATIO_UNSET)

        assertThat(summary).isEqualTo(context.getString(R.string.user_aspect_ratio_app_default))
    }

    @Test
    fun aspectRatioAppListModel_getSummaryWhenSplitScreen() {
        val summary = getSummary(USER_MIN_ASPECT_RATIO_SPLIT_SCREEN)

        assertThat(summary).isEqualTo(context.getString(R.string.user_aspect_ratio_half_screen))
    }

    private fun getSummary(userOverride: Int): String {
        val listModel = UserAspectRatioAppListModel(context)
        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary = listModel.getSummary(option = 0,
                record = UserAspectRatioAppListItemModel(
                    app = APP,
                    userOverride = userOverride,
                    suggested = false,
                    canDisplay = true,
                ))
        }
        return summary()
    }


    private companion object {
        private const val EXPECTED_PROVIDER_NAME = "UserAspectRatioAppsPage"
        private const val PACKAGE_NAME = "package.name"
        private const val USER_ID = 0
        private const val LABEL = "Label"
        private const val SUMMARY = "Summary"

        private val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
        private val APP_RECORD_SUGGESTED = UserAspectRatioAppListItemModel(
            APP,
            userOverride = USER_MIN_ASPECT_RATIO_UNSET,
            suggested = true,
            canDisplay = true
        )
        private val APP_RECORD_NOT_DISPLAYED = UserAspectRatioAppListItemModel(
            APP,
            userOverride = USER_MIN_ASPECT_RATIO_UNSET,
            suggested = true,
            canDisplay = false
        )
    }
}