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

package com.android.settings.spa.app.specialaccess

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.DeadSystemRuntimeException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.AppOpsController
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
import org.mockito.Mockito.eq
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class PictureInPictureTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var listModel: PictureInPictureListModel

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(packageManager.getInstalledPackagesAsUser(any<PackageInfoFlags>(), anyInt()))
            .thenReturn(emptyList())
        listModel = PictureInPictureListModel(context)
    }

    @Test
    fun modelResourceId() {
        assertThat(listModel.pageTitleResId).isEqualTo(R.string.picture_in_picture_title)
        assertThat(listModel.switchTitleResId).isEqualTo(R.string.picture_in_picture_app_detail_switch)
        assertThat(listModel.footerResId).isEqualTo(R.string.picture_in_picture_app_detail_summary)
    }

    @Test
    fun transform() = runTest {
        whenever(packageManager.getInstalledPackagesAsUser(any<PackageInfoFlags>(), anyInt()))
            .thenReturn(listOf(PICTURE_IN_PICTURE_PACKAGE_INFO))

        val recordListFlow = listModel.transform(
            userIdFlow = flowOf(USER_ID),
            appListFlow = flowOf(listOf(PICTURE_IN_PICTURE_APP)),
        )

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
        val record = recordList[0]
        assertThat(record.app).isSameInstanceAs(PICTURE_IN_PICTURE_APP)
        assertThat(record.isSupport).isTrue()
    }

    @Test
    fun transform_getInstalledPackagesAsUserThrowsException_treatAsNotSupported() = runTest {
        whenever(packageManager.getInstalledPackagesAsUser(any<PackageInfoFlags>(), anyInt()))
            .thenThrow(DeadSystemRuntimeException())

        val recordListFlow = listModel.transform(
            userIdFlow = flowOf(USER_ID),
            appListFlow = flowOf(listOf(PICTURE_IN_PICTURE_APP)),
        )

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
        val record = recordList[0]
        assertThat(record.app).isSameInstanceAs(PICTURE_IN_PICTURE_APP)
        assertThat(record.isSupport).isFalse()
    }

    @Test
    fun transformItem() {
        whenever(
            packageManager.getPackageInfoAsUser(
                eq(PICTURE_IN_PICTURE_PACKAGE_NAME), any<PackageInfoFlags>(), eq(USER_ID)
            )
        ).thenReturn(PICTURE_IN_PICTURE_PACKAGE_INFO)

        val record = listModel.transformItem(PICTURE_IN_PICTURE_APP)

        assertThat(record.app).isSameInstanceAs(PICTURE_IN_PICTURE_APP)
        assertThat(record.isSupport).isTrue()
    }

    @Test
    fun transformItem_getPackageInfoAsUserThrowsException_treatAsNotSupported() {
        whenever(
            packageManager.getPackageInfoAsUser(
                eq(PICTURE_IN_PICTURE_PACKAGE_NAME), any<PackageInfoFlags>(), eq(USER_ID)
            )
        ).thenThrow(DeadSystemRuntimeException())

        val record = listModel.transformItem(PICTURE_IN_PICTURE_APP)

        assertThat(record.app).isSameInstanceAs(PICTURE_IN_PICTURE_APP)
        assertThat(record.isSupport).isFalse()
    }

    @Test
    fun filter_isSupport() = runTest {
        val record = createRecord(isSupport = true)

        val recordListFlow = listModel.filter(flowOf(USER_ID), flowOf(listOf(record)))

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
    }

    @Test
    fun filter_notSupport() = runTest {
        val record = createRecord(isSupport = false)

        val recordListFlow = listModel.filter(flowOf(USER_ID), flowOf(listOf(record)))

        val recordList = recordListFlow.first()
        assertThat(recordList).isEmpty()
    }

    @Test
    fun isChangeable_isSupport() {
        val record = createRecord(isSupport = true)

        val isChangeable = listModel.isChangeable(record)

        assertThat(isChangeable).isTrue()
    }

    @Test
    fun isChangeable_notSupport() {
        val record = createRecord(isSupport = false)

        val isChangeable = listModel.isChangeable(record)

        assertThat(isChangeable).isFalse()
    }

    private fun createRecord(isSupport: Boolean) = PictureInPictureRecord(
        app = PICTURE_IN_PICTURE_APP,
        isSupport = isSupport,
        appOpsController = AppOpsController(
            context = context,
            app = PICTURE_IN_PICTURE_APP,
            op = AppOpsManager.OP_PICTURE_IN_PICTURE,
        ),
    )

    private companion object {
        const val USER_ID = 0
        const val PICTURE_IN_PICTURE_PACKAGE_NAME = "picture.in.picture.package.name"
        val PICTURE_IN_PICTURE_APP = ApplicationInfo().apply {
            packageName = PICTURE_IN_PICTURE_PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
        }
        val PICTURE_IN_PICTURE_PACKAGE_INFO = PackageInfo().apply {
            packageName = PICTURE_IN_PICTURE_PACKAGE_NAME
            activities = arrayOf(ActivityInfo().apply {
                flags = ActivityInfo.FLAG_SUPPORTS_PICTURE_IN_PICTURE
            })
        }
    }
}