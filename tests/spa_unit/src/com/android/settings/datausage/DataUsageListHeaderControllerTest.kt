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
import android.net.NetworkTemplate
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.widget.Spinner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DataUsageListHeaderControllerTest {

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }

    private val repository = object : INetworkCycleDataRepository {
        override suspend fun loadCycles() = emptyList<NetworkUsageData>()
        override fun getCycles() = emptyList<Range<Long>>()
        override fun getPolicy() = null
        override suspend fun queryChartData(startTime: Long, endTime: Long) = null
    }

    private val header =
        LayoutInflater.from(context).inflate(R.layout.apps_filter_spinner, null, false)

    private val configureButton: View = header.requireViewById(R.id.filter_settings)

    private val spinner: Spinner = header.requireViewById(R.id.filter_spinner)

    private val testLifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)

    private val controller = DataUsageListHeaderController(
        header = header,
        template = mock<NetworkTemplate>(),
        sourceMetricsCategory = 0,
        viewLifecycleOwner = testLifecycleOwner,
        onCyclesLoad = {},
        updateSelectedCycle = {},
        repository = repository,
    )

    @Test
    fun onViewCreated_shouldHideCycleSpinner() {
        assertThat(spinner.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun updateCycleData_shouldShowCycleSpinner() = runBlocking {
        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(100)

        assertThat(spinner.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun setConfigButtonVisible_setToTrue_shouldShowConfigureButton() {
        controller.setConfigButtonVisible(true)

        assertThat(configureButton.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun setConfigButtonVisible_setToFalse_shouldHideConfigureButton() {
        controller.setConfigButtonVisible(false)

        assertThat(configureButton.visibility).isEqualTo(View.GONE)
    }
}
