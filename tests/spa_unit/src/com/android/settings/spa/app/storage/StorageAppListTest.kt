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

package com.android.settings.spa.app.storage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.icu.text.CollationKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageAppListTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun storageAppListPageProvider_apps_name() {
        assertThat(StorageAppListPageProvider.Apps.name).isEqualTo("StorageAppList")
    }

    @Test
    fun storageAppListPageProvider_games_name() {
        assertThat(StorageAppListPageProvider.Games.name).isEqualTo("GameStorageAppList")
    }

    @Test
    fun transform_containsSize() = runTest {
        val listModel = StorageAppListModel(context, StorageType.Apps)
        val recordListFlow = listModel.transform(flowOf(0), flowOf(listOf(APP)))
        val recordList = recordListFlow.firstWithTimeoutOrNull()!!
        assertThat(recordList).hasSize(1)
        assertThat(recordList.first().app).isSameInstanceAs(APP)
        assertThat(recordList.first().size).isEqualTo(0L)
    }

    @Test
    fun filter_apps_appWithoutGame() = runTest {
        val listModel = StorageAppListModel(context, StorageType.Apps)
        val recordListFlow = listModel.filter(
            flowOf(0),
            0,
            flowOf(
                listOf(
                    AppRecordWithSize(APP, 1L),
                    AppRecordWithSize(APP2, 1L),
                    AppRecordWithSize(GAME, 1L)
                )
            )
        )
        val recordList = recordListFlow.firstWithTimeoutOrNull()!!
        assertThat(recordList).hasSize(2)
        assertThat(recordList.none { it.app === GAME }).isTrue()
    }

    @Test
    fun filter_games_gameWithoutApp() = runTest {
        val listModel = StorageAppListModel(context, StorageType.Games)
        val recordListFlow = listModel.filter(
            flowOf(0),
            0,
            flowOf(
                listOf(
                    AppRecordWithSize(APP, 1L),
                    AppRecordWithSize(APP2, 1L),
                    AppRecordWithSize(GAME, 1L)
                )
            )
        )
        val recordList = recordListFlow.firstWithTimeoutOrNull()!!
        assertThat(recordList).hasSize(1)
        assertThat(recordList.all { it.app === GAME }).isTrue()
    }

    @Test
    fun getComparator_sortsByDescendingSize() {
        val listModel = StorageAppListModel(context, StorageType.Apps)
        val source = listOf(
            AppEntry(
                AppRecordWithSize(app = APP, size = 1L),
                "app1",
                CollationKey("first", byteArrayOf(0))
            ),
            AppEntry(
                AppRecordWithSize(app = APP2, size = 3L),
                "app2",
                CollationKey("second", byteArrayOf(0))
            ),
            AppEntry(
                AppRecordWithSize(app = APP3, size = 3L),
                "app3",
                CollationKey("third", byteArrayOf(0))
            )
        )

        val result = source.sortedWith(listModel.getComparator(0))

        assertThat(result[0].record.app).isSameInstanceAs(APP2)
        assertThat(result[1].record.app).isSameInstanceAs(APP3)
        assertThat(result[2].record.app).isSameInstanceAs(APP)
    }

    private companion object {
        const val APP_PACKAGE_NAME = "app.package.name"
        const val APP_PACKAGE_NAME2 = "app.package.name2"
        const val APP_PACKAGE_NAME3 = "app.package.name3"
        const val GAME_PACKAGE_NAME = "game.package.name"
        val APP = ApplicationInfo().apply {
            packageName = APP_PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
        }
        val APP2 = ApplicationInfo().apply {
            packageName = APP_PACKAGE_NAME2
            flags = ApplicationInfo.FLAG_INSTALLED
        }
        val APP3 = ApplicationInfo().apply {
            packageName = APP_PACKAGE_NAME3
            flags = ApplicationInfo.FLAG_INSTALLED
        }
        val GAME = ApplicationInfo().apply {
            packageName = GAME_PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_IS_GAME
        }
    }
}