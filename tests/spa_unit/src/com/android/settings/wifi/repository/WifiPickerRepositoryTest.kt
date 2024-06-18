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

package com.android.settings.wifi.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.android.wifitrackerlib.WifiEntry
import com.android.wifitrackerlib.WifiPickerTracker
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiPickerRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockWifiPickerTracker = mock<WifiPickerTracker>()

    private var callback: WifiPickerTracker.WifiPickerTrackerCallback? = null

    private val repository =
        WifiPickerRepository(context) { _, callback ->
            this.callback = callback
            mockWifiPickerTracker
        }

    @Test
    fun connectedWifiEntryFlow_callOnStartOnStopAndOnDestroy() = runBlocking {
        repository.connectedWifiEntryFlow().firstWithTimeoutOrNull()

        verify(mockWifiPickerTracker).onStart()
        verify(mockWifiPickerTracker).onStop()
        verify(mockWifiPickerTracker).onDestroy()
    }

    @Test
    fun connectedWifiEntryFlow_initial() = runBlocking {
        val wifiEntry = repository.connectedWifiEntryFlow().firstWithTimeoutOrNull()

        assertThat(wifiEntry).isNull()
    }

    @Test
    fun connectedWifiEntryFlow_onWifiEntriesChanged() = runBlocking {
        val listDeferred = async { repository.connectedWifiEntryFlow().toListWithTimeout() }
        delay(100)

        mockWifiPickerTracker.stub { on { connectedWifiEntry } doReturn mock<WifiEntry>() }
        callback?.onWifiEntriesChanged()

        assertThat(listDeferred.await().filterNotNull()).isNotEmpty()
    }
}
