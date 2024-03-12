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

package com.android.settings.datausage

import android.content.Context
import android.content.Intent
import android.net.NetworkTemplate
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.SettingsActivity
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settingslib.AppItem
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DataUsageListAppsControllerTest {

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }

    private val controller = DataUsageListAppsController(context, "test_key")

    @Before
    fun setUp() {
        controller.init(mock<NetworkTemplate>())
        val data = NetworkUsageData(START_TIME, END_TIME, 0)
        controller.updateCycles(listOf(data))
    }

    @Test
    fun startAppDataUsage_shouldAddCyclesInfoToLaunchArguments() {
        controller.startAppDataUsage(AppItem(), END_TIME)

        val intent = argumentCaptor<Intent> {
            verify(context).startActivity(capture())
        }.firstValue
        val arguments = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)!!
        assertThat(arguments.getLong(AppDataUsage.ARG_SELECTED_CYCLE)).isEqualTo(END_TIME)
        assertThat(
            arguments.getSerializable(AppDataUsage.ARG_NETWORK_CYCLES, ArrayList::class.java)
        ).containsExactly(END_TIME, START_TIME).inOrder()
    }

    private companion object {
        const val START_TIME = 1521583200000L
        const val END_TIME = 1521676800000L
    }
}
