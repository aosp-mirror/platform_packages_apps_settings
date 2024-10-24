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

package com.android.settings.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.MobileNetworkListFragment.Companion.SearchIndexProvider
import com.android.settings.network.telephony.SimRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class MobileNetworkListFragmentTest {
    private val mockSimRepository = mock<SimRepository>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun isPageSearchEnabled_showMobileNetworkPage_returnTrue() {
        mockSimRepository.stub { on { canEnterMobileNetworkPage() } doReturn true }

        val isEnabled = SearchIndexProvider { mockSimRepository }.isPageSearchEnabled(context)

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun isPageSearchEnabled_hideMobileNetworkPage_returnFalse() {
        mockSimRepository.stub { on { canEnterMobileNetworkPage() } doReturn false }

        val isEnabled = SearchIndexProvider { mockSimRepository }.isPageSearchEnabled(context)

        assertThat(isEnabled).isFalse()
    }
}
