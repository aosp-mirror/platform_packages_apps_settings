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

package com.android.settings.spa.development.compat

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class PlatformCompatAppListModelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var listModel: PlatformCompatAppListModel

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(packageManager.getInstalledPackagesAsUser(any<PackageInfoFlags>(), anyInt()))
            .thenReturn(emptyList())
        listModel = PlatformCompatAppListModel(context)
    }

    @Test
    fun transform() = runTest {
        val recordListFlow = listModel.transform(
            userIdFlow = flowOf(USER_ID),
            appListFlow = flowOf(listOf(APP)),
        )

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
        val record = recordList[0]
        assertThat(record.app).isSameInstanceAs(APP)
    }

    @Test
    fun getSummary() = runTest {
        val summary = getSummary(APP)

        assertThat(summary).isEqualTo(PACKAGE_NAME)
    }

    private fun getSummary(app: ApplicationInfo): String {
        lateinit var summary: () -> String
        composeTestRule.setContent {
            summary = listModel.getSummary(
                option = 0,
                record = PlatformCompatAppRecord(app),
            )
        }
        return summary()
    }

    private companion object {
        const val USER_ID = 0
        const val PACKAGE_NAME = "package.name"
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}